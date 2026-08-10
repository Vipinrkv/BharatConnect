@echo off
title BharatConnect 🇮🇳 — Mobile Social App (1-Click Start)
color 0A
cls

echo =======================================================================
echo          🇮🇳 BHARATCONNECT (Python Kivy + KivyMD Edition) 🇮🇳
echo          Sub-50ms Realtime Text Messaging ^& Contact Sync
echo =======================================================================
echo.

cd /d "%~dp0"

echo 🚀 Checking Python environment...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Python is not installed or not in PATH! Please install Python 3.11+.
    echo.
    pause
    exit /b 1
)

echo 📦 Checking required dependencies (Kivy, KivyMD, Pillow, Requests)...
python -c "import kivy, kivymd" >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚙️ Installing required dependencies...
    pip install -r requirements.txt --trusted-host pypi.org --trusted-host files.pythonhosted.org
    if %errorlevel% neq 0 (
        echo ⚠️ Failed to install dependencies automatically.
        pause
        exit /b 1
    )
)

echo.
echo ⚡ Launching BharatConnect Application...
echo.
python main.py

if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Application exited with code %errorlevel%.
    pause
)
