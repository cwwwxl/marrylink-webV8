@echo off
REM MarryLink Windows 部署脚本
REM 用法: scripts\deploy.bat [命令]

setlocal enabledelayedexpansion

REM 设置颜色（Windows 10+）
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM 获取命令参数
set COMMAND=%1
if "%COMMAND%"=="" set COMMAND=deploy

echo.
echo ==========================================
echo        MarryLink Docker 部署工具
echo ==========================================
echo.

if "%COMMAND%"=="deploy" goto :deploy
if "%COMMAND%"=="start" goto :start
if "%COMMAND%"=="stop" goto :stop
if "%COMMAND%"=="restart" goto :restart
if "%COMMAND%"=="status" goto :status
if "%COMMAND%"=="logs" goto :logs
if "%COMMAND%"=="build" goto :build
if "%COMMAND%"=="cleanup" goto :cleanup
if "%COMMAND%"=="help" goto :help
if "%COMMAND%"=="--help" goto :help
if "%COMMAND%"=="-h" goto :help

echo %RED%[ERROR]%NC% 未知命令: %COMMAND%
echo 使用 'deploy.bat help' 查看帮助信息
goto :eof

:check_docker
echo %BLUE%[INFO]%NC% 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Docker 未安装，请先安装 Docker Desktop
    exit /b 1
)
docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo %RED%[ERROR]%NC% Docker Compose 未安装
        exit /b 1
    )
)
echo %GREEN%[SUCCESS]%NC% Docker 环境检查通过
goto :eof

:check_env
echo %BLUE%[INFO]%NC% 检查环境变量配置...
if not exist .env (
    echo %YELLOW%[WARNING]%NC% .env 文件不存在，从示例文件创建...
    if exist .env.example (
        copy .env.example .env >nul
        echo %GREEN%[SUCCESS]%NC% 已创建 .env 文件
    ) else (
        echo %RED%[ERROR]%NC% .env.example 文件不存在
        exit /b 1
    )
)
echo %GREEN%[SUCCESS]%NC% 环境变量配置检查通过
goto :eof

:deploy
call :check_docker
if errorlevel 1 goto :eof
call :check_env
if errorlevel 1 goto :eof

echo %BLUE%[INFO]%NC% 开始构建 Docker 镜像...
docker-compose build --no-cache
if errorlevel 1 (
    echo %RED%[ERROR]%NC% 镜像构建失败
    goto :eof
)
echo %GREEN%[SUCCESS]%NC% 镜像构建完成

echo %BLUE%[INFO]%NC% 启动所有服务...
docker-compose up -d
if errorlevel 1 (
    echo %RED%[ERROR]%NC% 服务启动失败
    goto :eof
)
echo %GREEN%[SUCCESS]%NC% 服务启动命令已执行

echo %BLUE%[INFO]%NC% 等待服务就绪（约30秒）...
timeout /t 30 /nobreak >nul

call :status

echo.
echo ==========================================
echo %GREEN%🎉 MarryLink 部署完成！%NC%
echo ==========================================
echo.
echo 📌 前端管理后台: %BLUE%http://localhost:80%NC%
echo 📌 后端 API 地址: %BLUE%http://localhost:8080/api/v1%NC%
echo.
echo ==========================================
echo.
goto :eof

:start
echo %BLUE%[INFO]%NC% 启动所有服务...
docker-compose up -d
echo %GREEN%[SUCCESS]%NC% 服务已启动
call :status
goto :eof

:stop
echo %BLUE%[INFO]%NC% 停止所有服务...
docker-compose down
echo %GREEN%[SUCCESS]%NC% 服务已停止
goto :eof

:restart
call :stop
call :start
goto :eof

:status
echo %BLUE%[INFO]%NC% 服务状态:
echo.
docker-compose ps
echo.
goto :eof

:logs
echo %BLUE%[INFO]%NC% 查看日志（按 Ctrl+C 退出）...
if "%2"=="" (
    docker-compose logs -f
) else (
    docker-compose logs -f %2
)
goto :eof

:build
call :check_docker
if errorlevel 1 goto :eof
echo %BLUE%[INFO]%NC% 开始构建 Docker 镜像...
docker-compose build --no-cache
echo %GREEN%[SUCCESS]%NC% 镜像构建完成
goto :eof

:cleanup
echo %YELLOW%[WARNING]%NC% 即将清理所有资源（包括数据），是否继续？
set /p confirm="输入 yes 确认: "
if not "%confirm%"=="yes" (
    echo 操作已取消
    goto :eof
)
echo %BLUE%[INFO]%NC% 清理 Docker 资源...
docker-compose down -v --rmi local
docker system prune -f
echo %GREEN%[SUCCESS]%NC% 清理完成
goto :eof

:help
echo 用法: deploy.bat [命令]
echo.
echo 命令:
echo   deploy    构建并启动所有服务（默认）
echo   start     启动服务
echo   stop      停止所有服务
echo   restart   重启所有服务
echo   status    查看服务状态
echo   logs      查看日志 (可选: logs [service])
echo   build     仅构建镜像
echo   cleanup   清理所有资源（包括数据）
echo   help      显示帮助信息
echo.
goto :eof