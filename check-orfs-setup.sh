#!/bin/bash

# ORFS 配置测试脚本

echo "=========================================="
echo "OpenROAD Flow Scripts 配置检查"
echo "=========================================="
echo ""

# 1. 检查 Docker
echo "1. 检查 Docker..."
if command -v docker &> /dev/null; then
    echo "   ✓ Docker 已安装"
    docker --version

    if docker ps &> /dev/null; then
        echo "   ✓ Docker 服务运行正常"
    else
        echo "   ✗ Docker 服务未运行或权限不足"
        exit 1
    fi
else
    echo "   ✗ Docker 未安装"
    exit 1
fi
echo ""

# 2. 检查 ORFS 镜像
echo "2. 检查 ORFS 镜像..."
if docker images | grep -q "openroad/orfs"; then
    echo "   ✓ ORFS 镜像已存在"
    docker images | grep openroad/orfs
else
    echo "   ✗ ORFS 镜像不存在"
    echo "   请运行: docker pull openroad/orfs:latest"
    exit 1
fi
echo ""

# 3. 检查工作空间
echo "3. 检查 OpenROAD-flow-scripts 工作空间..."
WORKSPACE="/workspace/OpenROAD-flow-scripts/flow"

if [ -d "$WORKSPACE" ]; then
    echo "   ✓ 工作空间存在: $WORKSPACE"

    if [ -f "$WORKSPACE/Makefile" ]; then
        echo "   ✓ Makefile 存在"
    else
        echo "   ✗ Makefile 不存在"
        exit 1
    fi

    if [ -d "$WORKSPACE/designs/sky130hd/gcd" ]; then
        echo "   ✓ GCD 测试设计存在"
    else
        echo "   ✗ GCD 测试设计不存在"
    fi
else
    echo "   ✗ 工作空间不存在: $WORKSPACE"
    echo "   请修改 application.yml 中的 eda.orfs.workspace 配置"
    exit 1
fi
echo ""

# 4. 测试 Docker 挂载
echo "4. 测试 Docker 挂载..."
if docker run --rm -v "$WORKSPACE:/OpenROAD-flow-scripts/flow" -w /OpenROAD-flow-scripts/flow openroad/orfs:latest ls Makefile &> /dev/null; then
    echo "   ✓ Docker 挂载测试成功"
else
    echo "   ✗ Docker 挂载测试失败"
    exit 1
fi
echo ""

# 5. 检查 Java 环境
echo "5. 检查 Java 环境..."
if command -v java &> /dev/null; then
    echo "   ✓ Java 已安装"
    java -version 2>&1 | head -1
else
    echo "   ✗ Java 未安装"
    exit 1
fi
echo ""

# 6. 检查项目编译
echo "6. 检查项目编译状态..."
if [ -f "target/silicon-agent-flow-1.0.0.jar" ]; then
    echo "   ✓ 项目已编译"
else
    echo "   ⚠ 项目未编译，正在编译..."
    mvn clean package -DskipTests
fi
echo ""

echo "=========================================="
echo "✓ 所有检查通过！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 启动应用: java -jar target/silicon-agent-flow-1.0.0.jar"
echo "2. 检查配置: curl http://localhost:8080/api/jobs/orfs/config"
echo "3. 提交任务: curl -X POST http://localhost:8080/api/jobs -H 'Content-Type: application/json' -d '{\"parameters\":{},\"autoOptimize\":false}'"
echo ""
