@echo off
title BharatConnect Full Stack Starter (Backend + App)
color 0B
cls

echo =======================================================================
echo          🇮🇳 BHARATCONNECT FULL STACK 1-CLICK LAUNCHER 🇮🇳
echo          Launching Universal API Server & Kivy Mobile App
echo =======================================================================
echo.

cd /d "%~dp0"

echo 🚀 1. Launching Universal Backend Server on http://127.0.0.1:8000 ...
start "BharatConnect API Server" cmd /k "start_backend.bat"

timeout /t 2 /nobreak >nul

echo ⚡ 2. Launching BharatConnect Application Client...
call start.bat
