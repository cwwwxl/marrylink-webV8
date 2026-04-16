#!/bin/bash
# MarryLink 一键部署脚本
# 用法: ./scripts/deploy.sh [dev|prod]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 是否安装
check_docker() {
    log_info "检查 Docker 环境..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# 检查环境变量文件
check_env() {
    log_info "检查环境变量配置..."
    
    if [ ! -f .env ]; then
        log_warning ".env 文件不存在，从示例文件创建..."
        if [ -f .env.example ]; then
            cp .env.example .env
            log_success "已创建 .env 文件，请根据需要修改配置"
        else
            log_error ".env.example 文件不存在"
            exit 1
        fi
    fi
    
    log_success "环境变量配置检查通过"
}

# 构建镜像
build_images() {
    log_info "开始构建 Docker 镜像..."
    
    docker-compose build --no-cache
    
    log_success "镜像构建完成"
}

# 启动服务
start_services() {
    log_info "启动所有服务..."
    
    docker-compose up -d
    
    log_success "服务启动命令已执行"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local max_attempts=60
    local attempt=0
    
    # 等待 MySQL
    log_info "等待 MySQL 就绪..."
    while [ $attempt -lt $max_attempts ]; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            log_success "MySQL 已就绪"
            break
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    
    if [ $attempt -eq $max_attempts ]; then
        log_warning "MySQL 启动超时，请手动检查"
    fi
    
    # 等待后端服务
    attempt=0
    log_info "等待后端服务就绪..."
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1; then
            log_success "后端服务已就绪"
            break
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    
    if [ $attempt -eq $max_attempts ]; then
        log_warning "后端服务启动超时，请手动检查"
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    echo ""
    docker-compose ps
    echo ""
}

# 显示访问信息
show_access_info() {
    # 读取端口配置
    source .env 2>/dev/null || true
    
    local frontend_port=${FRONTEND_PORT:-80}
    local backend_port=${BACKEND_PORT:-8080}
    
    echo ""
    echo "=========================================="
    echo -e "${GREEN}🎉 MarryLink 部署完成！${NC}"
    echo "=========================================="
    echo ""
    echo -e "📌 前端管理后台: ${BLUE}http://localhost:${frontend_port}${NC}"
    echo -e "📌 后端 API 地址: ${BLUE}http://localhost:${backend_port}/api/v1${NC}"
    echo ""
    echo "=========================================="
    echo ""
}

# 停止服务
stop_services() {
    log_info "停止所有服务..."
    docker-compose down
    log_success "服务已停止"
}

# 清理资源
cleanup() {
    log_info "清理 Docker 资源..."
    docker-compose down -v --rmi local
    docker system prune -f
    log_success "清理完成"
}

# 查看日志
show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$service"
    fi
}

# 主函数
main() {
    local command=${1:-deploy}
    
    echo ""
    echo "=========================================="
    echo "       MarryLink Docker 部署工具"
    echo "=========================================="
    echo ""
    
    case $command in
        deploy|start)
            check_docker
            check_env
            build_images
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        build)
            check_docker
            build_images
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            echo "用法: $0 [命令]"
            echo ""
            echo "命令:"
            echo "  deploy    构建并启动所有服务（默认）"
            echo "  start     启动服务"
            echo "  stop      停止所有服务"
            echo "  restart   重启所有服务"
            echo "  status    查看服务状态"
            echo "  logs      查看日志 (可选: logs [service])"
            echo "  build     仅构建镜像"
            echo "  cleanup   清理所有资源（包括数据）"
            echo "  help      显示帮助信息"
            echo ""
            ;;
        *)
            log_error "未知命令: $command"
            echo "使用 '$0 help' 查看帮助信息"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"