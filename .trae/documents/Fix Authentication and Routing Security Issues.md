# 修复数据为空及路由权限漏洞

## 问题根源分析
1.  **后端数据为空 (`NoSuchElementException`)**：
    *   **原因**：`SecurityConfig` 中配置了 `/api/clubs/**` (GET) 为 `permitAll`（允许匿名访问）。由于 `/api/clubs/my` 匹配该规则，Spring Security 允许未携带 Token 或 Token 无效的请求通过。
    *   **后果**：请求进入 `ClubController` 时，身份为 `anonymousUser`。`getCurrentUser()` 尝试在数据库查找名为 "anonymousUser" 的用户，结果为空，导致 `orElseThrow` 抛出 `NoSuchElementException`。
2.  **前端路由越权**：
    *   **原因**：前端路由守卫 (`router.beforeEach`) 仅检查了 `requiresAuth`（是否登录），但未检查 `meta.role`（角色权限）。
    *   **后果**：社团管理员（Club Admin）可以通过手动修改 URL 访问仅限系统管理员（Admin）的页面。

## 实施计划

### 1. 后端修复 (Spring Boot)
*   **修正安全配置 (`SecurityConfig.java`)**：
    *   在 `authorizeHttpRequests` 链中，显式添加 `.requestMatchers("/api/clubs/my").authenticated()`。
    *   **关键点**：必须放在 `/api/clubs/**` 的 `permitAll` 规则**之前**，因为 Spring Security 按顺序匹配规则。
*   **优化用户获取逻辑 (`ClubController.java`, `AdminController.java`)**：
    *   改进 `getCurrentUser()` 方法，增加对 `anonymousUser` 或未认证状态的检查。
    *   若未认证，直接抛出明确的异常（如 "用户未登录"），而非通用的系统错误。

### 2. 前端修复 (Vue.js)
*   **完善路由守卫 (`router/index.js`)**：
    *   在 `beforeEach` 中增加角色检查逻辑。
    *   获取目标路由的 `meta.role`。
    *   对比当前用户的角色列表 (`authStore.user.roles`)。
    *   若权限不足，重定向至 `/home` 或显示错误提示。

## 验证方案
1.  **后端**：再次调用 `GET /api/clubs/my`，应返回 403 Forbidden（未登录时）或正确的数据（登录时），不再报 500 错误。
2.  **前端**：使用社团管理员账号登录，尝试访问 `/admin`，应被自动拦截并跳转回首页或社团管理页。
