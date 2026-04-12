# lottery-system

基于 Spring Boot 4、MyBatis、MySQL、Redis 和 RabbitMQ 的抽奖系统项目，包含用户注册登录、活动管理、奖品管理、抽奖执行、中奖记录查询，以及一套可直接访问的静态管理页面。

## 项目概览

- 后端框架：Spring Boot 4.0.2、Spring MVC、Spring Validation
- 数据访问：MyBatis、MySQL
- 中间件：Redis、RabbitMQ
- 认证能力：JWT
- 扩展能力：阿里云短信验证码、JavaMail 邮件发送、本地图片上传
- 前端形态：`src/main/resources/static` 下的原生静态页面

## 当前已实现功能

- 用户注册
- 密码登录
- 短信验证码发送与验证码登录
- 管理员新增用户
- 活动创建、分页查询、详情查看
- 奖品创建、图片上传、分页查询
- 执行抽奖
- 查询中奖记录

## 目录结构

```text
.
├─src/main/java                  后端业务代码
├─src/main/resources
│  ├─application.properties      主配置
│  ├─application-local.properties.example 本地敏感配置示例
│  └─static                      静态页面与前端资源
├─src/test/java                  测试代码
├─docs                           补充设计与迁移说明
├─lottery_system.sql             数据库初始化脚本
└─pic                            运行期图片目录
```

## 运行环境

- JDK 17
- Maven 3.9+（或直接使用仓库自带 `mvnw` / `mvnw.cmd`）
- MySQL 8.x
- Redis 7.x
- RabbitMQ 3.x

## 本地启动

1. 创建数据库并导入脚本：

   ```sql
   source lottery_system.sql;
   ```

2. 启动本地依赖服务：

   - MySQL：默认连接 `jdbc:mysql://localhost:3306/lottery_system`
   - Redis：默认连接 `localhost:6379`
   - RabbitMQ：默认连接 `127.0.0.1:5672`

3. 配置本地参数：

   - 数据库、Redis、RabbitMQ 默认值写在 `application.properties` 中
   - 复制 `src/main/resources/application-local.properties.example` 为 `application-local.properties`，按需填写本地敏感配置
   - `SMS_ACCESS_KEY_ID`、`SMS_ACCESS_KEY_SECRET`、`MAIL_USERNAME`、`MAIL_PASSWORD` 需要通过系统环境变量或 IDE 启动配置提供
   - 仓库中的 `.env.example` 仅作为变量示例，不会被 Spring Boot 自动加载

4. 启动项目：

   Windows:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   macOS / Linux:

   ```bash
   ./mvnw spring-boot:run
   ```

5. 访问页面：

   - 登录页：`http://localhost:8080/blogin.html`
   - 注册页：`http://localhost:8080/register.html`
   - 管理后台：`http://localhost:8080/admin.html`
   - 活动列表：`http://localhost:8080/activities-list.html`

## 配置说明

- Spring Profile：默认启用 `dev,local`
- 鉴权方式：大部分受保护接口通过请求头 `user_token` 传递 JWT
- 图片存储：本地目录 `./pic/`
- 静态资源映射：`classpath:/static/` 与 `file:./pic/`
- 邮件配置：默认使用 QQ 邮箱 SMTP，端口 `587`
- 短信配置：使用阿里云号码认证服务相关参数

## 主要页面

- `blogin.html`：登录入口
- `register.html`：用户注册
- `admin.html`：后台容器页
- `activities-list.html`：活动列表
- `create-activity.html`：新建活动
- `prizes-list.html`：奖品列表
- `create-prizes.html`：创建奖品
- `user-list.html`：用户列表
- `draw.html`：抽奖页

## 主要接口

### 用户

- `POST /register`
- `POST /admin/user/add`
- `GET /verification-code/send`
- `GET /verification-code/check`
- `POST /password/login`
- `POST /message/login`
- `GET /base-user/find-list`

### 活动

- `POST /activity/create`
- `GET /activity/find-list`
- `GET /activity-detail/find`

### 奖品

- `POST /pic/upload`
- `POST /prize/create`
- `GET /prize/find-list`

### 抽奖

- `POST /draw-prize`
  - 请求体：

    ```json
    {
      "activityId": 1,
      "prizeId": 18
    }
    ```

  - 成功返回：`code = 200`，`data` 中包含奖项信息、开奖时间和 `winnerList`
  - 处理中返回：`code = 202`，`msg = "当前奖项正在处理中"`
  - 候选人不足：`code = 404`
- `POST /winning-records/show`
  - 支持按活动维度查询全部中奖记录
  - 也支持按 `activityId + prizeId` 查询单个奖项最终结果

### 前后端职责

- 前端负责发起 `/draw-prize` 命令和展示抽奖动画
- 后端负责随机选人、状态流转、中奖记录落库和异步邮件通知
- 当前奖项返回 `code = 202` 时，前端应轮询 `POST /winning-records/show`

## 数据表

初始化脚本 `lottery_system.sql` 当前包含以下核心表：

- `user`
- `activity`
- `prize`
- `activity_user`
- `activity_prize`
- `winning_record`

## 开发备注

- 当前代码中保留了本地调试验证码 `888888` 的校验逻辑，仅建议用于本地联调
- `docs/` 目录中保留了短信迁移、中奖记录落库等补充说明，可作为后续维护参考
- `docs/notes/` 下的大量资料属于学习或过程文档，不属于项目运行所必需内容

## 测试

可使用以下命令执行测试：

```powershell
.\mvnw.cmd test
```

如果本地未启动 MySQL、Redis、RabbitMQ 或未补齐外部配置，部分测试可能无法通过。
