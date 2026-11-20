param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $projectRoot 'backend\target\light-spec-backend-0.0.1-SNAPSHOT.jar'

function Resolve-ToolPath {
    param(
        [string[]]$Names,
        [string[]]$FallbackPaths
    )

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) {
            return $command.Source
        }
    }

    foreach ($path in $FallbackPaths) {
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            return $path
        }
    }

    return $null
}

function Import-LocalEnvironment {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*(?:export\s+)?(?<name>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?<value>.*)\s*$') {
            $name = $Matches.name
            $value = $Matches.value.Trim()
            if ($value.Length -ge 2) {
                $first = $value[0]
                $last = $value[$value.Length - 1]
                if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
            }

            if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
            }
        }
    }
}

Push-Location $projectRoot
try {
    Import-LocalEnvironment -Path (Join-Path $projectRoot '.env')

    $jwtSecret = [Environment]::GetEnvironmentVariable('LIGHTSPEC_JWT_SECRET', 'Process')
    if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
        if ($env:LIGHTSPEC_ENV -eq 'production' -or $env:SPRING_PROFILES_ACTIVE -match '(^|,)prod(,|$)') {
            throw '正式環境必須明確設定 LIGHTSPEC_JWT_SECRET（至少 32 bytes）；不會自動產生。'
        }

        $secretBytes = New-Object byte[] 48
        $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $randomGenerator.GetBytes($secretBytes)
        }
        finally {
            $randomGenerator.Dispose()
        }
        $jwtSecret = [Convert]::ToBase64String($secretBytes)
        [Environment]::SetEnvironmentVariable('LIGHTSPEC_JWT_SECRET', $jwtSecret, 'Process')

        $envPath = Join-Path $projectRoot '.env'
        $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
        if (Test-Path -LiteralPath $envPath -PathType Leaf) {
            [System.IO.File]::AppendAllText($envPath, "`r`nLIGHTSPEC_JWT_SECRET=$jwtSecret`r`n", $utf8WithoutBom)
        }
        else {
            [System.IO.File]::WriteAllText($envPath, "LIGHTSPEC_JWT_SECRET=$jwtSecret`r`n", $utf8WithoutBom)
        }
        Write-Host '已產生本機隨機 JWT secret 並保存於 .env。正式環境請明確設定 secret。'
    }

    $secretBytesCount = [System.Text.Encoding]::UTF8.GetByteCount([string]$jwtSecret)
    if ($secretBytesCount -lt 32 -or $jwtSecret -match '(?i)replace-with|change-me|your-secret|<.*>') {
        throw 'LIGHTSPEC_JWT_SECRET 太短或仍是範例值。請在 .env 設定至少 32 bytes 的隨機 secret。'
    }

    $productionMode = $env:LIGHTSPEC_ENV -eq 'production' -or $env:SPRING_PROFILES_ACTIVE -match '(^|,)prod(,|$)'
    if ([string]::IsNullOrWhiteSpace($env:LIGHTSPEC_COOKIE_SECURE) -and -not $productionMode) {
        $env:LIGHTSPEC_COOKIE_SECURE = 'false'
        Write-Host '本機 HTTP 模式已停用 Secure cookie；正式環境請使用 HTTPS 並設定 LIGHTSPEC_COOKIE_SECURE=true。'
    }

    if ([string]::IsNullOrWhiteSpace($env:LIGHTSPEC_DB_PATH)) {
        $env:LIGHTSPEC_DB_PATH = Join-Path $projectRoot 'backend\data\lightspec.db'
    } elseif (-not [System.IO.Path]::IsPathRooted($env:LIGHTSPEC_DB_PATH)) {
        $env:LIGHTSPEC_DB_PATH = Join-Path $projectRoot $env:LIGHTSPEC_DB_PATH
    }

    $databaseDirectory = Split-Path -Parent $env:LIGHTSPEC_DB_PATH
    if (-not [string]::IsNullOrWhiteSpace($databaseDirectory)) {
        New-Item -ItemType Directory -Path $databaseDirectory -Force | Out-Null
    }

    $javaPath = Resolve-ToolPath -Names @('java.exe', 'java') -FallbackPaths @(
        'C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe',
        'C:\Program Files\Java\jdk-21\bin\java.exe'
    )
    if (-not $javaPath) {
        throw '找不到 Java。請安裝 Java 21 並將 java 加入 PATH。'
    }

    if ($Build -or -not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        $npmPath = Resolve-ToolPath -Names @('npm.cmd') -FallbackPaths @(
            'C:\Program Files\nodejs\npm.cmd'
        )
        $mavenPath = Resolve-ToolPath -Names @('mvn.cmd', 'mvn') -FallbackPaths @(
            'C:\Program Files\Apache\maven\bin\mvn.cmd'
        )
        if (-not $npmPath) {
            throw '建置前端需要 npm。請安裝 Node.js 24+ 與 npm，並將 npm 加入 PATH。'
        }
        if (-not $mavenPath) {
            throw '找不到 Maven。請安裝 Maven 3.9+ 並將 mvn 加入 PATH。'
        }

        $frontendDependencies = Join-Path $projectRoot 'frontend\node_modules'
        if (-not (Test-Path -LiteralPath $frontendDependencies -PathType Container)) {
            Write-Host '安裝前端相依套件...'
            & $npmPath --prefix frontend ci
            if ($LASTEXITCODE -ne 0) {
                throw "npm ci 失敗（exit code $LASTEXITCODE）。"
            }
        }

        Write-Host '建置前端...'
        & $npmPath --prefix frontend run build
        if ($LASTEXITCODE -ne 0) {
            throw "前端建置失敗（exit code $LASTEXITCODE）。"
        }

        Write-Host '建置 Spring Boot JAR...'
        & $mavenPath -f backend/pom.xml package
        if ($LASTEXITCODE -ne 0) {
            throw "後端建置失敗（exit code $LASTEXITCODE）。"
        }
    }

    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "找不到啟動 JAR：$jarPath"
    }

    $port = if ([string]::IsNullOrWhiteSpace($env:PORT)) { '8080' } else { $env:PORT }
    Write-Host "啟動 LightSpec（port $port）..."
    & $javaPath -jar $jarPath
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
