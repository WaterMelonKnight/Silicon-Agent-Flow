#!/bin/bash

###############################################################################
# Silicon Agent Flow - 一键部署脚本
# 支持部署项目的所有模块：MySQL、Spring Boot App、Python Worker
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目信息
PROJECT_NAME="silicon-agent-flow"
DOCKER_COMPOSE_FILE="docker-compose.yml"

# 日志函数
log_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
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

# 打印横幅
print_banner() {
    echo -e "${CYAN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║                                                           ║"
    echo "║        Silicon Agent Flow - 部署脚本 v1.0                ║"
    echo "║        AI-Driven EDA Job Scheduler                        ║"
    echo "║                                                           ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."

    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    log_success "Docker 已安装: $(docker --version)"

    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    log_success "Docker Compose 已安装: $(docker-compose --version)"

    # 检查 Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven 未安装，请先安装 Maven"
        exit 1
    fi
    log_success "Maven 已安装: $(mvn --version | head -n 1)"

    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "Java 未安装，请先安装 Java"
        exit 1
    fi
    log_success "Java 已安装: $(java -version 2>&1 | head -n 1)"
}

# 检查环境变量
check_env() {
    log_info "检查环境变量配置..."

    if [ ! -f ".env" ]; then
        log_warning ".env 文件不存在，将使用默认配置"
        log_info "如需配置 OpenAI API，请创建 .env 文件"
    else
        log_success ".env 文件已存在"
        source .env
    fi
}

# 构建 Spring Boot 应用
build_spring_boot() {
    log_info "开始构建 Spring Boot 应用..."

    # 清理旧的构建
    log_info "清理旧的构建文件..."
    mvn clean

    # 构建 JAR 包（跳过测试以加快速度）
    log_info "编译并打包应用（跳过测试）..."
    mvn package -DskipTests

    if [ $? -eq 0 ]; then
        log_success "Spring Boot 应用构建成功"
        ls -lh target/*.jar
    else
        log_error "Spring Boot 应用构建失败"
        exit 1
    fi
}

# 停止并清理旧容器
cleanup_old_containers() {
    log_info "停止并清理旧容器..."

    docker-compose down -v 2>/dev/null || true

    log_success "旧容器已清理"
}

# 构建 Docker 镜像
build_docker_images() {
    log_info "构建 Docker 镜像..."

    docker-compose build

    if [ $? -eq 0 ]; then
        log_success "Docker 镜像构建成功"
    else
        log_error "Docker 镜像构建失败"
        exit 1
    fi
}

# 启动所有服务
start_services() {
    log_info "启动所有服务..."

    docker-compose up -d

    if [ $? -eq 0 ]; then
        log_success "所有服务已启动"
    else
        log_error "服务启动失败"
        exit 1
    fi
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."

    # 等待 MySQL
    log_info "等待 MySQL 启动..."
    sleep 10

    # 等待 Spring Boot 应用
    log_info "等待 Spring Boot 应用启动..."
    MAX_RETRIES=30
    RETRY_COUNT=0

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -s http://localhost:8080/actuator/health &> /dev/null || \
           curl -s http://localhost:8080/ &> /dev/null; then
            log_success "Spring Boot 应用已就绪"
            break
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 2
    done

    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        log_warning "Spring Boot 应用启动超时，请检查日志"
    fi

    echo ""
}

# 显示服务状态
show_status() {
    log_info "服务状态："
    echo ""
    docker-compose ps
    echo ""
}

# 显示访问信息
show_access_info() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║                    部署成功！                             ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    echo -e "${CYAN}访问地址：${NC}"
    echo -e "  🌐 Web Dashboard:    ${GREEN}http://localhost:8080${NC}"
    echo -e "  📚 API 文档:          ${GREEN}http://localhost:8080/swagger-ui/index.html${NC}"
    echo -e "  🗄️  H2 Console:       ${GREEN}http://localhost:8080/h2-console${NC}"
    echo -e "  🐍 Python Worker:    ${GREEN}http://localhost:5000${NC}"
    echo ""
    echo -e "${CYAN}常用命令：${NC}"
    echo -e "  查看日志:    ${YELLOW}docker-compose logs -f${NC}"
    echo -e "  查看应用日志: ${YELLOW}docker-compose logs -f app${NC}"
    echo -e "  停止服务:    ${YELLOW}docker-compose down${NC}"
    echo -e "  重启服务:    ${YELLOW}docker-compose restart${NC}"
    echo ""
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  all              部署所有模块（默认）"
    echo "  build            仅构建 Spring Boot 应用"
    echo "  docker           仅构建 Docker 镜像"
    echo "  start            启动所有服务"
    echo "  stop             停止所有服务"
    echo "  restart          重启所有服务"
    echo "  status           查看服务状态"
    echo "  logs             查看所有日志"
    echo "  clean            清理所有容器和镜像"
    echo "  help             显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                # 完整部署"
    echo "  $0 build          # 仅构建应用"
    echo "  $0 restart        # 重启服务"
    echo ""
}

# 停止服务
stop_services() {
    log_info "停止所有服务..."
    docker-compose down
    log_success "所有服务已停止"
}

# 重启服务
restart_services() {
    log_info "重启所有服务..."
    docker-compose restart
    log_success "所有服务已重启"
}

# 查看日志
view_logs() {
    log_info "查看服务日志（按 Ctrl+C 退出）..."
    docker-compose logs -f
}

# 清理所有资源
clean_all() {
    log_warning "这将删除所有容器、镜像和数据卷，是否继续？(y/N)"
    read -r response

    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        log_info "清理所有资源..."
        docker-compose down -v --rmi all
        rm -rf target/
        log_success "清理完成"
    else
        log_info "取消清理操作"
    fi
}

# 主函数
main() {
    print_banner

    # 解析命令行参数
    case "${1:-all}" in
        all)
            check_dependencies
            check_env
            build_spring_boot
            cleanup_old_containers
            build_docker_images
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        build)
            check_dependencies
            build_spring_boot
            ;;
        docker)
            check_dependencies
            build_docker_images
            ;;
        start)
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            view_logs
            ;;
        clean)
            clean_all
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知选项: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
