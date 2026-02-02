为了将当前的聚合架构向设计文档的微服务/模块化架构靠拢，同时保证程序的可运行性，我制定了以下重构计划。我们将把 `community-club` 这个大模块拆解为 4 个专注于特定业务领域的新模块。

### 1. 新的模块结构
我们将创建以下 Maven 模块（加粗为新增）：

*   `community-core`: (保持不变) 基础实体、数据仓库、通用工具。
*   **`community-user`**: (新增) 包含用户服务 (`UserService`)、权限校验 (`PermissionService`)。
*   **`community-notice`**: (新增) 包含通知 (`NotificationService`)、公告 (`NoticeService`)。
*   **`community-activity`**: (新增) 包含活动服务 (`ActivityService`)。
*   **`community-recruit`**: (新增) 包含招新服务 (`RecruitService`)。
*   `community-club`: (保留核心) 包含社团服务 (`ClubService`)、财务 (`FinanceService`)、聊天 (`ChatService`)。
*   `community-admin`: (保持不变) 管理后台接口，将依赖所有业务模块。
*   `community-gateway`: (保持不变) 启动入口，聚合所有业务模块。

### 2. 依赖关系调整
为了避免循环依赖并保持层级清晰，新的依赖链如下：

1.  **底层**: `community-user`, `community-notice` (仅依赖 `community-core`)
2.  **中层**: `community-club` (依赖 `community-core`, `community-notice`)
3.  **上层**:
    *   `community-activity` (依赖 `community-user`, `community-notice`)
    *   `community-recruit` (依赖 `community-club`, `community-user`)
4.  **聚合层**: `community-admin`, `community-gateway` (依赖以上所有模块)

### 3. 执行步骤

#### 第一阶段：基础设施搭建
1.  在根目录下创建 4 个新模块文件夹：`community-user`, `community-notice`, `community-activity`, `community-recruit`。
2.  为每个模块创建标准的 Maven 目录结构 (`src/main/java`, `pom.xml`)。
3.  配置各模块的 `pom.xml`，定义上述依赖关系。
4.  更新根目录 `pom.xml`，注册新模块。

#### 第二阶段：代码迁移与包重构
我们将代码从 `community-club` 移动到新模块，并调整包名以符合模块规范：
1.  **User**: `UserService`, `PermissionService` -> `com.cloud.community.user`
2.  **Notice**: `NoticeService`, `NotificationService` -> `com.cloud.community.notice`
3.  **Activity**: `ActivityService` -> `com.cloud.community.activity`
4.  **Recruit**: `RecruitService` -> `com.cloud.community.recruit`
5.  **Club**: 保留剩余服务在 `com.cloud.community.club`

*注：Controller 层也会跟随 Service 一起迁移到对应模块。*

#### 第三阶段：依赖修复与验证
1.  更新 `community-admin` 和 `community-gateway` 的 `pom.xml`，引入所有新模块。
2.  全局修复因包名变更导致的 `import` 错误。
3.  编译整个项目，确保无编译错误。
4.  启动应用进行基本功能验证。

此方案在物理上实现了模块隔离，逻辑上保持了单体应用的运行模式，满足“划分后程序仍可运行”的要求。
