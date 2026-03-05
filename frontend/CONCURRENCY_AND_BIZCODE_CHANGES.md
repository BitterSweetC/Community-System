# 并发加锁与业务错误码改造说明

## 1. 改造背景

本次改造聚焦两个问题：

1. 并发场景下可能出现状态竞争（重复审批、重复签到、状态覆盖等）。
2. 前端只能拿到通用错误信息，无法稳定识别“并发冲突类错误”并做精确提示。

为此，进行了两类升级：

1. 后端关键流程补充数据库悲观锁（`PESSIMISTIC_WRITE`）与二次状态校验。
2. 引入统一业务异常 `BusinessException(code, message)`，输出稳定业务码与中文文案。

---

## 2. 主要改动清单

### 2.1 并发安全（加锁）

#### 资源申请/审批（已完成）

- 同一资源申请与审批流程采用资源行锁串行化处理。
- 审批通过前再次进行库存/时间冲突校验（防止待审批堆积后“穿透通过”）。

涉及：

- `community-core/.../ResourceRepository.java`
- `community-core/.../ResourceApplicationRepository.java`
- `community-club/.../ResourceServiceImpl.java`

#### 活动报名/签到（已完成）

- 报名时对用户和活动进行加锁读取，降低并发重复报名窗口。
- 签到时对报名记录加锁，防止重复签到并发写入。

涉及：

- `community-core/.../ActivityRepository.java`
- `community-core/.../ActivitySignupRepository.java`
- `community-activity/.../ActivityServiceImpl.java`

#### 招新审批（已完成）

- 一审/终审读取申请改为加锁读取（`findByIdForUpdate`）。
- 增加状态机校验（已处理不可重复处理、一审未通过不可终审）。
- 终审通过时仍保留配额校验（已存在）并在锁内执行。

涉及：

- `community-core/.../RecruitApplicationRepository.java`
- `community-recruit/.../RecruitServiceImpl.java`

#### 社团状态流转（已完成）

- 审批社团、撤销解散、通过解散、拒绝解散、强制解散、恢复社团等操作改为社团行锁读取。
- 避免后台并发操作与定时任务冲突导致状态覆盖。

涉及：

- `community-club/.../ClubServiceImpl.java`

#### 用户角色变更与清理任务（已完成）

- 角色授予/移除流程改为用户行锁读取后再修改角色集合，避免角色丢更新。
- 定时清理任务改为“按 ID 二次加锁 + 状态/时间复核”后再迁移/删除。

涉及：

- `community-club/.../ClubServiceImpl.java`
- `community-club/.../ClubCleanupTask.java`

---

### 2.2 统一业务异常与业务码

新增：

- `community-core/.../BusinessException.java`

增强：

- `community-core/.../GlobalExceptionHandler.java`
  - `BusinessException` 统一透传 `code`、`message`
  - `409xx` 返回 HTTP 409（冲突）
  - 其它业务码返回 HTTP 400

---

### 2.3 前端错误码透传

在 `frontend/src/api/axios.js` 中：

- 非 200 业务响应时，将后端 `res.code` 透传为 `error.bizCode`
- 前端既可显示 `error.message`，也可按 `bizCode` 分支处理

---

## 3. 已落地业务码（核心并发冲突）

### 活动

- `40901`：重复报名
- `40902`：重复签到

### 招新

- `40911`：重复提交申请
- `40912`：一审已处理
- `40913`：一审未通过，不能终审
- `40914`：终审已处理
- `40915`：名额已满
- `40916`：已是社团成员

### 社团

- `40921`：重复添加成员
- `40922`：社团不在解散流程
- `40923`：社团非已解散状态，无法恢复

### 财务

- `40931`：财务申请已处理（重复审批）

### 资源

- `40041`：资源编号不能为空
- `40042`：申请数量非法
- `40941`：资源申请已处理（重复审批）
- `40942`：申请数量超过库存
- `40943`：时间段库存不足
- `40944`：时间段场地冲突

---

## 4. 改造效果

1. **并发一致性提升**
   - 关键审批/状态流转路径在数据库层完成串行化，减少双击、重复请求、多人并发导致的数据穿透。

2. **错误可识别性提升**
   - 前端可通过 `bizCode` 精准识别冲突类型，而不是仅靠模糊文案判断。

3. **用户提示更友好**
   - 业务冲突场景统一返回中文提示，可直接展示给用户。

4. **后续扩展更稳定**
   - 后续前端可按业务码做更细粒度交互（例如自动提示“换时间段/刷新列表/禁用按钮”）。

---

## 5. 验证结果

已完成编译验证：

- 后端：`community-activity, community-recruit, community-club` 联编通过（含依赖模块）
- 前端：`npm run build` 通过

---

## 6. 后续建议（可选）

1. 为高频冲突接口补充并发单测（双线程重复审批/签到/状态流转）。
2. 前端在关键按钮处增加“请求中禁用”与幂等提交保护。
3. 将业务码整理为统一文档（接口文档附录）供前后端共同维护。
