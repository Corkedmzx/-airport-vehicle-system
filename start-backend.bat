@echo off
chcp 65001 >nul
echo ========================================
echo   机场车辆监控与调度系统 - 后端启动脚本
echo ========================================
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Java，请先安装Java 17+
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM 检查Maven是否安装
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Maven，请先安装Maven 3.8+
    echo 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM 进入后端目录
cd /d %~dp0backend
if not exist "pom.xml" (
    echo [错误] 未找到pom.xml文件，请确保在项目根目录运行此脚本
    pause
    exit /b 1
)

REM 检查配置文件是否存在
if not exist "src\main\resources\application.yml" (
    echo [提示] 未找到application.yml配置文件
    echo [提示] 正在从示例文件创建...
    if exist "src\main\resources\application-example.yml" (
        copy "src\main\resources\application-example.yml" "src\main\resources\application.yml" >nul
        echo [提示] 已创建application.yml，请编辑数据库配置后重新运行
        echo [提示] 配置文件位置: backend\src\main\resources\application.yml
        pause
        exit /b 1
    ) else (
        echo [错误] 未找到配置文件模板
        pause
        exit /b 1
    )
)

REM 检查是否已编译
if not exist "target\airport-vehicle-system-1.0.0.jar" (
    echo [提示] 未找到编译后的JAR文件，开始编译...
    echo.
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [错误] 编译失败，请检查错误信息
        pause
        exit /b 1
    )
    echo.
    echo [成功] 编译完成
    echo.
)

REM 检查数据库配置提示
echo [提示] 请确保已配置数据库连接信息
echo [提示] 配置文件: backend\src\main\resources\application.yml
echo [提示] 如果数据库未配置，请先编辑配置文件
echo.
echo 按任意键继续启动，或按Ctrl+C取消...
pause >nul
echo.

REM 启动应用
echo [信息] 正在启动后端服务...
echo [信息] 后端地址: http://localhost:8080/api
echo [信息] API文档: http://localhost:8080/api/doc.html
echo [信息] 按Ctrl+C停止服务
echo.
java -jar target/airport-vehicle-system-1.0.0.jar

pause

