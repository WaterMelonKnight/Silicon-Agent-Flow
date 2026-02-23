#!/bin/bash

# Silicon Agent Flow 启动脚本
# 用途：统一管理应用的启动、停止、重启和状态查看

set -e

APP_NAME="silicon-agent-flow"
APP_VERSION="1.0.0"
JAR_FILE="target/${APP_NAME}-${APP_VERSION}.jar"
LOG_FILE="app.log"
PID_FILE=".app.pid"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查应用是否正在运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# 获取应用 PID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    else
        pgrep -f "$JAR_FILE" | head -1
    fi
}

# 启动应用
start() {
    print_info "启动 Silicon Agent Flow..."

    # 检查是否已经在运行
    if is_running; then
        print_warning "应用已经在运行中 (PID: $(get_pid))"
        return 1
    fi

    # 检查 JAR 文件是否存在
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR 文件不存在: $JAR_FILE"
        print_info "请先运行: mvn clean package -DskipTests"
        return 1
    fi

    # 加载 .env 文件中的环境变量并转换为 Java 系统属性
    JAVA_OPTS=""
    if [ -f ".env" ]; then
        print_info "加载环境变量从 .env 文件..."
        while IFS='=' read -r key value; do
            # 跳过注释和空行
            if [[ ! "$key" =~ ^#.*$ ]] && [[ -n "$key" ]]; then
                # 移除可能的引号
                value=$(echo "$value" | sed 's/^["'\'']\(.*\)["'\'']$/\1/')

                # 转换为 Spring Boot 系统属性格式
                if [ "$key" = "OPENAI_API_KEY" ]; then
                    JAVA_OPTS="$JAVA_OPTS -Dspring.ai.openai.api-key=$value"
                elif [ "$key" = "OPENAI_BASE_URL" ]; then
                    JAVA_OPTS="$JAVA_OPTS -Dspring.ai.openai.base-url=$value"
                fi
            fi
        done < .env
        print_success "环境变量已加载"
    fi

    # 启动应用
    print_info "正在启动应用..."
    nohup java $JAVA_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    APP_PID=$!
    echo $APP_PID > "$PID_FILE"

    # 等待启动
    print_info "等待应用启动..."
    sleep 5

    # 检查是否启动成功
    if is_running; then
        print_success "应用启动成功！"
        print_info "PID: $(get_pid)"
        print_info "日志文件: $LOG_FILE"
        print_info "访问地址: http://localhost:8080"

        # 显示最近的日志
        echo ""
        print_info "最近的日志输出:"
        tail -10 "$LOG_FILE"
    else
        print_error "应用启动失败"
        print_info "请查看日志: tail -f $LOG_FILE"
        return 1
    fi
}

# 停止应用
stop() {
    print_info "停止 Silicon Agent Flow..."

    if ! is_running; then
        print_warning "应用未运行"
        return 1
    fi

    PID=$(get_pid)
    print_info "正在停止应用 (PID: $PID)..."

    # 尝试优雅停止
    kill "$PID" 2>/dev/null || true

    # 等待进程结束
    for i in {1..10}; do
        if ! is_running; then
            break
        fi
        sleep 1
    done

    # 如果还在运行，强制停止
    if is_running; then
        print_warning "优雅停止失败，强制停止..."
        kill -9 "$PID" 2>/dev/null || true
        sleep 1
    fi

    # 清理 PID 文件
    rm -f "$PID_FILE"

    if ! is_running; then
        print_success "应用已停止"
    else
        print_error "停止应用失败"
        return 1
    fi
}

# 重启应用
restart() {
    print_info "重启 Silicon Agent Flow..."
    stop
    sleep 2
    start
}

# 查看应用状态
status() {
    print_info "检查应用状态..."

    if is_running; then
        PID=$(get_pid)
        print_success "应用正在运行"
        echo ""
        echo "  PID: $PID"
        echo "  日志: $LOG_FILE"
        echo "  URL: http://localhost:8080"
        echo ""

        # 显示进程信息
        print_info "进程信息:"
        ps aux | grep "$JAR_FILE" | grep -v grep

        # 检查端口
        echo ""
        print_info "端口监听:"
        netstat -tlnp 2>/dev/null | grep ":8080" || lsof -i :8080 2>/dev/null || echo "  无法获取端口信息"

        # 检查 API 健康状态
        echo ""
        print_info "API 健康检查:"
        if curl -s http://localhost:8080/api/jobs > /dev/null 2>&1; then
            print_success "API 可访问"
        else
            print_warning "API 不可访问"
        fi
    else
        print_warning "应用未运行"
        return 1
    fi
}

# 查看日志
logs() {
    if [ ! -f "$LOG_FILE" ]; then
        print_error "日志文件不存在: $LOG_FILE"
        return 1
    fi

    if [ "$1" = "-f" ] || [ "$1" = "--follow" ]; then
        print_info "实时查看日志 (Ctrl+C 退出)..."
        tail -f "$LOG_FILE"
    else
        LINES=${1:-50}
        print_info "显示最近 $LINES 行日志:"
        tail -n "$LINES" "$LOG_FILE"
    fi
}

# 编译应用
build() {
    print_info "编译 Silicon Agent Flow..."

    if ! command -v mvn &> /dev/null; then
        print_error "Maven 未安装"
        return 1
    fi

    print_info "运行: mvn clean package -DskipTests"
    mvn clean package -DskipTests

    if [ $? -eq 0 ]; then
        print_success "编译成功！"
        print_info "JAR 文件: $JAR_FILE"
    else
        print_error "编译失败"
        return 1
    fi
}

# 显示帮助信息
usage() {
    cat << EOF
${CYAN}Silicon Agent Flow 服务管理脚本${NC}

用法: $0 {start|stop|restart|status|logs|build|help}

命令:
  ${GREEN}start${NC}       启动应用
  ${GREEN}stop${NC}        停止应用
  ${GREEN}restart${NC}     重启应用
  ${GREEN}status${NC}      查看应用状态
  ${GREEN}logs${NC}        查看日志 (默认显示最近 50 行)
              logs -f     实时查看日志
              logs 100    显示最近 100 行
  ${GREEN}build${NC}       编译应用
  ${GREEN}help${NC}        显示此帮助信息

示例:
  $0 start              # 启动应用
  $0 stop               # 停止应用
  $0 restart            # 重启应用
  $0 status             # 查看状态
  $0 logs -f            # 实时查看日志
  $0 build              # 编译应用

配置:
  应用名称: $APP_NAME
  版本号:   $APP_VERSION
  JAR 文件: $JAR_FILE
  日志文件: $LOG_FILE
  访问地址: http://localhost:8080

EOF
}

# 主函数
main() {
    case "$1" in
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        status)
            status
            ;;
        logs)
            shift
            logs "$@"
            ;;
        build)
            build
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            print_error "未知命令: $1"
            echo ""
            usage
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
