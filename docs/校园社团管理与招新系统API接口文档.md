# 校园社团管理与招新系统 - API 接口文档

## 1. 概述
本接口文档描述校园社团管理与招新系统后端 RESTful API。所有响应统一使用 JSON，编码 UTF-8；认证使用 JWT Bearer Token；时间采用 ISO 8601（含时区或使用 `YYYY-MM-DDTHH:mm:ss` 默认东八区说明）。

Base URL: `https://api.university-club.com` （示例）

版本：v1

---

## 2. 认证与通用规范
Header: `Authorization: Bearer <access_token>`
响应统一结构：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {}
}
```
错误示例：
```json
{
  "code": "VALIDATION_ERROR",
  "message": "招新批次不存在",
  "data": null
}
```

分页参数：`page`（从1开始），`size`；返回含 `page`,`size`,`total`,`records`。

---

## 3. 认证服务 `/api/auth`

### 3.1 登录
- Method: POST
- URL: `/api/auth/login`
- Body:
```json
{ "username": "202501234", "password": "secret" }
```
- Success:
```json
{
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyR...",
    "expiresIn": 3600,
    "user": { "id": 12, "realName": "张三", "roles": ["STUDENT"] }
  }
}
```

### 3.2 刷新令牌
POST `/api/auth/refresh`  Body `{ "refreshToken": "eyR..." }`

### 3.3 登出
POST `/api/auth/logout`  Header 携带 AccessToken，服务端列入黑名单（可选）。

---

## 4. 用户与角色 `/api/users`

### 4.1 获取当前用户
GET `/api/users/me`
返回：`id, username, realName, roles, lastLoginAt`。

### 4.2 修改个人资料
PUT `/api/users/me`
Body: `{ "email": "a@b.com", "mobile": "13800001111" }`

### 4.3 重置密码（忘记密码）
POST `/api/users/password/reset` Body: `{ "username": "202501234", "email": "a@b.com" }` → 发邮件链接。

---

## 5. 社团管理 `/api/clubs`

### 5.1 创建社团（管理员/负责人）
POST `/api/clubs`
```json
{
  "name": "科技创新协会",
  "shortName": "科创",
  "category": "科技",
  "description": "科技实践与创新项目社团"
}
```
返回创建的社团对象。状态默认 `PENDING`。

### 5.2 审核社团（管理员）
POST `/api/clubs/{id}/approve` Body: `{ "decision": "PASS", "remark": "资料齐全" }`

### 5.3 查询社团列表（公开）
GET `/api/clubs?keyword=科技&category=科技&page=1&size=10`
返回活跃度指标：`memberCount, activityCount`。

### 5.4 更新社团信息（负责人）
PUT `/api/clubs/{id}` Body: `{ "logoUrl": "https://...", "description": "更新简介" }`

### 5.5 冻结社团（管理员）
POST `/api/clubs/{id}/freeze` Body: `{ "reason": "违规内容" }`

---

## 6. 招新批次 `/api/recruit/batches`

### 6.1 创建招新批次（干部）
POST `/api/recruit/batches`
```json
{
  "clubId": 10,
  "title": "2025春季招新",
  "description": "欢迎加入",
  "startTime": "2025-03-01T08:00:00",
  "endTime": "2025-03-10T23:59:59",
  "quota": 50
}
```

### 6.2 定义表单字段
POST `/api/recruit/batches/{batchId}/fields`
```json
[
  { "fieldKey": "strength", "label": "个人特长", "type": "TEXT", "required": true },
  { "fieldKey": "intent", "label": "加入动机", "type": "TEXT", "required": true },
  { "fieldKey": "department", "label": "期望部门", "type": "SELECT", "options": ["技术部", "宣传部"], "required": true }
]
```

### 6.3 学生提交报名
POST `/api/recruit/applications`
```json
{
  "batchId": 18,
  "applyData": {
    "strength": "熟悉Java与前端",
    "intent": "希望提升实践能力",
    "department": "技术部"
  }
}
```
响应：状态 `PENDING`。

### 6.4 初审
POST `/api/recruit/applications/{id}/first-review`
```json
{ "decision": "PASS", "comment": "表现不错" }
```

### 6.5 复审
POST `/api/recruit/applications/{id}/final-review`
```json
{ "decision": "PASS", "comment": "录取" }
```
成功后触发成员创建。

### 6.6 查询报名列表（干部）
GET `/api/recruit/batches/{batchId}/applications?status=FIRST_PASS&page=1&size=20`

### 6.7 导出报名（干部）
GET `/api/recruit/batches/{batchId}/export` → 返回文件下载链接。

---

## 7. 成员管理 `/api/members`

### 7.1 查询社团成员
GET `/api/members?clubId=10&role=OFFICER&page=1&size=20`

### 7.2 调整成员角色（负责人）
POST `/api/members/{id}/role` Body: `{ "roleCode": "OFFICER" }`

### 7.3 更新成员状态
POST `/api/members/{id}/status` Body: `{ "status": "TRIAL" }`

---

## 8. 活动管理 `/api/activities`

### 8.1 创建活动
POST `/api/activities`
```json
{
  "clubId": 10,
  "title": "技术分享会",
  "type": "讲座",
  "location": "教学楼A101",
  "maxParticipants": 100,
  "signupStart": "2025-04-01T08:00:00",
  "signupEnd": "2025-04-03T23:59:59",
  "startTime": "2025-04-05T19:00:00",
  "endTime": "2025-04-05T21:00:00",
  "needAttendance": true
}
```

### 8.2 报名活动
POST `/api/activities/{id}/signup`

### 8.3 取消报名
DELETE `/api/activities/{id}/signup`

### 8.4 签到（二维码调用）
POST `/api/activities/{id}/attendance`
```json
{ "userId": 12, "token": "qr_signature" }
```

### 8.5 获取活动详情
GET `/api/activities/{id}` 返回报名人数、签到人数、状态。

### 8.6 活动归档
POST `/api/activities/{id}/archive` Body: `{ "summary": "活动顺利进行" }`

---

## 9. 公告与消息 `/api/notices`

### 9.1 发布公告
POST `/api/notices`
```json
{
  "clubId": 10,
  "title": "本周例会通知",
  "content": "周五晚七点召开例会",
  "scope": "CLUB"
}
```

### 9.2 全局公告（管理员）
POST `/api/notices` Body `{"scope": "GLOBAL", "title": "校团委通知", ...}`

### 9.3 公告列表
GET `/api/notices?clubId=10&page=1&size=10`

### 9.4 标记已读
POST `/api/notices/{id}/read`

---

## 10. 附件上传 `/api/files`
POST `/api/files/upload` (Multipart)
响应：`{ "url": "https://oss/...", "fileName": "xxx.png" }`
限制：大小 ≤ 10MB，类型白名单（image/*, application/pdf）。

---

## 11. 统计报表 `/api/stats`

### 11.1 全局统计（管理员）
GET `/api/stats/global` 返回：`clubCount, activeClubCount, totalMembers, recruitConversionRate`。

### 11.2 社团内部统计
GET `/api/stats/clubs/{clubId}` 返回：`applications, accepted, activities, averageAttendanceRate, memberGrowth`。

---

## 12. 审计日志 `/api/audit`
GET `/api/audit?userId=12&action=LOGIN&page=1&size=20`

---

## 13. 通用错误码
| code | message | 说明 |
|------|---------|------|
| SUCCESS | 操作成功 | 正常 |
| AUTH_INVALID | 认证失败 | Token 无效 |
| PERMISSION_DENIED | 权限不足 | 无访问权限 |
| VALIDATION_ERROR | 参数校验失败 | 字段不符合要求 |
| RESOURCE_NOT_FOUND | 资源不存在 | ID错误 |
| CONFLICT_LIMIT | 名额已满 | 报名达到上限 |
| SYSTEM_ERROR | 系统错误 | 未捕获异常 |

---

## 14. 安全与速率限制
- 登录接口：IP + 用户名组合 5 次失败锁 10 分钟。
- 报名接口：单用户每分钟最多 10 次尝试。
- 返回头：`X-Rate-Limit-Remaining`。

---

## 15. 版本规划
- v1：核心招新/活动/公告
- v1.1：资源申请、消息推送
- v1.2：统计分析细化、机器学习推荐（活动推荐）

---

## 16. 示例：报名完整请求与响应
请求：
```json
{
  "batchId": 33,
  "applyData": {
    "strength": "组织协调能力强",
    "intent": "想提高自己并服务同学",
    "department": "宣传部"
  }
}
```
响应：
```json
{
  "code": "SUCCESS",
  "data": {
    "id": 9901,
    "batchId": 33,
    "status": "PENDING"
  }
}
```

（本 API 文档将在实现时与实际返回字段同步迭代）

