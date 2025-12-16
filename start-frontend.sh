#!/bin/bash

echo "========================================"
echo "  机场车辆监控与调度系统 - 前端启动脚本"
echo "========================================"
echo

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "[错误] 未检测到Node.js，请先安装Node.js 16+"
    echo "安装命令: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash - && sudo apt-get install -y nodejs"
    echo "或访问: https://nodejs.org/"
    exit 1
fi

# 检查npm是否安装
if ! command -v npm &> /dev/null; then
    echo "[错误] 未检测到npm"
    exit 1
fi

# 进入前端目录
cd "$(dirname "$0")/frontend" || exit 1

if [ ! -f "package.json" ]; then
    echo "[错误] 未找到package.json文件，请确保在项目根目录运行此脚本"
    exit 1
fi

# 检查node_modules是否存在
if [ ! -d "node_modules" ]; then
    echo "[提示] 未找到node_modules目录，开始安装依赖..."
    echo
    npm install
    if [ $? -ne 0 ]; then
        echo "[错误] 依赖安装失败，请检查错误信息"
        exit 1
    fi
    echo
    echo "[成功] 依赖安装完成"
    echo
fi

# 启动开发服务器
echo "[信息] 正在启动前端开发服务器..."
echo "[信息] 前端地址: http://localhost:5173"
echo "[信息] 按Ctrl+C停止服务"
echo
npm run dev

