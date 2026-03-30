# 部署说明

这套部署文件现在已经放进 Git 仓库，可以和业务代码一起跟踪。

## 目录说明

- `deploy/docker-compose.yml`: 生产部署入口
- `deploy/.env.example`: 环境变量模板
- `Mentor-Dual-Selection-System/Dockerfile`: 后端镜像构建文件
- `Mentor-Dual-Selection-System-Frontend/`: 前端源码，GitHub Actions 会自动构建并发布静态文件

## 本地或服务器手动部署

1. 克隆仓库到服务器
2. 复制 `deploy/.env.example` 为 `deploy/.env`
3. 按实际环境填写数据库、邮件、MinIO 等变量
4. 执行下面的命令

```bash
cd deploy
./deploy.sh
```

只想校验配置、不真正部署时：

```bash
cd deploy
./deploy.sh --check
```

## 从当前 `/usr/soft/Mentor-Dual-Selection-System` 迁移

1. 保留当前线上容器不动，先把仓库克隆到新的目录
2. 在新目录里配置 `deploy/.env`
3. 执行 `docker compose up -d --build`
4. 确认 `http://服务器IP:50000/hello` 和业务接口都正常
5. 再停掉旧目录下的容器，避免直接覆盖当前运行环境

## GitHub Actions 所需 Secrets

`deploy-backend.yml` 依赖下面这些仓库 Secrets:

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_APP_DIR`
- `DEPLOY_FRONTEND_DIR` 可选，默认 `/www/wwwroot/select.lumoxuan.cn`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM`
- `JWT_SECRET`
- `APP_DEFAULT_INITIAL_PASSWORD`
- `MINIO_URL`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET_NAME`

可以先把这些值写进 `deploy/.env`，再用下面的脚本推到 GitHub Secrets:

```bash
cd deploy
./push-github-secrets.sh owner/repo
```

如果你当前目录本身就是这个仓库，也可以省略 `owner/repo`。
这个脚本依赖 GitHub CLI `gh`，需要先安装并登录。

## 自动部署行为

- 当 `Mentor-Dual-Selection-System/**`、`Mentor-Dual-Selection-System-Frontend/**`、`deploy/**` 或部署 workflow 本身发生变更并推送到 `main` 时，会自动触发部署。
- 后端会继续通过 Docker Compose 更新。
- 前端会在 GitHub Actions 上完成构建，然后把 `dist/` 发布到 `DEPLOY_FRONTEND_DIR`。
- 前端目录在发布前会清空旧文件，但会保留 `.well-known`，避免影响证书校验目录。
- 部署完成后会在服务器本机校验后端 `http://127.0.0.1:50000/hello` 和前端 `index.html` 是否存在，避免假成功。

## 安全建议

- 不要再把真实密码写回 `application.yml` 或 `.env.example`
- 当前已经暴露过的数据库、邮箱、MinIO 凭据建议立刻轮换
- `deploy/.env` 不要提交到 Git
