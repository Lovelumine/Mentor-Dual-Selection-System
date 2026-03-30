# 导师双选系统

这是一个前后端分离的导师双选系统，包含学生申请导师、导师审批、管理员维护师生关系、公告、个人信息、文件上传、密码找回等功能。

## 项目结构

```text
Mentor-Dual-Selection-System/
├── Mentor-Dual-Selection-System/             # 后端，Spring Boot 3 + Java 17
├── Mentor-Dual-Selection-System-Frontend/    # 前端，Vue 3 + Vite + Element Plus
├── deploy/                                   # Docker / CI/CD 部署文件
└── scripts/                                  # 迁移、同步等辅助脚本
```

## 技术栈

- 后端：Spring Boot 3.3、Spring Security、Spring Data JPA、Flyway、MySQL、MinIO
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router
- 部署：Docker Compose、GitHub Actions

## 本地开发环境

建议准备：

- Java 17
- Node.js 20+
- npm
- MySQL 8

后端运行依赖环境变量。至少需要准备下面这些值：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`

如果要联调文件上传、邮件、AI 生成申请理由，还需要额外配置：

- `MINIO_URL`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET_NAME`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM`
- `DEEPSEEK_API_KEY`

生产环境变量模板见 [`deploy/.env.example`](./deploy/.env.example)。

## 启动后端

后端目录：

```bash
cd Mentor-Dual-Selection-System
```

先跑测试：

```bash
bash ./mvnw test
```

再启动开发后端。

如果你已经有本地开发环境文件，可以这样启动：

```bash
set -a
source /path/to/your/dev.env
set +a
bash ./mvnw spring-boot:run
```

如果你想直接手动指定环境变量，可以这样：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/teacherselectionsystem?useSSL=false&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='your_username'
export SPRING_DATASOURCE_PASSWORD='your_password'
export SERVER_ADDRESS='127.0.0.1'
export SERVER_PORT='18080'
bash ./mvnw spring-boot:run
```

启动成功后，开发后端通常访问：

- `http://127.0.0.1:18080`
- `http://127.0.0.1:18080/swagger-ui.html`

如果你不传环境变量，后端会回退到 [`application.yml`](./Mentor-Dual-Selection-System/src/main/resources/application.yml) 里的默认值，例如端口 `50000`。

## 启动前端

前端目录：

```bash
cd Mentor-Dual-Selection-System-Frontend
```

安装依赖并启动：

```bash
npm install
npm run dev
```

默认情况下，前端开发服务运行在：

- `http://127.0.0.1:5174`

并通过 Vite 代理把 `/api`、`/admin`、`/teacher`、`/student`、`/upload` 转发到本地后端 `http://127.0.0.1:18080`。

如果需要自定义前端开发地址，可以设置：

- `VITE_API_PROXY_TARGET`
- `VITE_DEV_HOST`
- `VITE_DEV_PORT`

对应配置见 [`vite.config.ts`](./Mentor-Dual-Selection-System-Frontend/vite.config.ts)。

## 常用命令

后端测试：

```bash
cd Mentor-Dual-Selection-System
bash ./mvnw test
```

后端打包：

```bash
cd Mentor-Dual-Selection-System
bash ./mvnw package
```

前端构建：

```bash
cd Mentor-Dual-Selection-System-Frontend
npm run build
```

前端类型检查：

```bash
cd Mentor-Dual-Selection-System-Frontend
npm run type-check
```

## 部署

部署相关说明见 [`deploy/README.md`](./deploy/README.md)。

## 说明

- 当前系统已经支持学生、导师、管理员三种角色。
- 当前业务里导师容量上限按代码逻辑为 `9` 名学生。
- 根目录 README 主要负责开发和项目总览，部署细节统一放在 `deploy/README.md`。
