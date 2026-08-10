@echo off
title BharatConnect Universal API Server (FastAPI + WebSockets)
color 0B
cls

echo =======================================================================
echo          🌐 BHARATCONNECT UNIVERSAL BACKEND SERVER 🌐
echo          FastAPI REST API ^& WebSocket Realtime Messaging Engine
echo =======================================================================
echo.

cd /d "%~dp0"

echo 🚀 Checking Python environment...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Python is not installed or not in PATH! Please install Python 3.11+.
    pause
    exit /b 1
)

echo 📦 Checking backend dependencies (FastAPI, Uvicorn, SQLAlchemy, PyJWT)...
python -c "import fastapi, uvicorn, sqlalchemy, jose" >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚙️ Installing backend dependencies...
    pip install -r backend/requirements.txt --trusted-host pypi.org --trusted-host files.pythonhosted.org
    if %errorlevel% neq 0 (
        echo ⚠️ Failed to install backend dependencies.
        pause
        exit /b 1
    )
)

echo.
echo ⚡ Starting Universal Backend Server on http://127.0.0.1:8000 ...
echo 📖 Interactive API Documentation available at http://127.0.0.1:8000/docs
echo.
python backend/server.py

if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Backend Server exited with code %errorlevel%.
    pause
)
