# Tasks

## 1. 專案骨架與建置

- [x] 1.1 建立 `backend/` Maven Spring Boot 專案及 Repository、Service、MVC Controller、Web API Controller 套件邊界，並以 Maven 測試命令驗證應用程式 context 可啟動。
- [x] 1.2 加入 Spring Web、Validation、Security、JPA、Flyway、SQLite、JWT 與測試依賴，並以 dependency resolution 與後端建置成功驗證版本相容性。
- [x] 1.3 建立 `frontend/` React + TypeScript + Vite 專案，安裝本機 Tailwind CSS、Axios、SweetAlert2、路由與測試依賴，並驗證前端測試及 production build 均成功且 HTML 無 CDN 參照。
- [x] 1.4 設定 Vite 開發代理及正式環境由 Spring MVC 提供 SPA 靜態產物與 fallback route，並以建置後的深層路由請求可載入 SPA 驗證同源部署。

## 2. SQLite 資料層

- [x] 2.1 建立 Flyway 初始 migration，包含 users、`normalized_username` 唯一索引、角色與啟用狀態欄位、時間欄位及單列 bootstrap guard，並以全新 SQLite 檔案成功 migration 驗證 schema。
- [x] 2.2 實作使用者、角色與 bootstrap guard entity 及 Spring Data repositories，設定 JPA 僅驗證 schema，並以真實 SQLite repository 整合測試驗證 ADMIN 查詢、唯一名稱及 guard 版本行為。
- [x] 2.3 實作固定的 Unicode 使用者名稱正規化與輸入驗證元件，並以單元測試覆蓋大小寫等價、前後空白、Unicode 正規化、無效名稱與密碼界線。
- [x] 2.4 設定可外部指定的 SQLite 路徑、busy timeout 與忽略本機資料庫檔案的版本控制規則，並以替代測試路徑啟動應用程式確認設定生效。

## 3. 首次管理員初始化後端

- [x] 3.1 實作初始化狀態 Service 與公開 `GET /api/setup/status`，並以無 ADMIN 與已有 ADMIN 的整合測試驗證回應。
- [x] 3.2 實作 transaction 內鎖定 bootstrap guard、重查 ADMIN、雜湊密碼並建立固定 `ADMIN` 角色的初始化 Service，並以 service 測試驗證合法、無效及重複初始化結果。
- [x] 3.3 實作 `POST /api/setup/admin` DTO、validation、problem response 與 Controller，並以 API 整合測試驗證初始化成功、欄位錯誤及 ADMIN 已存在時不產生資料變更。
- [x] 3.4 建立兩個同時初始化請求的真實 SQLite 併發整合測試，驗證僅一筆 ADMIN 提交且另一請求收到衝突回應。

## 4. 使用者認證與授權後端

- [x] 4.1 實作 Argon2id 密碼編碼、一般使用者註冊 Service 與 `POST /api/auth/register`，並以測試驗證只有 ADMIN 已存在時可建立固定 `USER` 角色、重複名稱遭拒且資料庫不含明文密碼。
- [x] 4.2 實作由環境提供 secret 的短效 JWT 簽發與驗證元件，缺少或弱 secret 時拒絕啟動，並以單元測試驗證 claims、到期、竄改與錯誤簽章。
- [x] 4.3 實作 Spring Security filter chain、JWT HttpOnly/Secure/SameSite Cookie、CSRF token 與角色授權，並以 security 整合測試驗證公開端點、有效 JWT、缺少或無效 JWT、CSRF 及權限不足。
- [x] 4.4 實作登入、登出與 `/api/auth/me` API，統一錯誤訊息且避免記錄敏感內容，並以整合測試驗證成功登入、錯誤帳密、停用帳號、顯示名稱、Cookie 清除與到期工作階段。

## 5. React 啟動狀態與共用 UI

- [x] 5.1 建立集中式 Axios client，支援 credentials、CSRF header、problem response 與 401 處理，並以 mock API 測試驗證請求及失效工作階段處理。
- [x] 5.2 實作 Auth ViewModel 的 checking、setup-required、anonymous、authenticated 狀態及 `/setup/admin` 路由守衛，並以元件測試驗證無 ADMIN 時所有一般路由被導向、已有 ADMIN 時初始化頁被導向登入。
- [x] 5.3 建立現代化響應式認證版面、可存取表單元件與 SweetAlert2 回饋封裝，並以小螢幕與鍵盤操作的元件測試驗證標籤、焦點與錯誤呈現。

## 6. 管理員初始化介面

- [x] 6.1 實作新增管理員 ViewModel 與使用者名稱、密碼、確認密碼表單，成功後刷新初始化狀態並導向登入，並以元件測試驗證 loading、欄位驗證、API 錯誤及成功流程。
- [x] 6.2 在初始化畫面清楚提示「公開服務的第一位完成者將成為管理員」及完成後不可再次使用，並以 UI 測試確認警示在提交前可見。

## 7. 註冊、登入與已登入介面

- [x] 7.1 實作一般使用者註冊 ViewModel 與頁面，成功後導向登入，並以元件測試驗證 ADMIN 存在後頁面可用、輸入錯誤、重複名稱與成功流程。
- [x] 7.2 實作登入 ViewModel 與頁面，成功後呼叫 `/me` 還原使用者資料，並以元件測試驗證錯誤帳密、loading、成功登入與重新整理後狀態。
- [x] 7.3 實作已登入首頁，顯示目前使用者名稱與登出操作，並以元件測試驗證名稱呈現、登出導向及 JWT 失效時回到登入頁。

## 8. 整合與部署驗證

- [x] 8.1 建立端對端測試，從空 SQLite 資料庫驗證強制初始化、建立 ADMIN、阻擋再次初始化、註冊 USER、登入、顯示名稱與登出的完整流程。
- [x] 8.2 建立公開部署設定與操作文件，記載 TLS、JWT secret、SQLite 備份、首次初始化搶先風險及回滾步驟，並由乾淨環境依文件完成一次啟動驗證。
- [x] 8.3 執行後端測試與建置、前端測試與 production build、OpenSpec strict validation，並確認產物不含外部 CDN、secret、SQLite 資料庫或測試憑證。

## Workflow follow-up

- 實作及審查完成後封存此 change，將兩份 capability delta 同步至正式規格。
