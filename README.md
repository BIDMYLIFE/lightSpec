# LightSpec 使用者認證網站

LightSpec 是同源部署的 Spring Boot + React 認證網站，使用 SQLite、Flyway、Argon2id、短效 JWT Secure Cookie 與 CSRF 防護。前端資產會封裝進 Spring Boot JAR，正式環境不依賴外部 CDN。

## 公開部署前必讀

> **首次初始化是公開的。** 空資料庫啟動後，第一位完成 `/setup/admin` 表單的訪客將成為管理員。產品已接受此風險；請先準備管理員帳密，開放公網後立即完成初始化。建立第一位 ADMIN 後，後端會永久拒絕再次初始化。

正式環境 MUST 使用 TLS。JWT 與 CSRF Cookie 都標記為 `Secure`，直接使用 HTTP 將無法完成登入。建議由 Caddy、Nginx 或雲端負載平衡器終止 TLS，再反向代理到只對內開放的 `127.0.0.1:8080`。應保留 `X-Forwarded-Proto` 與 `X-Forwarded-For`；應用已啟用 framework forwarded-header 支援。

以 `start.ps1` 在本機直接使用 HTTP 時，啟動檔會自動設定 `LIGHTSPEC_COOKIE_SECURE=false`，讓瀏覽器可使用 CSRF 與登入 Cookie。此設定僅適用於本機開發；正式環境請使用 HTTPS 並保持 `LIGHTSPEC_COOKIE_SECURE=true`（預設值）。

## 必要環境

- Java 21
- Maven 3.9+
- Node.js 24+ 與 npm
- 可寫入且會持久保存的 SQLite 目錄

## 建置

在專案根目錄執行：

```powershell
& 'C:\Program Files\nodejs\npm.cmd' --prefix frontend ci
& 'C:\Program Files\nodejs\npm.cmd' --prefix frontend run build
& 'C:\Program Files\Apache\maven\bin\mvn.cmd' -f backend/pom.xml clean package
```

產物位於 `backend/target/light-spec-backend-0.0.1-SNAPSHOT.jar`。

## 設定與啟動

複製 [.env.example](.env.example) 作為設定清單，但不要提交實際 `.env`。必要參數如下：

- `LIGHTSPEC_JWT_SECRET`：必填，至少 32 bytes 的隨機秘密；缺少或太短時應用拒絕啟動。
- `LIGHTSPEC_JWT_TTL`：ISO-8601 duration，預設 `PT15M`。
- `LIGHTSPEC_DB_PATH`：SQLite 絕對路徑；預設為工作目錄下 `./data/lightspec.db`。
- `PORT`：內部 HTTP port，預設 `8080`。

PowerShell 可用下列方式產生 secret：

```powershell
$secretBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($secretBytes)
[Convert]::ToBase64String($secretBytes)
```

啟動範例：

```powershell
$env:LIGHTSPEC_JWT_SECRET = '<填入產生的隨機值>'
$env:LIGHTSPEC_DB_PATH = 'D:\LightSpec\data\lightspec.db'
$env:PORT = '8080'
java -jar backend\target\light-spec-backend-0.0.1-SNAPSHOT.jar
```

全新資料庫啟動時 Flyway 會自動建立 schema。用 `GET /api/setup/status` 驗證服務；`{"setupRequired":true}` 代表必須立即前往 `/setup/admin`。

## TLS 反向代理檢查清單

- 公網只開放 443，8080 僅供反向代理連線。
- 強制 HTTP 重新導向 HTTPS，啟用現代 TLS 與 HSTS。
- 傳遞 `Host`、`X-Forwarded-Proto https`、`X-Forwarded-For`。
- 不在 proxy access log 記錄 Cookie、Authorization 或完整認證 request body。
- 初始化完成後確認 `/api/setup/status` 回傳 `false`，且再次呼叫初始化 API 得到 409。

## SQLite 備份與還原

資料庫是單一可移植檔案，但線上複製可能遇到未提交的 WAL 資料。建議每日備份並定期測試還原：

1. 最安全方式是先優雅停止應用，再複製 `lightspec.db` 到有日期與異地保存的備份目錄。
2. 必須線上備份時，使用 SQLite CLI 的一致性備份：`sqlite3 lightspec.db ".backup 'backup/lightspec-YYYYMMDD.db'"`。
3. 備份檔應加密、限制權限，並與 JWT secret 分開保存。
4. 還原前先保留目前 DB，再以備份取代資料庫，啟動後檢查 Flyway migration 與登入流程。

## 更新與回滾

更新前先備份 JAR 與 SQLite。部署新 JAR 後檢查啟動 log、`/api/setup/status`、註冊／登入及 `/api/auth/me`。

若需回滾：

1. 停止服務，避免同時寫入 SQLite。
2. 還原上一版 JAR。
3. 若新版已套用不相容 migration，同時還原更新前的 SQLite 備份；不得手動修改 Flyway history 或直接執行破壞性 down migration。
4. 使用原本的 `LIGHTSPEC_JWT_SECRET` 啟動，驗證狀態與登入後再恢復流量。

## 開發驗證

```powershell
& 'C:\Program Files\nodejs\npm.cmd' --prefix frontend run test
& 'C:\Program Files\nodejs\npm.cmd' --prefix frontend run build
& 'C:\Program Files\Apache\maven\bin\mvn.cmd' -f backend/pom.xml test
```
