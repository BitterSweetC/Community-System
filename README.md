# 校园社团管理与招新系统

一个基于 `Spring Boot 3`、`Vue 3`、`FastAPI` 的校园社团管理与招新系统。

当前仓库的实际形态是：
- Java 后端采用 **Maven 多模块单体** 架构
- 前端采用 **Vue 3 + JavaScript + Vite**
- AI 能力由独立的 **FastAPI RAG / 推荐服务** 提供
- 基础设施依赖 **MySQL / Redis / RabbitMQ / Prometheus / Grafana**

## 项目能力

当前已落地的主要功能：

- 用户注册、登录、刷新、登出、忘记密码
- 社团创建、查询、推荐、成员管理、解散流程
- 招新批次、表单字段、报名、初审/终审、导出
- 活动创建、报名、签到、签到导出
- 公告发布、站内通知、未读统计
- 资源申请与审批
- 财务流水与审批
- 审计日志与统计接口
- AI 问答与社团推荐

## 技术栈

### 后端
- `Spring Boot 3.2.x`
- `Spring Security + JWT`
- `Spring Data JPA`
- `MySQL 8.0`
- `Redis`
- `RabbitMQ`
- `Micrometer + Prometheus`

### 前端
- `Vue 3`
- `Vite`
- `Pinia`
- `Vue Router`
- `Element Plus`
- `ECharts`

### AI / 智能体
- `FastAPI`
- `Chroma`
- `DeepSeek API`

### 文件与通知
- 阿里云 `OSS`
- `SMTP` 邮件

## 仓库结构

```text
.
├─ community-core        # 公共实体、仓储、统一返回体、审计、OSS、邮件等
├─ community-gateway     # Spring Boot 启动入口、认证、安全、Dashboard、WebSocket
├─ community-user        # 用户资料、密码修改、文件上传
├─ community-club        # 社团、成员、推荐、资源、财务、统计、聊天入口
├─ community-recruit     # 招新批次、表单字段、报名、审核、导出
├─ community-activity    # 活动、报名、签到、导出
├─ community-notice      # 公告、通知、RabbitMQ 消费
├─ community-admin       # 系统管理、审计日志
├─ frontend              # Vue 3 前端
├─ agent                 # FastAPI RAG / 推荐服务
├─ monitoring            # Prometheus 配置
└─ docs                  # 项目文档
```

## 运行方式

### 方式一：Docker Compose

项目根目录已提供 `docker-compose.yml`，可启动：

- `mysql`
- `app`
- `frontend`
- `redis`
- `rabbitmq`
- `rag-service`
- `prometheus`
- `grafana`

启动命令：

```bash
docker compose up -d --build
```

默认访问地址：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`
- RabbitMQ 管理台：`http://localhost:15672`
- Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`

### 方式二：本地分别启动

#### 1. 准备基础依赖
- JDK `17+`
- Maven `3.6+`
- MySQL `8.0+`
- Redis
- RabbitMQ
- Node.js `20+`
- Python `3.10+`（用于 `agent`）

#### 2. 启动后端

```bash
./mvnw.cmd -pl community-gateway -am spring-boot:run
```

或先编译：

```bash
./mvnw.cmd -pl community-gateway -am clean package
```

#### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

#### 4. 启动 AI 服务

```bash
cd agent
pip install -r requirements.txt
python main.py
```

## 配置说明

后端主配置位于：

- `community-gateway/src/main/resources/application.properties`

重点配置项包括：

- 数据库：`DB_HOST`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- Redis：`REDIS_HOST`、`REDIS_PASSWORD`
- RabbitMQ：`RABBITMQ_HOST`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
- JWT：`JWT_SECRET`
- AI 服务：`RAG_SERVICE_URL`
- OSS：`OSS_ENDPOINT`、`OSS_BUCKET_NAME`
- 邮件：`MAIL_HOST`、`MAIL_USERNAME`、`MAIL_PASSWORD`

推荐通过环境变量注入，不建议把生产密钥直接写入配置文件。

## 认证说明

当前系统实际认证方式为：

- 后端签发 `JWT`
- 浏览器通过 `HttpOnly Cookie` 持有 access token / refresh token
- 前端通过 `withCredentials` 自动携带 Cookie
- 后端也兼容 `Authorization: Bearer <token>`，主要用于测试或非浏览器客户端

因此，浏览器主流程并不是“前端手动保存 Bearer Token”。

## API 概览

以下是当前仓库中已存在的主要接口分组：

- `/api/auth`：注册、登录、刷新、登出、忘记密码、重置密码
- `/api/users`：当前用户资料、改密、我的活动
- `/api/files`：文件上传
- `/api/clubs`：社团、成员、推荐、解散流程
- `/api/recruit`：招新批次、字段、报名、审核、导出
- `/api/activities`：活动、报名、签到、导出
- `/api/notices`：公告
- `/api/notifications`：站内通知
- `/api/resources`：资源定义与资源申请
- `/api/finance`：财务流水与审批
- `/api/admin`：系统管理
- `/api/admin/audit-logs`：审计日志
- `/api/dashboard`：仪表盘统计
- `/api/stats`：系统统计与社团统计
- `/api/club/chat`：AI 问答

更完整的接口说明请查看：

- `docs/校园社团管理与招新系统API接口文档.md:1`

## 文档索引

- 设计说明：`docs/校园社团管理与招新系统设计说明书.md:1`
- 数据库说明：`docs/校园社团管理与招新系统数据库设计说明书.md:1`
- API 文档：`docs/校园社团管理与招新系统API接口文档.md:1`
- 需求规格：`docs/校园社团管理与招新系统需求规格说明书.md:1`
- 项目计划：`docs/校园社团管理与招新系统项目计划.md:1`

## 测试现状

当前仓库中已存在的测试主要包括：

- `community-gateway` 认证相关测试
- `community-user` 用户服务测试
- `community-club` 社团服务测试

整体测试覆盖仍有提升空间，建议后续补充更多控制器级、服务级和集成测试。

## 注意事项

- 当前项目是 **模块化单体**，不是多个独立部署的 Java 微服务。
- 数据库初始化脚本目前不完整，部署前应确认库表准备方式。
- 生产环境请务必清理明文敏感配置并改用环境变量或密钥管理方案。
- 若启用 AI 推荐与问答，需要保证 `agent` 服务和相关依赖正常运行。

## License

本项目当前主要用于学习、课程设计与项目演示，请根据实际需要自行补充许可证与部署规范。
