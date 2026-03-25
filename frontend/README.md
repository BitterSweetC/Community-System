# frontend

`frontend/` 是校园社团管理与招新系统的 Vue 3 前端工程，覆盖：

- 游客入口页、登录、注册、找回密码
- 学生端首页、社团检索、活动列表、公告、通知、个人中心
- 学生侧成员积分档案页面
- 管理端仪表盘、统一待办中心、实时运营看板
- 社团管理员侧成员、招新、活动、资源、财务、公告等页面
- 平台管理员侧审计日志、违禁词管理、资源定义等页面

当前工程基于 `Vue 3 + Vite + Pinia + Vue Router + Element Plus`。

## 环境要求

- Node.js `20+`
- npm `10+`（建议跟随 Node 20 自带版本）

仓库中的 `package.json` 当前声明：

```json
"engines": {
  "node": "^20.19.0 || >=22.12.0"
}
```

## 常用命令

安装依赖：

```powershell
npm install
```

本地开发：

```powershell
npm run dev
```

生产构建：

```powershell
npm run build
```

本地预览构建结果：

```powershell
npm run preview
```

运行 Playwright：

```powershell
npm run test:e2e
```

CI 风格输出：

```powershell
npm run test:e2e:ci
```

有界面模式：

```powershell
npm run test:e2e:headed
```

## 本地开发说明

### Vite 代理

本地 `npm run dev` 时，前端通过 Vite 代理把 `/api` 请求转发到：

```text
http://localhost:8080
```

也就是说：

- 前端开发端口默认是 `5173`
- 本地后端默认应运行在 `8080`
- 浏览器端请求统一走 `/api`

当前 Axios 实例还开启了：

- `withCredentials: true`

因此浏览器会自动携带后端签发的 `HttpOnly Cookie`。

### 认证模型

这个前端不是“把 JWT 存在 localStorage 再手动拼请求头”的方案。当前实现更接近：

- 后端把令牌写入 `HttpOnly Cookie`
- 前端通过 `withCredentials` 自动携带 Cookie
- 前端本地状态主要保存用户信息和页面状态
- 当接口返回 `401` 或 `403` 时，拦截器会清理认证状态并跳回登录页

所以前端调试时，优先排查 Cookie、登录态和后端会话，而不是去找本地 Bearer Token。

## 目录结构

当前前端主要目录如下：

```text
frontend/
├─ public/
├─ src/
│  ├─ api/           Axios 封装与业务 API
│  ├─ components/    复用组件，如 ChatWidget、InterestSelector
│  ├─ router/        路由与导航守卫
│  ├─ stores/        Pinia 状态管理
│  └─ views/
│     ├─ student/    学生端页面
│     ├─ admin/      管理端与社团管理员页面
│     └─ tools/      辅助页面，如截图登录页
├─ tests/e2e/        Playwright 用例
├─ package.json
├─ vite.config.js
└─ playwright.config.js
```

## 页面与路由概览

当前路由按角色大致分为三组：

- 公开页面：`/`、`/login`、`/register`、`/forgot-password`
- 学生端：`/home/*`、`/user/*`
- 管理端：`/admin/*`
- 社团管理员端：`/clubadmin/*`

其中最近补充的页面包括：

- `/user/archive`：成员积分档案
- `/admin/todos`：平台待办中心
- `/clubadmin/todos`：社团管理员待办中心
- `/admin/realtime`：实时运营看板
- `/admin/prohibited-words`：平台违禁词管理

## E2E 测试说明

当前 Playwright 配置位于：

- `playwright.config.js`

默认行为：

- 测试目录：`./tests/e2e`
- 未设置 `E2E_BASE_URL` 时，会自动拉起一个本地 Vite 服务
- 默认测试地址：`http://127.0.0.1:4173`
- 当前仓库中已有 `login-flow.spec.js`

如果你已经让整套系统跑在 Docker 里，推荐这样执行：

```powershell
$env:E2E_BASE_URL='http://127.0.0.1:5173'
npm run test:e2e:ci
```

这样 Playwright 会直接复用已运行的前端，而不会再额外启动一个开发服务器。

## 与后端联调时的注意事项

- 本地开发依赖后端 `http://localhost:8080`
- 若后端没启动，页面会正常打开，但 API 会全部失败
- 若你在 Docker 中运行前端而本地运行后端，要额外确认跨域和代理路径是否一致
- 下载类接口走 `blob` 响应，出问题时优先看后端返回头和业务码
- 登录刷新链路在浏览器场景下依赖 Cookie；非浏览器脚本行为可能和页面表现不完全一致

## 构建与部署

开发阶段使用 Vite 开发服务器。

Docker 部署时，前端会被构建为静态资源镜像，并在根 `docker-compose.yml` 中以：

- 容器 `community-frontend`
- 宿主机端口 `5173`

对外提供服务。

## 建议阅读顺序

如果你是第一次接手这个前端，建议按下面顺序看：

1. `package.json`
2. `vite.config.js`
3. `src/router/index.js`
4. `src/api/axios.js`
5. `src/stores/auth.js`
6. `src/views/student/` 与 `src/views/admin/`

这样可以最快建立起页面、认证和联调方式的整体认知。
