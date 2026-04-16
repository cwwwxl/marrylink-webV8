# MarryLink Docker 部署指南

## 📋 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署命令](#部署命令)
- [服务管理](#服务管理)
- [移动端 H5 部署](#移动端-h5-部署)
- [故障排查](#故障排查)
- [生产环境建议](#生产环境建议)

---

## 环境要求

### 必需软件

| 软件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发/测试 | 2核 | 4GB | 20GB |
| 生产环境 | 4核+ | 8GB+ | 50GB+ |

---

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd hunli
```

### 2. 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑配置（根据实际情况修改）
vim .env
```

### 3. 本地构建项目

由于 Dockerfile 采用直接部署模式（不在容器内构建），需要先在本地构建各项目：

```bash
# 后端：打包 jar
cd marrylink-admin/backend
mvn clean package -DskipTests
cd ../..

# Web 管理端：构建 dist
cd marrylink-web
pnpm install
pnpm run build-only
cd ..

# App H5 端：构建 dist/build/h5（需要 Node.js 16.x）
cd marrylink-app
npm install
npm run build:h5
cd ..
```

### 4. 启动服务

```bash
# 构建并启动所有服务
docker compose up -d --build

# 查看服务状态
docker compose ps
```

### 4. 访问服务

- **管理后台**: http://localhost:80
- **移动端 H5**: http://localhost:8081
- **后端 API**: http://localhost:8080/api/v1

---

## 配置说明

### 环境变量 (.env)

```bash
# ==================== 版本 ====================
VERSION=latest                    # 镜像版本标签

# ==================== MySQL 配置 ====================
MYSQL_ROOT_PASSWORD=marrylink123  # MySQL root 密码
MYSQL_DATABASE=marrylink          # 数据库名称
MYSQL_USER=marrylink              # 数据库用户名
MYSQL_PASSWORD=marrylink123       # 数据库密码
MYSQL_PORT=3306                   # MySQL 外部端口

# ==================== Redis 配置 ====================
REDIS_PASSWORD=marrylink123       # Redis 密码
REDIS_PORT=6379                   # Redis 外部端口

# ==================== 后端配置 ====================
BACKEND_PORT=8080                 # 后端服务端口
JWT_SECRET=your-secret-key        # JWT 签名密钥（生产环境必须修改）
JWT_EXPIRATION=86400000           # JWT 过期时间（毫秒）
JAVA_OPTS=-Xms512m -Xmx1024m      # JVM 参数

# ==================== 管理后台前端配置 ====================
FRONTEND_PORT=80                  # 管理后台端口

# ==================== 移动端 H5 配置 ====================
MOBILE_PORT=8081                  # 移动端 H5 端口
```

### 服务架构

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           Docker Network                                  │
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                       │
│  │  Frontend   │  │ Mobile App  │  │   Backend   │                       │
│  │  (Admin)    │  │    (H5)     │  │  (Spring)   │                       │
│  │   :80       │  │   :8081     │  │   :8080     │                       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                       │
│         │                │                │                              │
│         └────────────────┴────────┬───────┘                              │
│                                   │                                      │
└───────────────────────────────────┼──────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │       外部服务 (宿主机)         │
                    │  ┌─────────┐  ┌─────────────┐ │
                    │  │  MySQL  │  │    Redis    │ │
                    │  │  :3306  │  │    :6379    │ │
                    │  └─────────┘  └─────────────┘ │
                    └───────────────────────────────┘
```

> **注意**: MySQL 和 Redis 使用外部服务（宿主机或独立服务器），不在 Docker 中部署。

---

## 部署命令

### 基础命令

> **注意**: 新版 Docker (20.10+) 使用 `docker compose`（不带连字符），旧版使用 `docker-compose`。

```bash
# 构建镜像
docker compose build

# 启动所有服务（后台运行）
docker compose up -d

# 构建并启动
docker compose up -d --build

# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 会删除所有数据）
docker compose down -v
```

### 单独管理服务

```bash
# 只启动后端
docker compose up -d backend

# 重启前端
docker compose restart frontend

# 查看后端日志
docker compose logs -f backend

# 进入 MySQL 容器
docker compose exec mysql mysql -u root -p
```

### 镜像管理

```bash
# 查看构建的镜像
docker images | grep marrylink

# 导出镜像
docker save marrylink-backend:latest | gzip > marrylink-backend.tar.gz
docker save marrylink-frontend:latest | gzip > marrylink-frontend.tar.gz

# 导入镜像
docker load < marrylink-backend.tar.gz
docker load < marrylink-frontend.tar.gz
```

---

## 服务管理

### 查看服务状态

```bash
# 查看所有容器状态
docker compose ps

# 查看资源使用情况
docker stats

# 查看服务健康状态
docker inspect --format='{{.State.Health.Status}}' marrylink-backend
```

### 日志管理

```bash
# 查看所有服务日志
docker compose logs

# 实时查看后端日志
docker compose logs -f backend

# 查看最近 100 行日志
docker compose logs --tail=100 backend

# 查看指定时间范围的日志
docker compose logs --since="2024-01-01" --until="2024-01-02" backend
```

### 扩展服务

```bash
# 扩展后端服务实例（需要负载均衡支持）
docker compose up -d --scale backend=3
```

---

## 移动端 H5 部署

### 概述

marrylink-app 是基于 uni-app 开发的移动端应用，支持多种部署方式：

| 部署方式 | 说明 | 适用场景 |
|----------|------|----------|
| **H5 (Docker)** | 构建为 H5 网页，通过 Docker 部署 | 微信公众号、移动端浏览器 |
| **微信小程序** | 构建为微信小程序包 | 微信生态 |
| **App** | 构建为原生 App | iOS/Android 应用商店 |

### H5 部署（Docker）

H5 版本已集成在 docker-compose.yml 中，会自动部署。

**前置条件**：需要先在本地构建 H5 产物：
```bash
cd marrylink-app
npm install
npm run build:h5
cd ..
```

```bash
# 单独构建移动端 H5
docker compose build mobile-app

# 单独启动移动端 H5
docker compose up -d mobile-app

# 查看移动端日志
docker compose logs -f mobile-app
```

访问地址：http://localhost:8081

### 微信小程序部署

微信小程序需要单独构建，不通过 Docker 部署：

```bash
cd marrylink-app

# 安装依赖
npm install

# 构建微信小程序
npm run build:mp-weixin
```

构建产物位于 `dist/build/mp-weixin` 目录，使用微信开发者工具导入并上传。

**注意事项：**
1. 需要在 `manifest.json` 中配置微信小程序 AppID
2. 需要在微信公众平台配置服务器域名（后端 API 地址）
3. 小程序中的 API 地址需要使用 HTTPS

### App 部署

原生 App 需要使用 HBuilderX 或云打包：

```bash
cd marrylink-app

# 构建 App 资源
npm run build:app-plus
```

然后使用 HBuilderX 进行云打包或本地打包。

### API 地址配置

不同环境的 API 地址配置在 `marrylink-app/utils/request.js` 中：

| 环境 | API 地址 | 说明 |
|------|----------|------|
| H5 (Docker) | `/app-api` | 通过 nginx 代理到后端 |
| 小程序/App | `http://your-domain.com/api/v1` | 需要修改为实际域名 |

**生产环境配置示例：**

```javascript
// marrylink-app/utils/request.js
const getBaseUrl = () => {
  // #ifdef H5
  return '/app-api'  // H5 使用相对路径
  // #endif
  
  // #ifndef H5
  return 'https://api.your-domain.com/api/v1'  // 小程序/App 使用完整 URL
  // #endif
}
```

---

---

## 故障排查

### 常见问题

#### 1. 后端无法连接 MySQL

```bash
# 检查 MySQL 是否启动
docker compose ps mysql

# 查看 MySQL 日志
docker compose logs mysql

# 测试连接
docker compose exec mysql mysql -u root -p -e "SELECT 1"
```

#### 2. 前端无法访问后端 API

```bash
# 检查后端健康状态
curl http://localhost:8080/api/v1/actuator/health

# 检查 nginx 配置
docker compose exec frontend cat /etc/nginx/conf.d/marrylink.conf

# 查看 nginx 错误日志
docker compose exec frontend cat /var/log/nginx/error.log
```

#### 3. 容器启动失败

```bash
# 查看详细错误信息
docker compose logs --tail=50 <service-name>

# 检查容器退出原因
docker inspect <container-id> --format='{{.State.ExitCode}}'
```

#### 4. 磁盘空间不足

```bash
# 清理未使用的镜像和容器
docker system prune -a

# 清理构建缓存
docker builder prune
```

### 调试模式

```bash
# 以交互模式启动容器
docker compose run --rm backend sh

# 检查网络连通性
docker compose exec backend ping mysql
docker compose exec backend ping redis
```

---

## 生产环境建议

### 安全配置

1. **修改默认密码**
   ```bash
   # 生成强密码
   openssl rand -base64 32
   ```

2. **配置 HTTPS**
   - 使用 Let's Encrypt 或商业证书
   - 在 nginx 配置中启用 SSL

3. **限制端口暴露**
   - 只暴露必要的端口（80/443）
   - MySQL 和 Redis 不对外暴露

### 性能优化

1. **JVM 调优**
   ```bash
   JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
   ```

2. **MySQL 优化**
   - 增加 `innodb_buffer_pool_size`
   - 配置慢查询日志

3. **Redis 优化**
   - 配置 `maxmemory` 和淘汰策略
   - 启用 AOF 持久化

### 监控告警

推荐使用以下工具：
- **Prometheus + Grafana**: 指标监控
- **ELK Stack**: 日志分析
- **Portainer**: Docker 可视化管理

### 高可用部署

```yaml
# docker-compose.prod.yml 示例
services:
  backend:
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

---

## 常用脚本

### 一键部署脚本

```bash
#!/bin/bash
# deploy.sh

set -e

echo "🚀 开始部署 MarryLink..."

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    exit 1
fi

# 检查环境变量文件
if [ ! -f .env ]; then
    echo "📝 创建环境变量文件..."
    cp .env.example .env
fi

# 本地构建项目
echo "🔨 构建后端 jar..."
cd marrylink-admin/backend && mvn clean package -DskipTests && cd ../..

echo "🔨 构建前端 dist..."
cd marrylink-web && pnpm install && pnpm run build-only && cd ..

echo "🔨 构建移动端 H5..."
cd marrylink-app && npm install && npm run build:h5 && cd ..

# 构建 Docker 镜像并启动
echo "🔨 构建 Docker 镜像..."
docker compose build

echo "🚀 启动服务..."
docker compose up -d

# 等待服务就绪
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "✅ 服务状态:"
docker compose ps

echo "🎉 部署完成！"
echo "📌 前端地址: http://localhost:${FRONTEND_PORT:-80}"
echo "📌 移动端地址: http://localhost:${MOBILE_PORT:-8081}"
echo "📌 后端地址: http://localhost:${BACKEND_PORT:-8080}/api/v1"
```

### 健康检查脚本

```bash
#!/bin/bash
# health-check.sh

echo "🔍 检查服务健康状态..."

services=("mysql" "redis" "backend" "frontend")

for service in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Health.Status}}' marrylink-$service 2>/dev/null || echo "unknown")
    if [ "$status" == "healthy" ]; then
        echo "✅ $service: $status"
    else
        echo "❌ $service: $status"
    fi
done
```

---

## 联系支持

如有问题，请提交 Issue 或联系开发团队。

---

*最后更新: 2024年*