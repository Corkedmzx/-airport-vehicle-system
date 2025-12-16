#!/bin/bash

echo "========================================"
echo "  机场车辆监控与调度系统 - 后端启动脚本"
echo "========================================"
echo

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到Java，请先安装Java 17+"
    echo "安装命令: sudo apt install openjdk-17-jdk (Ubuntu/Debian)"
    echo "或: brew install openjdk@17 (macOS)"
    exit 1
fi

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未检测到Maven，请先安装Maven 3.8+"
    echo "安装命令: sudo apt install maven (Ubuntu/Debian)"
    echo "或: brew install maven (macOS)"
    exit 1
fi

# 进入后端目录
cd "$(dirname "$0")/backend" || exit 1

if [ ! -f "pom.xml" ]; then
    echo "[错误] 未找到pom.xml文件，请确保在项目根目录运行此脚本"
    exit 1
fi

# 检查配置文件是否存在
if [ ! -f "src/main/resources/application.yml" ]; then
    echo "[提示] 未找到application.yml配置文件"
    echo "[提示] 正在从示例文件创建..."
    if [ -f "src/main/resources/application-example.yml" ]; then
        cp "src/main/resources/application-example.yml" "src/main/resources/application.yml"
        echo "[提示] 已创建application.yml，请编辑数据库配置后重新运行"
        echo "[提示] 配置文件位置: backend/src/main/resources/application.yml"
        echo
        echo "可以使用以下命令编辑配置:"
        echo "  nano src/main/resources/application.yml"
        echo "  或"
        echo "  vim src/main/resources/application.yml"
        exit 1
    else
        echo "[错误] 未找到配置文件模板"
        exit 1
    fi
fi

# 检查是否已编译
if [ ! -f "target/airport-vehicle-system-1.0.0.jar" ]; then
    echo "[提示] 未找到编译后的JAR文件，开始编译..."
    echo
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "[错误] 编译失败，请检查错误信息"
        exit 1
    fi
    echo
    echo "[成功] 编译完成"
    echo
fi

# 检查数据库配置提示
echo "[提示] 请确保已配置数据库连接信息"
echo "[提示] 配置文件: backend/src/main/resources/application.yml"
echo "[提示] 如果数据库未配置，请先编辑配置文件"
echo
read -p "按Enter键继续启动，或按Ctrl+C取消... "
echo

# 启动应用
echo "[信息] 正在启动后端服务..."
echo "[信息] 后端地址: http://localhost:8080/api"
echo "[信息] API文档: http://localhost:8080/api/doc.html"
echo "[信息] 按Ctrl+C停止服务"
echo
java -jar target/airport-vehicle-system-1.0.0.jar

