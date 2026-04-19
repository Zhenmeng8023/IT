# Playwright E2E Scaffold

本目录用于承接真实端到端测试基础设施，当前已提供：

- `setup/auth.setup.js`: 生成普通用户与管理员 `storageState`
- `smoke/smoke.spec.js`: 首页、圈子、blog、管理端圈子页（有权限时）基础 smoke
- `auth/`、`circle-public/`、`circle-manage/`、`blog-flow/`: 后续业务用例预留目录

## 环境变量

- `E2E_BASE_URL`：前端地址，默认 `http://localhost:3000`
- `E2E_API_BASE_URL`：后端地址，默认 `http://localhost:18080`
- `E2E_USER_USERNAME` / `E2E_USER_PASSWORD`：普通用户登录凭据
- `E2E_ADMIN_USERNAME` / `E2E_ADMIN_PASSWORD`：管理员登录凭据
- `E2E_USE_DEV_SERVER=0`：如需复用外部已启动前端，可关闭 Playwright 内置 webServer

## 运行

```bash
npm run e2e:setup-auth
npm run e2e:smoke
```

如果未提供角色凭据，`setup-auth` 会写入空 `storageState`，并在需要角色态的场景中自动跳过。
