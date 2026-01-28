# 校园社团管理与招新系统

## 项目简介

这是一个基于 Spring Boot 3.x 开发的校园社团管理与招新系统后端服务，提供完整的社团管理、招新、活动管理、公告通知等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.0
- **安全框架**: Spring Security + JWT
- **数据持久化**: Spring Data JPA
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **构建工具**: Maven

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+ (可选，用于缓存和分布式锁)

## 快速开始

### 1. 数据库准备

创建数据库：

```sql
CREATE DATABASE community_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 2. 配置文件

修改 `src/main/resources/application.properties` 中的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/community_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. 运行项目

```bash
# 使用 Maven 运行
mvn spring-boot:run

# 或者先编译再运行
mvn clean package
java -jar target/Community-0.0.1-SNAPSHOT.jar
```

项目启动后，访问 `http://localhost:8080`

### 4. 初始化数据（可选）

系统使用 JPA 自动建表（`spring.jpa.hibernate.ddl-auto=update`），首次运行会自动创建表结构。

建议手动创建初始角色数据：

```sql
INSERT INTO t_role (code, name, description, created_at, updated_at) VALUES
('VISITOR', '游客', '未登录用户', NOW(), NOW()),
('STUDENT', '学生', '普通学生用户', NOW(), NOW()),
('CLUB_ADMIN', '社团管理员', '社团内部管理人员', NOW(), NOW()),
('ADMIN', '系统管理员', '系统管理员', NOW(), NOW());
```

## API 文档

### 认证接口

- `POST /api/auth/login` - 用户登录

### 用户接口

- `GET /api/users/me` - 获取当前用户信息
- `PUT /api/users/me` - 更新用户信息

### 社团接口

- `GET /api/clubs` - 搜索社团列表
- `GET /api/clubs/{id}` - 获取社团详情
- `POST /api/clubs` - 创建社团（需认证）
- `PUT /api/clubs/{id}` - 更新社团信息
- `POST /api/clubs/{id}/approve` - 审核社团（管理员）
- `POST /api/clubs/{id}/freeze` - 冻结社团（管理员）

### 招新接口

- `POST /api/recruit/batches` - 创建招新批次
- `POST /api/recruit/batches/{batchId}/fields` - 定义表单字段
- `POST /api/recruit/applications` - 提交报名申请
- `POST /api/recruit/applications/{id}/first-review` - 初审
- `POST /api/recruit/applications/{id}/final-review` - 复审
- `GET /api/recruit/batches/{batchId}/applications` - 查询报名列表

### 活动接口

- `POST /api/activities` - 创建活动
- `GET /api/activities/{id}` - 获取活动详情
- `POST /api/activities/{id}/signup` - 报名活动
- `DELETE /api/activities/{id}/signup` - 取消报名
- `POST /api/activities/{id}/attendance` - 活动签到
- `POST /api/activities/{id}/archive` - 活动归档

### 公告接口

- `POST /api/notices` - 发布公告
- `GET /api/notices` - 查询公告列表
- `GET /api/notices/{id}` - 获取公告详情
- `POST /api/notices/{id}/read` - 标记已读

## 项目结构

```
src/main/java/com/cloud/community/
├── common/          # 通用类（BaseEntity, ApiResponse等）
├── config/          # 配置类（Security, JPA等）
├── controller/      # REST控制器
├── dto/             # 数据传输对象
├── entity/          # 实体类
├── exception/       # 异常处理
├── repository/      # 数据访问层
├── security/        # 安全相关
├── service/         # 业务逻辑层
└── util/            # 工具类
```

## 注意事项

1. **JWT Secret**: 生产环境请修改 `application.properties` 中的 `jwt.secret` 为强密钥
2. **数据库**: 确保 MySQL 服务已启动
3. **Redis**: Redis 为可选，如未安装可注释相关配置
4. **时区**: 系统默认使用 Asia/Shanghai 时区

## 开发说明

- 使用 JPA 自动建表，首次运行会自动创建表结构
- 所有 API 返回统一格式：`{code, message, data}`
- 认证使用 JWT Bearer Token，在请求头中添加：`Authorization: Bearer <token>`
- 分页参数：`page`（从1开始），`size`（每页数量）

## 许可证

本项目仅供学习和参考使用。

