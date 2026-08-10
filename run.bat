@echo off
title BharatConnect Quick Launcher
cd /d "%~dp0"
python main.py
if %errorlevel% neq 0 (
    pause
)
