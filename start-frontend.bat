@echo off
chcp 65001 >nul
echo ========================================
echo   机场车辆监控与调度系统 - 前端启动脚本
echo ========================================
echo.

REM 检查Node.js是否安装
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Node.js，请先安装Node.js 16+
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)

REM 检查npm是否安装
npm -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到npm，请先安装npm
    pause
    exit /b 1
)

REM 进入前端目录
cd /d %~dp0frontend
if not exist "package.json" (
    echo [错误] 未找到package.json文件，请确保在项目根目录运行此脚本
    pause
    exit /b 1
)

REM 检查node_modules是否存在
if not exist "node_modules" (
    echo [提示] 未找到node_modules目录，开始安装依赖...
    echo.
    call npm install
    if %errorlevel% neq 0 (
        echo [错误] 依赖安装失败，请检查错误信息
        pause
        exit /b 1
    )
    echo.
    echo [成功] 依赖安装完成
    echo.
)

REM 启动开发服务器
echo [信息] 正在启动前端开发服务器...
echo [信息] 前端地址: http://localhost:5173
echo [信息] 按Ctrl+C停止服务
echo.
call npm run dev

pause

