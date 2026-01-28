为了实现“通过SQL文件建立表结构和初始数据，且每次运行重置”，我将执行以下计划：

### 1. 编写 SQL 脚本
我将根据刚刚读取的实体类定义，创建两个标准的 SQL 文件：
*   **`src/main/resources/schema.sql`**:
    *   包含所有表的 `DROP TABLE IF EXISTS` 语句。
    *   包含所有表的 `CREATE TABLE` 语句，严格对应 Entity 定义（包含主键、外键、约束）。
    *   涵盖表：`t_user`, `t_role`, `t_user_role`, `t_club`, `t_club_tag`, `t_member`, `t_activity`, `t_activity_signup`, `t_activity_attendance`, `t_recruit_batch`, `t_recruit_form_field`, `t_recruit_application`, `t_notice`, `t_notice_read`, `t_audit_log`。

*   **`src/main/resources/data.sql`**:
    *   插入基础角色 (`ROLE_USER`, `ROLE_ADMIN` 等)。
    *   插入默认管理员账号 (`admin/123456`)。
    *   插入测试社团数据（如“编程俱乐部”）。

### 2. 修改系统配置
修改 `src/main/resources/application.properties`：
*   **禁用 Hibernate 自动建表**: 设置 `spring.jpa.hibernate.ddl-auto=none` (或 `validate`)，确保表结构完全由 `schema.sql` 控制。
*   **启用 SQL 初始化**: 设置 `spring.sql.init.mode=always`，确保每次启动都执行 SQL 脚本。
*   **编码设置**: 确保 `spring.sql.init.encoding=UTF-8` 防止中文乱码。

### 3. 清理旧代码
*   删除之前创建的 `src/main/java/com/cloud/community/config/DataInitializer.java`，避免 Java 代码与 SQL 脚本冲突。

### 4. 验证与测试
*   启动后端服务，观察日志确保 SQL 脚本执行成功。
*   启动前端服务，验证登录和数据展示是否正常。

此方案完全符合您“SQL文件放在resources下”、“每次运行重新加载数据”的要求，并能确保数据的绝对安全（重置状态）。
