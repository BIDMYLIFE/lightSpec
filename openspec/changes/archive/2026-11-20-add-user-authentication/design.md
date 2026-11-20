# Design

## Context

專案目前只有架構指引與 OpenSpec，沒有既有應用程式碼或資料庫。實作必須同時建立 Spring Boot 分層後端與 React MVVM 前端，核心資產不得依賴 CDN；認證使用 JWT，持久層使用 SQLite 並保留未來更換關聯式資料庫的路徑。行為範圍見 `proposal.md` 與兩份 capability specs。

首次管理員初始化將直接暴露在公開網路，不採用 setup token、IP 限制或本機管理通道。這是使用者明確接受的產品決策，因此設計只保證「最多一個並行請求成功」與「已有 ADMIN 後不可再次初始化」，不宣稱能辨識第一位訪客是否為合法站台擁有者。

## Goals / Non-Goals

**Goals:**

- 以清楚的 Repository、Service、MVC Controller 與 Web API Controller 邊界建立可測試後端。
- 提供同源部署、響應式且不依賴 CDN 的 React 認證介面。
- 讓公開管理員初始化在多個同時請求下仍只有一個成功。
- 避免明文密碼、瀏覽器持久化 JWT 與可偽造前端檢查造成的安全缺口。
- 以 migration、可攜資料型別與正規化欄位降低日後移植資料庫的成本。

**Non-Goals:**

- OAuth、社群登入、多因素驗證、忘記密碼、電子郵件驗證與帳號自助復原。
- 管理員後台、角色管理、刪除使用者或建立額外管理員。
- 跨裝置 token 撤銷、refresh token、單一登入或即時登出所有裝置。
- 保護公開首次初始化免受合法擁有者以外的第一位訪客搶先操作。
- 此 change 內執行 SQLite 至其他資料庫的實際搬遷。

## Decisions

### 1. 採用後端與前端分目錄的單一儲存庫

建立 `backend/` Maven Spring Boot 專案及 `frontend/` React + TypeScript 專案。開發期間由 Vite 提供前端並代理 `/api`，正式建置時將前端靜態產物交由 Spring MVC 提供，讓 Cookie、CSRF 與 CORS 邊界保持同源。

替代方案是讓前後端分開部署，但會增加 CORS、Cookie domain、TLS 與部署設定複雜度，不適合目前的單一網站範圍。

### 2. 後端使用嚴格分層及專用 DTO

Repository 只處理 JPA persistence；Service 負責 transaction、密碼雜湊、初始化競態及認證規則；Web API Controller 只負責 HTTP mapping 與輸入驗證；MVC Controller 提供 SPA fallback，不接觸 Repository。API 請求與回應使用 DTO，不直接序列化 entity。

### 3. 使用可正規化的唯一使用者名稱

使用者資料保存顯示用 `username`、唯一索引用 `normalized_username`、密碼雜湊、角色、啟用狀態與建立時間。服務層以固定 Unicode 正規化與不區分大小寫規則產生 `normalized_username`，資料庫對該欄位建立唯一約束。

相較依賴 SQLite collation，此方式在日後移植至不同資料庫時行為較一致，也能保留使用者原始顯示名稱。

### 4. 使用 Flyway 與可攜 schema 管理 SQLite

所有表格、索引及 bootstrap guard 初始列都由 Flyway migration 建立；JPA schema generation 僅用於驗證，不負責正式建表。主鍵、時間、布林值與 enum 儲存採可映射至主流關聯式資料庫的形式，repository 避免 SQLite 專屬查詢。

SQLite 檔案位置由環境設定，預設放在應用程式可寫的 data 目錄；資料庫檔案與 JWT secret 不納入版本控制。

### 5. 以 bootstrap guard 交易防止重複建立第一位管理員

資料庫包含固定一列的 `bootstrap_guard`，並具有樂觀鎖版本欄位。初始化 service 在同一 transaction 中載入 guard、查詢 ADMIN、建立 ADMIN 並更新 guard 版本。兩個請求同時通過初始查詢時，版本競爭只允許一個 transaction 提交；失敗者重新確認狀態後回傳衝突。

前端 route guard 只改善導覽體驗；`POST /api/setup/admin` 每次都必須在 transaction 內重查 ADMIN。若未來管理員記錄因外部操作全部消失，系統將依需求重新進入初始化狀態。

替代方案包括應用程式內 mutex 與 SQLite 專屬鎖。前者無法支援多執行個體，後者增加日後移植成本，因此不採用。

### 6. JWT 置於安全 Cookie，前端透過 `/me` 還原狀態

登入成功後，後端簽發短效 JWT，包含不可變使用者 ID、顯示名稱、角色、簽發時間與到期時間，並透過 `HttpOnly`、`Secure`、`SameSite=Strict` Cookie 傳送。JWT 不寫入 `localStorage` 或 `sessionStorage`；Axios 使用 credentials，React 的 Auth ViewModel 透過 `/api/auth/me` 還原登入狀態與顯示名稱。

JWT 簽章 secret 必須由部署環境提供，啟動時若缺少或強度不足則失敗。修改狀態的 Cookie 認證 API 使用 CSRF 防護。登出清除 Cookie；由於本階段不建立撤銷清單，被複製的 JWT 在到期前仍可能有效，以短存活時間降低影響。

相較把 JWT 放入 `localStorage`，HttpOnly Cookie 可降低 XSS 直接竊取 token 的風險；相較 access/refresh token 雙 token 設計，此方案較符合目前簡單帳號系統的範圍。

### 7. 前端使用集中式 ViewModel 控制啟動狀態

應用程式啟動先查詢 `/api/setup/status`，狀態分為 checking、setup-required、anonymous 與 authenticated。setup-required 時，路由層只允許新增管理員頁；已有 ADMIN 時，該頁立即導向登入。完成初始化後重新取得狀態，不把前端旗標當成安全依據。

表單由各自 ViewModel 管理 loading、欄位錯誤與 API 錯誤；Axios 統一處理未認證回應；SweetAlert2 顯示成功或全域錯誤。Tailwind 的 local build 產生響應式樣式，不引用 CDN。

### 8. API 邊界

- `GET /api/setup/status`：公開回傳是否需要初始化。
- `POST /api/setup/admin`：僅在沒有 ADMIN 時公開建立第一位管理員。
- `POST /api/auth/register`：已有 ADMIN 時公開建立 USER。
- `POST /api/auth/login`：已有 ADMIN 時驗證帳密並設定 JWT Cookie。
- `POST /api/auth/logout`：清除 JWT Cookie。
- `GET /api/auth/me`：回傳目前使用者 ID、使用者名稱與角色。

API 使用一致的 validation 與 problem response 格式。密碼使用具記憶體成本的單向雜湊演算法；日誌不得記錄密碼、JWT、Cookie 或完整認證請求內容。

## Risks / Trade-offs

- [公開首次初始化可被陌生訪客搶先完成] → 產品已明確接受；文件與部署輸出醒目提示，並將公開服務時間與初始化操作安排得盡量接近。
- [前端轉跳可被繞過] → 所有初始化與授權規則在 Service 與 Spring Security 層重複強制執行。
- [SQLite 寫入併發有限] → 保持短 transaction、設定 busy timeout，並以並行整合測試驗證 bootstrap；成長後可依 migration 路徑更換資料庫。
- [JWT 無伺服器端即時撤銷] → 使用短效 token、安全 Cookie 與 TLS；跨裝置撤銷留待後續能力。
- [Cookie JWT 產生 CSRF 面] → SameSite=Strict 搭配 Spring Security CSRF token，Axios 統一送出 token。
- [Unicode 使用者名稱可能有視覺混淆] → 使用固定正規化規則與唯一 canonical 欄位；更嚴格的同形字限制可另案加入。
- [SQLite 方言與 migration 相容性] → 鎖定經測試的 JPA SQLite dialect，migration 與 repository 加入真實 SQLite 整合測試。

## Migration Plan

1. 建立前後端專案骨架、測試基礎與本機離線資產建置流程。
2. 加入 Flyway 初始 migration，建立使用者、唯一索引及 bootstrap guard。
3. 部署時提供 SQLite 檔案路徑及高強度 JWT secret，並確保 data 目錄可寫且不被靜態網站公開。
4. 首次啟動後立即透過公開初始化頁建立 ADMIN，再確認初始化 API 已拒絕重複建立。
5. 回滾應先停止服務並備份 SQLite 檔案；應用程式版本回退不得自動執行破壞性 down migration。
