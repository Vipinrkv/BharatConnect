@echo off
title BharatConnect 🇮🇳 — Python Kivy + KivyMD Platform Launcher
color 0A
cls
echo ================================================================
echo  🇮🇳  BHARATCONNECT — Modern Production Text Messaging Platform
echo  Engine: Python 3.11+ - Kivy 2.3 - KivyMD 2.0 - FastAPI Gateway
echo ================================================================
echo.
echo Launching BharatConnect Application...
echo.

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python is not found in system PATH.
    echo Please install Python 3.10+ and add it to environment variables.
    pause
    exit /b 1
)

python main.py

if %errorlevel% neq 0 (
    echo.
    echo [WARNING] Application exited with code %errorlevel%.
    pause
)
