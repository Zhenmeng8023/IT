# IT项目 README

## 项目介绍

IT项目是一个综合性的Web应用系统，包含用户管理、博客管理、互动交流、圈子管理等功能模块。系统采用模块化架构设计，具有良好的扩展性和可维护性。

## 技术栈

- **后端**：
    - Java 17
    - Spring Boot 4.0.3
    - Spring Security
    - JPA (Java Persistence API)
    - MySQL
    - Swagger (API文档)

- **前端**：
    - Vue.js
    - Element UI

## 项目结构

```
IT/
├── it-dependencies/       # 依赖管理
├── it-framework/          # 框架相关
├── it-main/               # 主模块
├── it-module-blog/        # 博客模块
├── it-module-circle/      # 圈子模块
├── it-module-common/      # 公共模块
├── it-module-interactive/ # 互动模块
├── it-module-login/       # 登录模块
├── it-module-project/     # 项目模块
├── it-module-recommend/   # 推荐模块
├── it-module-system/      # 系统模块
├── it-module-user/        # 用户模块
└── it-ui/                 # 前端模块
```

## 核心功能模块

### 1. 用户模块 (it-module-user)
- 用户信息管理
- 用户个人资料更新
- 用户权限管理

### 2. 博客模块 (it-module-blog)
- 博客创建、编辑、删除
- 博客列表查询
- 博客详情查看

### 3. 互动模块 (it-module-interactive)
- 点赞功能
- 收藏功能
- 评论功能
- 通知功能

### 4. 圈子模块 (it-module-circle)
- 圈子创建与管理
- 圈子成员管理
- 圈子内交流

### 5. 登录模块 (it-module-login)
- 用户注册
- 用户登录
- 验证码验证
- JWT token生成与验证

### 6. 公共模块 (it-module-common)
- 地区信息管理
- 标签管理
- 通用工具类

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd IT
   ```

2. **配置数据库**
    - 创建MySQL数据库，数据库名称为`it_data`
    - 修改`it-main/src/main/resources/application.properties`中的数据库连接信息
    - 需要在本地环境变量中设置SPRING_MAIL_USERNAME和SPRING_MAIL_PASSWORD，用于发送邮件通知, 
    - 或者可以在`it-main/src/main/resources/application.properties`中配置默认的发件人邮箱以及SMTP凭证码
    - 数据库文件在sql文件夹下

3. **构建项目**
   ```bash
   mvn clean install
   ```

4. **启动项目**
    ```bash
    cd it-main
    mvn spring-boot:run
    ```
   前端:
    ```bash
    cd it-ui/ui
    npm install
    npm run dev
    ```

5. **访问系统**
    - 前端地址：http://localhost:18080
    - Swagger API文档：http://localhost:18080/swagger-ui.html

## API文档

系统集成了Swagger API文档，您可以通过以下地址访问：
- http://localhost:18080/swagger-ui.html

API文档包含了所有接口的详细信息，包括请求参数、响应格式、示例等，方便前端开发人员了解和使用后端接口。

## 开发指南

### 后端开发

1. **模块划分**：按照功能模块划分，每个模块负责特定的业务功能
2. **代码规范**：遵循Java代码规范，使用Lombok简化代码
3. **接口设计**：RESTful风格接口设计
4. **异常处理**：统一的异常处理机制
5. **日志记录**：使用SLF4J进行日志记录

### 前端开发

1. **组件化开发**：使用Vue组件化开发
2. **状态管理**：使用Vuex进行状态管理
3. **路由管理**：使用Vue Router进行路由管理
4. **API调用**：使用Axios进行API调用
5. **UI组件**：使用Element UI组件库


        