# 校园社团管理与招新系统 - API 接口文档

## 1. 概述
本文档以当前仓库中的控制器实现为准，描述系统已存在的 REST API。

- Base URL：`/api`
- 响应格式：统一包装为 `Result<T>`
- 认证方式：`JWT + HttpOnly Cookie`
- 兼容方式：后端过滤器也支持从 `Authorization: Bearer <token>` 中读取 token，主要用于测试或非浏览器客户端

统一响应结构：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

分页结构（实际为 `PageResult<T>`）：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 10,
  "totalPages": 0
}
```

说明：文档不再保留仓库中不存在的接口，例如 `/api/members`、`/api/users/password/reset`、`/api/clubs/{id}/freeze` 等。

---

## 2. 认证接口 `/api/auth`

### 2.1 注册
- `POST /api/auth/register`
- Body：`User` 基本信息

### 2.2 登录
- `POST /api/auth/login`
- Body：

```json
{
  "username": "202501234",
  "password": "secret"
}
```

- 返回：`Result<LoginResponse>`
- `LoginResponse` 当前只包含 `user`
- access token / refresh token 主要通过 Cookie 下发

### 2.3 刷新令牌
- `POST /api/auth/refresh`
- refresh token 从 Cookie 中读取，不要求前端显式传 JSON Body

### 2.4 登出
- `POST /api/auth/logout`
- 后端清理 refresh token 并清除 Cookie

### 2.5 忘记密码
- `POST /api/auth/forgot-password`
- Body：

```json
{
  "email": "user@example.com"
}
```

### 2.6 重置密码
- `POST /api/auth/reset-password`
- Body：

```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "newPassword123"
}
```

---

## 3. 用户接口 `/api/users` 与 `/api/files`

### 3.1 获取当前用户
- `GET /api/users/me`

### 3.2 更新当前用户资料
- `PUT /api/users/me`
- 可更新字段：`realName`、`avatarUrl`、`mobile`、`email`、`interests`

### 3.3 修改当前用户密码
- `POST /api/users/me/password`
- Body：

```json
{
  "oldPassword": "old",
  "newPassword": "new"
}
```

### 3.4 获取我的活动
- `GET /api/users/me/activities`

### 3.5 文件上传
- `POST /api/files/upload`
- 表单字段：`file`

---

## 4. 社团接口 `/api/clubs`

### 4.1 创建社团
- `POST /api/clubs`

### 4.2 查询社团列表
- `GET /api/clubs?keyword=&category=&page=0&size=10`
- 当前控制器参数默认 `page=0`
- 返回 `PageResult<ClubVO>`，响应中的 `page` 会被包装为从 1 开始

### 4.3 推荐社团
- `GET /api/clubs/recommended`

### 4.4 获取我的社团
- `GET /api/clubs/my`

### 4.5 获取社团详情
- `GET /api/clubs/{id}`

### 4.6 更新社团
- `PUT /api/clubs/{id}`

### 4.7 审核社团
- `POST /api/clubs/{id}/approve`

### 4.8 添加成员
- `POST /api/clubs/{id}/members?userId={userId}&role={role}`

### 4.9 查询社团成员
- `GET /api/clubs/{id}/members`

### 4.10 导出社团成员
- `GET /api/clubs/{id}/members/export`

### 4.11 更新成员角色
- `PUT /api/clubs/{id}/members/{userId}/role?role={role}`

### 4.12 退出社团
- `DELETE /api/clubs/{id}/members/me`

### 4.13 移除成员
- `DELETE /api/clubs/{id}/members/{userId}`

### 4.14 申请解散社团
- `POST /api/clubs/{id}/dissolve`

### 4.15 撤回解散申请
- `POST /api/clubs/{id}/dissolve/withdraw`

### 4.16 强制删除社团
- `DELETE /api/clubs/{id}/force`

### 4.17 恢复社团
- `POST /api/clubs/{id}/recover`

---

## 5. 招新接口 `/api/recruit`

### 5.1 创建招新批次
- `POST /api/recruit/batches`

### 5.2 查询某社团的招新批次
- `GET /api/recruit/batches?clubId={clubId}`

### 5.3 查询单个批次
- `GET /api/recruit/batches/{id}`

### 5.4 新增表单字段
- `POST /api/recruit/fields`

### 5.5 查询表单字段
- `GET /api/recruit/fields?batchId={batchId}`

### 5.6 提交报名
- `POST /api/recruit/applications`

### 5.7 查询报名
- `GET /api/recruit/applications`
- `GET /api/recruit/applications?batchId={batchId}`

### 5.8 初审
- `POST /api/recruit/applications/{id}/first-review?pass=true&comment=...`

### 5.9 终审
- `POST /api/recruit/applications/{id}/final-review?pass=true&comment=...`

### 5.10 查询当前处于招新状态的社团
- `GET /api/recruit/active-clubs`

### 5.11 导出报名结果
- `GET /api/recruit/batches/{batchId}/applications/export`

---

## 6. 活动接口 `/api/activities`

### 6.1 创建活动
- `POST /api/activities`

### 6.2 分页查询活动
- `GET /api/activities`
- 支持按 `clubId` 等条件查询

### 6.3 查询社团活动
- `GET /api/activities/club/{clubId}`

### 6.4 查询活动详情
- `GET /api/activities/{id}`

### 6.5 报名活动
- `POST /api/activities/{id}/signup`

### 6.6 活动签到
- `POST /api/activities/{id}/signin`

### 6.7 查询报名记录
- `GET /api/activities/{id}/signups`

### 6.8 导出签到记录
- `GET /api/activities/{id}/checkins/export`

### 6.9 查询我的报名
- `GET /api/activities/my-signups`

### 6.10 更新活动
- `PUT /api/activities/{id}`

### 6.11 删除活动
- `DELETE /api/activities/{id}`

---

## 7. 公告与通知

### 7.1 公告接口 `/api/notices`
- `GET /api/notices/{id}`
- `GET /api/notices`
- `POST /api/notices`
- `DELETE /api/notices/{id}`

### 7.2 通知接口 `/api/notifications`
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `PUT /api/notifications/{id}/read`
- `PUT /api/notifications/read-all`

---

## 8. 资源接口 `/api/resources`

### 8.1 查询资源定义
- `GET /api/resources/list`

### 8.2 管理端查询资源定义
- `GET /api/resources/admin/list`

### 8.3 新增资源定义
- `POST /api/resources/admin`

### 8.4 更新资源定义
- `PUT /api/resources/admin`

### 8.5 删除资源定义
- `DELETE /api/resources/admin/{id}`

### 8.6 提交资源申请
- `POST /api/resources/applications`

### 8.7 查询某社团的资源申请
- `GET /api/resources/clubs/{clubId}/applications`

### 8.8 查询待审批申请
- `GET /api/resources/applications/pending`

### 8.9 审批通过
- `POST /api/resources/applications/{id}/approve`

### 8.10 审批驳回
- `POST /api/resources/applications/{id}/reject`

---

## 9. 财务接口 `/api/finance`

### 9.1 创建流水
- `POST /api/finance/transactions`

### 9.2 查询社团流水
- `GET /api/finance/clubs/{clubId}/transactions`

### 9.3 查询社团余额
- `GET /api/finance/clubs/{clubId}/balance`

### 9.4 审批流水
- `POST /api/finance/transactions/{id}/approve`

### 9.5 驳回流水
- `POST /api/finance/transactions/{id}/reject`

---

## 10. 管理与统计接口

### 10.1 系统管理 `/api/admin`
- `GET /api/admin/clubs/pending`
- `GET /api/admin/clubs/dissolving`
- `POST /api/admin/clubs/{id}/approve`
- `POST /api/admin/clubs/{id}/approve-dissolution`
- `POST /api/admin/clubs/{id}/reject-dissolution`
- `DELETE /api/admin/clubs/{id}`
- `POST /api/admin/users/{id}/status?status=...`
- `POST /api/admin/roles/cleanup`

### 10.2 审计日志 `/api/admin/audit-logs`
- `GET /api/admin/audit-logs`

### 10.3 Dashboard `/api/dashboard`
- `GET /api/dashboard/stats`

### 10.4 统计 `/api/stats`
- `GET /api/stats/system`
- `GET /api/stats/club/{clubId}`

---

## 11. AI 接口

### 11.1 社团问答 `/api/club/chat`
- `POST /api/club/chat`
- Body：

```json
{
  "message": "我想找技术类社团",
  "sessionId": "optional-session-id"
}
```

说明：
- 已登录用户会自动使用用户身份作为会话标识
- 匿名用户可传 `sessionId`
- 控制器内置简单频率限制

---

## 12. 说明
以下内容已从文档中移除，因为当前代码仓库中不存在或并未这样实现：
- `/api/members` 独立成员模块接口
- `/api/users/password/reset`
- `/api/clubs/{id}/freeze`
- 通过 JSON Body 传 refresh token 的固定约定
- 统一 `Bearer Token` 作为浏览器主认证方式
- 返回结构中的 `records` 字段

（本文档按当前控制器实现修订，后续若控制器变更，应同步更新。）
