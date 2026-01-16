# 后端服务启动脚本
# 自动读取 .env 文件并设置环境变量，然后启动后端服务
# 
# 使用方法：
#   .\start-backend.ps1
#   或
#   .\start-backend.ps1 -Profile dev
#
# 参数：
#   -Profile: Spring Boot profile (dev, prod等)，默认: dev

param(
    [string]$Profile = "dev"
)

# 获取脚本所在目录
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  机场车辆系统 - 后端服务启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 .env 文件是否存在
$EnvFile = Join-Path $ScriptDir ".env"
if (-not (Test-Path $EnvFile)) {
    Write-Host "错误：找不到 .env 文件" -ForegroundColor Red
    Write-Host "   请先复制 .env.example 为 .env 并填写配置" -ForegroundColor Yellow
    Write-Host "   文件路径: $EnvFile" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   命令: Copy-Item .env.example .env" -ForegroundColor Cyan
    exit 1
}

Write-Host "读取环境变量配置文件: .env" -ForegroundColor Green

# 读取 .env 文件并设置环境变量
$EnvVars = @{}
$LineNumber = 0

Get-Content $EnvFile | ForEach-Object {
    $LineNumber++
    $Line = $_.Trim()
    
    # 跳过空行和注释
    if ($Line -eq "" -or $Line.StartsWith("#")) {
        return
    }
    
    # 解析 KEY=VALUE 格式
    if ($Line -match '^([^=]+)=(.*)$') {
        $Key = $Matches[1].Trim()
        $Value = $Matches[2].Trim()
        
        # 移除引号（如果存在）
        if ($Value.StartsWith('"') -and $Value.EndsWith('"')) {
            $Value = $Value.Substring(1, $Value.Length - 2)
        } elseif ($Value.StartsWith("'") -and $Value.EndsWith("'")) {
            $Value = $Value.Substring(1, $Value.Length - 2)
        }
        
        # 设置环境变量
        [Environment]::SetEnvironmentVariable($Key, $Value, "Process")
        $EnvVars[$Key] = $Value
        
        Write-Host "   [OK] $Key = $('*' * [Math]::Min($Value.Length, 20))" -ForegroundColor Gray
    } else {
        Write-Host "   [警告] 第 $LineNumber 行格式不正确: $Line" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "已加载 $($EnvVars.Count) 个环境变量" -ForegroundColor Green
Write-Host ""

# 验证关键环境变量
$RequiredVars = @("DB_PASSWORD", "JWT_SECRET")
$MissingVars = @()

foreach ($Var in $RequiredVars) {
    if (-not $EnvVars.ContainsKey($Var) -or [string]::IsNullOrEmpty($EnvVars[$Var])) {
        $MissingVars += $Var
    }
}

if ($MissingVars.Count -gt 0) {
    Write-Host "错误：缺少必需的环境变量:" -ForegroundColor Red
    foreach ($Var in $MissingVars) {
        Write-Host "   - $Var" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "   请在 .env 文件中配置这些变量" -ForegroundColor Yellow
    exit 1
}

# 显示配置摘要
Write-Host "配置摘要:" -ForegroundColor Cyan
Write-Host "数据库: $($EnvVars['DB_NAME']) @ $($EnvVars['DB_HOST']):$($EnvVars['DB_PORT'])" -ForegroundColor Gray
Write-Host "用户: $($EnvVars['DB_USERNAME'])" -ForegroundColor Gray
if ($EnvVars.ContainsKey('HUAWEI_IOT_MQTT_ENABLED') -and $EnvVars['HUAWEI_IOT_MQTT_ENABLED'] -eq 'true') {
    Write-Host "MQTT: 已启用 (设备: $($EnvVars['HUAWEI_IOT_MQTT_DEVICE_ID']))" -ForegroundColor Gray
} else {
    Write-Host "MQTT: 未启用" -ForegroundColor Gray
}
Write-Host "   Profile: $Profile" -ForegroundColor Gray
Write-Host ""

# 检查 Maven 是否安装
$MavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $MavenCmd) {
    Write-Host "错误：找不到 Maven (mvn)" -ForegroundColor Red
    Write-Host "请确保 Maven 已安装并添加到 PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host "启动后端服务..." -ForegroundColor Green
Write-Host ""

# 设置 Spring Boot Profile
$env:SPRING_PROFILES_ACTIVE = $Profile

# 启动 Spring Boot 应用
try {
    mvn spring-boot:run -Dspring-boot.run.profiles=$Profile
} catch {
    Write-Host ""
    Write-Host "启动失败: $_" -ForegroundColor Red
    exit 1
}
