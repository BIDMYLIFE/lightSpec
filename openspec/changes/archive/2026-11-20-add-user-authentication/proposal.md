# Proposal

## Why

目前專案尚未提供使用者身分識別與存取控制，無法支援個人化功能或保護 API。需要建立可離線部署、資料易於移植的帳號系統，並讓全新資料庫能透過網頁完成第一位管理員初始化。

## What Changes

- 新增一般使用者以使用者名稱及密碼自行註冊的流程；第一位管理員建立後仍持續開放註冊。
- 新增使用者登入、JWT 驗證、登出及取得目前使用者資訊的流程，登入後顯示使用者名稱。
- 新增公開的首次管理員初始化流程：資料庫沒有 `ADMIN` 記錄時，所有訪客強制導向管理員建立頁面。
- 第一位管理員建立後，前端不再顯示初始化頁面，後端也拒絕所有再次初始化的請求。
- 使用 SQLite 保存帳號資料，並以版本化 migration 維持未來移植至其他關聯式資料庫的能力。
- 新增 React MVVM、Tailwind CSS、Axios 與 SweetAlert2 所需的認證與初始化介面行為。
- 此公開初始化模式刻意不使用 setup token、來源 IP 限制或本機管理介面；接受公開服務首次啟動時，最先完成初始化的訪客可成為管理員的風險。

## Capabilities

### New Capabilities

- `user-authentication`: 一般使用者註冊、帳密登入、JWT 驗證、登出及登入後顯示使用者名稱。
- `admin-bootstrap`: 無管理員資料時的全站導向、第一位管理員建立，以及建立後停用初始化功能。

### Modified Capabilities

無。

## Impact

- 建立 Spring Boot 後端專案及 Repository、Service、MVC Controller、Web API Controller 分層。
- 建立 React 前端專案與 MVVM 狀態管理、響應式頁面、Axios API 用戶端及 SweetAlert2 回饋。
- 新增 Spring Security、JWT、密碼雜湊、輸入驗證、SQLite 驅動、JPA 與資料庫 migration 相關依賴。
- 新增使用者資料表、角色欄位、唯一使用者名稱約束與初始化競態保護。
- 新增認證、授權、初始化流程的單元、整合及前端測試。
