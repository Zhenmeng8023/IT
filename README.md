<div align="center">

# IT

> 一个基于 **Spring Boot + Vue** 的模块化综合平台项目  
> 采用 **后端多模块 + 前端独立工程** 的组织方式，围绕内容、互动、协作、智能化能力持续扩展。

<p>
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen" alt="Spring Boot 3.4.0" />
  <img src="https://img.shields.io/badge/Vue-Frontend-42b883" alt="Vue" />
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8.0" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-red" alt="License" />
</p>

</div>

---

## 项目简介

IT 是一个面向综合业务场景的模块化平台项目，整体采用 **前后端分离** 架构设计：

- 后端基于 **Java 17 + Spring Boot**
- 前端基于 **Vue**
- 数据层使用 **MySQL**
- 通过多模块拆分业务边界，便于后续持续演进与扩展

从当前仓库结构来看，项目已经按业务方向拆分出多个独立模块，包括：

- 登录认证
- 博客内容
- 圈子互动
- 通用能力
- AI 模块
- 项目协作
- 支付能力
- 推荐能力
- 系统管理

这意味着项目具备继续向 **内容平台 + 协作平台 + 智能能力平台** 演进的基础。

---

## 功能亮点

- **模块化架构**：后端按职责拆分，便于维护与扩展
- **前后端分离**：前端独立开发、独立联调、独立部署
- **业务边界清晰**：各模块职责相对明确，方便后续重构与治理
- **扩展方向明确**：AI、协作、推荐、支付等模块已具备结构基础
- **适合持续迭代**：适合作为长期演进型项目逐步完善

---

## 技术栈

### 后端

- Java 17
- Spring Boot 3.4.0
- Spring Security
- Spring Data JPA
- MySQL
- Swagger / OpenAPI
- Maven

### 前端

- Vue.js
- JavaScript
- Element UI
- CSS
- Axios
- Vue Router
- Vuex

---

## 项目架构

```text
前端（it-ui/ui）
   ↓
Controller 接口层
   ↓
Service 业务层
   ↓
Repository / JPA 持久层
   ↓
MySQL

公共支撑：
- it-framework
- it-module-common

业务模块：
- it-module-login
- it-module-blog
- it-module-circle
- it-module-interactive
- it-module-project
- it-module-ai
- it-module-payment
- it-module-recommend
- it-module-system

---

## 仓库结构

```text
IT/
├── it-dependencies/        # Maven 依赖统一管理
├── it-framework/           # 基础框架层、公共底层支撑
├── it-main/                # 后端启动入口
├── it-module-ai/           # AI 模块
├── it-module-blog/         # 博客模块
├── it-module-circle/       # 圈子模块
├── it-module-common/       # 公共能力模块
├── it-module-interactive/  # 互动模块
├── it-module-login/        # 登录与认证模块
├── it-module-payment/      # 支付模块
├── it-module-project/      # 项目协作模块
├── it-module-recommend/    # 推荐模块
├── it-module-system/       # 系统管理模块
├── it-ui/
│   └── ui/                 # 前端工程
├── sql/                    # 数据库脚本
├── pom.xml                 # Maven 聚合工程
└── README.md
```

---

## 模块说明

| 模块                      | 说明                            |
| ----------------------- | ----------------------------- |
| `it-main`               | 应用启动模块，负责聚合各业务模块并启动后端服务       |
| `it-framework`          | 提供项目底层框架支撑、配置能力与基础设施封装        |
| `it-module-common`      | 公共工具、公共组件、通用业务能力沉淀            |
| `it-module-login`       | 登录、注册、认证、授权等用户访问控制相关能力        |
| `it-module-blog`        | 博客内容相关业务模块                    |
| `it-module-circle`      | 圈子、社区、关系与圈内业务承载模块             |
| `it-module-interactive` | 点赞、评论、收藏、通知等互动能力模块            |
| `it-module-project`     | 项目协作相关模块，适合承载任务、成员、文档、文件等业务链路 |
| `it-module-ai`          | AI 能力扩展模块，适合承载知识库、问答、助手等功能    |
| `it-module-recommend`   | 推荐系统相关模块                      |
| `it-module-payment`     | 支付、订单、会员等相关能力模块               |
| `it-module-system`      | 系统管理、后台治理、配置管理等系统级能力          |
| `it-ui/ui`              | 前端页面、组件、路由、接口调用所在工程           |
| `sql`                   | 数据库初始化、升级、维护相关 SQL 文件         |

---

## 快速开始

### 环境要求

请先准备以下环境：

* JDK 17+
* Maven 3.6+
* MySQL 8.0+
* Node.js
* npm

---

### 1. 克隆项目

```bash
git clone https://github.com/Zhenmeng8023/IT.git
cd IT
```

---

### 2. 配置数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE it9_data DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后修改后端配置文件中的数据库连接信息：

```text
it-main/src/main/resources/application.properties
```

如果项目启用了邮件通知能力，需要配置以下环境变量：

```bash
SPRING_MAIL_USERNAME=your_email@example.com
SPRING_MAIL_PASSWORD=your_smtp_password
```

也可以直接在 `application.properties` 中配置默认发件邮箱和 SMTP 凭证。

数据库脚本位于：

```text
sql/
```

---

### 3. 构建后端

```bash
mvn clean install
```

---

### 4. 启动后端

```bash
cd it-main
mvn spring-boot:run
```

---

### 5. 启动前端

```bash
cd it-ui/ui
npm install
npm run dev
```

---

## 访问地址

项目启动成功后，可按本地配置访问：

* 前端地址：`http://localhost:18080`
* Swagger API 文档：`http://localhost:18080/swagger-ui.html`

---

## 配置说明

### 后端配置

主要配置文件：

```text
it-main/src/main/resources/application.properties
```

通常需要关注的配置项包括：

* 数据库连接地址
* 数据库用户名与密码
* 服务端口
* Swagger 配置
* 邮件发送配置
* AI 相关配置（如后续接入模型服务）

### 数据库脚本

数据库初始化和升级脚本统一放在：

```text
sql/
```

建议后续所有数据库变更都通过脚本维护，便于版本追踪。

---

## API 文档

系统已集成 Swagger API 文档，可通过以下地址访问：

```text
http://localhost:18080/swagger-ui.html
```

文档中包含：

* 接口地址
* 请求参数
* 响应结构
* 调试入口
* 示例信息

便于前后端联调与接口排查。

---

## 开发指南

### 后端开发规范

1. 按模块边界组织代码，避免职责混乱
2. 保持 `Controller / Service / Repository` 分层清晰
3. 公共逻辑优先沉淀到 `it-framework` 或 `it-module-common`
4. 接口尽量遵循 RESTful 风格
5. 统一异常处理与日志记录
6. SQL 变更统一维护到 `sql/` 目录，便于版本管理

### 前端开发规范

1. 使用 Vue 组件化开发
2. 使用 Vue Router 管理页面路由
3. 使用 Vuex 管理状态
4. 使用 Axios 统一进行 API 调用
5. 使用 Element UI 统一页面组件风格
6. 建议抽离公共组件、公共样式与请求封装，降低维护成本

---

## 推荐开发流程

```text
需求设计
  ↓
模块归属分析
  ↓
数据库设计 / SQL 变更
  ↓
后端接口开发
  ↓
前端页面联调
  ↓
功能测试
  ↓
文档补充
```

---

## 后续规划

### 协作链路完善

围绕项目模块继续补齐：

* 项目
* 成员
* 文档
* 文件
* 任务
* 分支
* 提交
* Merge Request
* Release

### AI 模块优化

围绕 AI 模块继续完善：

* 知识库
* 文档切分
* 向量召回
* 重排优化
* 问答质量
* 场景化助手能力

### 推荐系统演进

进一步建设推荐模块的数据链路、召回策略与排序能力。

### 支付与会员体系

完善订单、支付、权益、会员能力闭环。

### 系统治理

逐步补齐：

* 权限边界
* 日志治理
* 审计能力
* 配置管理
* 异常处理规范
* 接口文档规范

### 前端体验统一

持续优化：

* 页面层级
* 暗黑主题一致性
* 表单与表格体验
* 全站布局统一性
* 交互细节与视觉一致性

---

## 适用场景

本项目适合作为：

* 多模块 Spring Boot 项目练习样板
* Vue + Java 前后端分离项目实践
* 中大型个人项目长期迭代基础盘
* 内容平台 / 协作平台 / 智能平台融合型项目雏形

---

## 贡献指南

欢迎参与项目建设，建议遵循以下原则：

1. 保持模块边界清晰
2. 尽量避免跨模块直接耦合
3. 公共逻辑优先抽取复用
4. 提交前确保代码结构清晰、命名统一
5. 涉及 SQL 变更时请同步补充脚本
6. 涉及 API 变更时建议同步补充文档说明

---

## 常见问题

<details>
<summary><strong>1. 后端启动失败怎么办？</strong></summary>

请优先检查：

* JDK 版本是否为 17+
* MySQL 是否已启动
* `application.properties` 中数据库配置是否正确
* 数据库 `it9_data` 是否已创建
* 相关 SQL 是否已执行

</details>

<details>
<summary><strong>2. 前端启动失败怎么办？</strong></summary>

请优先检查：

* Node.js 与 npm 是否正确安装
* 是否已执行 `npm install`
* 前端依赖是否下载完整
* 本地端口是否被占用

</details>

<details>
<summary><strong>3. Swagger 无法访问怎么办？</strong></summary>

请优先检查：

* 后端是否已正常启动
* 访问端口是否正确
* Swagger 配置是否被环境禁用

</details>

---

## License

本项目采用 **Apache-2.0 License**。

---

## Star

如果这个项目对你有帮助，欢迎点一个 ⭐ 支持一下。


