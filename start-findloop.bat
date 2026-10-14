
@echo off
title FindLoop Launcher

cd /d "%~dp0"

echo =================================
echo       Starting FindLoop
echo =================================

echo Starting Spring Boot backend...
start "FindLoop Backend" cmd /k "cd /d "%~dp0" && mvn spring-boot:run"

echo Starting React frontend...
start "FindLoop Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo FindLoop is starting!
echo Frontend: http://localhost:5173
echo Backend:  http://localhost:8080
echo.
pause