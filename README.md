# 校园社团管理与招新系统

基于 `Spring Boot 3`、`Vue 3` 和 `FastAPI` 的校园社团管理与招新系统。仓库当前同时包含：

- Java 后端，多模块模块化单体架构
- Vue 3 前端，覆盖学生端与管理端页面
- 独立 AI 服务，提供 RAG 问答、社团推荐和知识库同步
- Docker Compose 一键运行编排
- 论文写作工程、测试脚本与复现实验输出

这个仓库既可以作为课程设计 / 毕业设计项目运行，也可以直接支撑论文中的功能验证、性能补测和实验留档。

## 功能概览

- 用户认证与账号管理：注册、登录、刷新、登出、忘记密码、资料维护
- 社团全生命周期管理：创建、审批、成员管理、推荐、解散与恢复
- 招新流程：批次管理、表单字段配置、报名、初审 / 终审、导出
- 活动管理：创建、报名、签到、签到导出、奖励结算
- 成员积分档案：社团内积分、积分流水、人工调分、活动奖励结算
- 公告与通知：公告发布、站内通知、RabbitMQ 异步分发、违禁词校验
- 资源与财务：资源申请、财务流水、审批链路
- 统一待办与运营看板：按角色聚合待办、统计接口、实时运营观察
- 审计与监控：审计日志、Prometheus 指标、Grafana 面板
- AI 能力：RAG 问答、协同过滤 / 内容推荐 / 混合推荐、知识库同步

## 技术栈

### 后端

- `Spring Boot 3.2.0`
- `Spring Security`
- `JWT + HttpOnly Cookie + Redis` 混合认证
- `Spring Data JPA`
- `MySQL 8.0`
- `Redis 7`
- `RabbitMQ 3`
- `Micrometer + Prometheus`

### 前端

- `Vue 3`
- `Vite`
- `Pinia`
- `Vue Router`
- `Element Plus`
- `ECharts`
- `Playwright`

### AI 服务

- `FastAPI`
- `LangChain`
- `Chroma`
- `scikit-learn`
- `DeepSeek API`

## 架构说明

后端不是拆分部署的 Java 微服务，而是 Maven 多模块组织的模块化单体。当前核心模块如下：

```text
community-core        公共实体、Repository、通用 VO/DTO、公共服务
community-gateway     Spring Boot 启动入口、认证授权、网关配置、Dashboard、WebSocket
community-user        用户资料、密码修改、权限辅助能力
community-club        社团、成员、积分档案、资源、财务、推荐入口
community-recruit     招新批次、动态表单、报名与审批
community-activity    活动、报名、签到、签到导出、奖励结算
community-notice      公告、通知、RabbitMQ 消费、违禁词校验
community-admin       平台管理、审计日志、违禁词管理
frontend              Vue 3 前端项目
agent                 FastAPI AI 服务，提供问答、推荐、知识同步
monitoring            Prometheus / Grafana 配置
scripts               论文复现、JMeter、导出校验等脚本
paper_outputs         已生成的测试与实验留档
thesis                LaTeX 论文工程
docs                  设计说明、数据库设计、API 文档、测试说明
```

## 快速开始

### 1. 准备环境

推荐使用 Docker Compose 直接运行整套系统。

如需本地开发，当前仓库对应的环境建议如下：

- Java `17+`
- Maven Wrapper（仓库已提供 `mvnw.cmd`）
- Node.js `20+`
- Python `3.10+`
- Docker / Docker Compose

说明：

- `pom.xml` 当前最低版本设定为 Java 17
- 根 `Dockerfile` 当前使用 `Temurin 21`
- `agent/Dockerfile` 当前使用 `Python 3.10`

### 2. 配置环境变量

根目录可通过环境变量覆盖以下配置：

```env
DEEPSEEK_API_KEY=
JWT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
OSS_ENDPOINT=
OSS_BUCKET_NAME=
OSS_URL_PREFIX=
```

其中：

- 不配置 `DEEPSEEK_API_KEY` 时，AI 问答能力可能不可用或表现降级
- 默认 `docker-compose.yml` 带有演示用途的数据库与中间件配置，生产环境不要直接使用

### 3. Docker Compose 启动

在项目根目录执行：

```powershell
docker compose up -d --build
```

启动后可访问：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- RabbitMQ 管理台：`http://localhost:15672`
- Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`

当前 `docker-compose.yml` 主要服务包括：

- `mysql`
- `redis`
- `rabbitmq`
- `app`
- `frontend`
- `rag-service`
- `rag-sync`
- `prometheus`
- `grafana`

### 4. 本地分模块启动

如果你不想全部放进 Docker，也可以本地分别启动。

#### 后端

```powershell
.\mvnw.cmd -pl community-gateway -am spring-boot:run
```

#### 前端

```powershell
cd frontend
npm install
npm run dev
```

#### AI 服务

```powershell
cd agent
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

#### 知识库同步

一次性同步：

```powershell
cd agent
python sync_db_knowledge.py
```

循环同步：

```powershell
cd agent
python sync_db_loop.py
```

## 认证说明

当前浏览器主流程不是“前端手动保存 Bearer Token”，而是：

- 后端签发 `access_token` / `refresh_token`
- 浏览器通过 `HttpOnly Cookie` 持有令牌
- 前端通过 `withCredentials` 自动携带 Cookie
- Redis 保存会话，用于实现主动失效
- 后端也兼容 `Authorization: Bearer <token>`，更适合测试脚本或非浏览器客户端

因此，浏览器场景下更接近“Cookie 持有令牌 + 服务端二次校验”的混合认证模型。

## 测试与验证

### 常用命令

后端测试：

```powershell
.\mvnw.cmd test
```

前端构建：

```powershell
cd frontend
npm run build
```

浏览器级 E2E：

```powershell
cd frontend
$env:E2E_BASE_URL='http://127.0.0.1:5173'
npm run test:e2e:ci
```

### 论文复现相关脚本

- `scripts/eval/`：导出、权限矩阵、读链路稳定性、AI 评估原始记录
- `scripts/jmeter/`：JMeter 性能压测脚本与说明
- `paper_outputs/`：已生成的实验结果、对比数据、E2E 结果和性能工件

最近一次仓库内复核基于 `2026-03-25` 的运行环境，已完成：

- 后端 `.\mvnw.cmd test`
- 前端 `npm run build`
- Playwright `npm run test:e2e:ci`
- 导出校验、权限矩阵检查、关键读链路稳定性检查
- JMeter 业务链路压测与 AI 低强度补测
- 推荐模式对比导出

当前验证结论可以概括为：

- 非 AI 核心业务链路运行稳定
- 导出、权限、关键读接口和多角色前端烟测已能复现
- AI 问答链路在高并发下仍然存在明显时延和超时问题

如果你需要继续追踪这些结果，优先查看 `paper_outputs/` 目录。

## 主要接口分组

当前仓库中的主要接口大致分为：

- `/api/auth`：注册、登录、刷新、登出、密码重置
- `/api/users`：当前用户资料、修改密码、个人相关接口
- `/api/files`：文件上传
- `/api/clubs`：社团、成员、推荐、解散与恢复
- `/api/recruit`：招新批次、表单字段、报名、审批、导出
- `/api/activities`：活动、报名、签到、签到导出、奖励结算
- `/api/notices`：公告
- `/api/notifications`：站内通知
- `/api/resources`：资源定义与资源申请
- `/api/finance`：财务流水与审批
- `/api/admin`：系统管理
- `/api/admin/audit-logs`：审计日志
- `/api/dashboard`：仪表盘与实时运营统计
- `/api/stats`：系统统计与社团统计
- `/api/club/chat`：AI 问答入口

更完整的接口说明见 [docs/校园社团管理与招新系统API接口文档.md](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9FAPI%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)。

## 文档索引

- [设计说明书](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9F%E8%AE%BE%E8%AE%A1%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
- [数据库设计说明书](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9F%E6%95%B0%E6%8D%AE%E5%BA%93%E8%AE%BE%E8%AE%A1%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
- [API 接口文档](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9FAPI%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- [需求规格说明书](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9F%E9%9C%80%E6%B1%82%E8%A7%84%E6%A0%BC%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
- [项目计划](docs/%E6%A0%A1%E5%9B%AD%E7%A4%BE%E5%9B%A2%E7%AE%A1%E7%90%86%E4%B8%8E%E6%8B%9B%E6%96%B0%E7%B3%BB%E7%BB%9F%E9%A1%B9%E7%9B%AE%E8%AE%A1%E5%88%92.md)
- [成员积分功能说明](docs/%E6%88%90%E5%91%98%E7%A7%AF%E5%88%86%E5%8A%9F%E8%83%BD%E8%AF%B4%E6%98%8E.md)
- [项目测试说明](docs/%E9%A1%B9%E7%9B%AE%E6%B5%8B%E8%AF%95%E8%AF%B4%E6%98%8E-2026-03-10.md)
- [自动化测试与告警改造说明](docs/%E8%87%AA%E5%8A%A8%E5%8C%96%E6%B5%8B%E8%AF%95%E4%B8%8E%E5%91%8A%E8%AD%A6%E6%94%B9%E9%80%A0%E8%AF%B4%E6%98%8E-2026-03-10.md)
- [论文工程](thesis/)

## 注意事项

- 当前项目定位是模块化单体，不要按独立 Java 微服务来理解模块边界。
- `docker-compose.yml` 里的默认账号、密码和密钥配置偏向开发 / 演示用途，生产环境必须自行替换。
- AI 问答和推荐依赖 `agent` 服务，问答质量与性能还会受到外部模型调用和知识库同步状态影响。
- 浏览器外客户端调用刷新接口时，Cookie 传递行为和浏览器场景并不完全一致，调试时需要明确客户端上下文。
- 仓库中存在 `thesis/`、`paper_outputs/` 和 `scripts/`，它们不是冗余文件，而是论文复现链路的一部分。

## License

当前仓库主要用于学习、课程设计、论文写作和项目演示。若用于正式部署或二次分发，请根据实际情况补充许可证、敏感配置治理和安全加固方案。
