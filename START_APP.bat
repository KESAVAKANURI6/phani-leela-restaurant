@echo off
title Phani Leela Restaurant App
color 0A

echo.
echo  =========================================
echo   PHANI LEELA RESTAURANT APP
echo  =========================================
echo.
echo  Starting server... Please wait 30-60 seconds.
echo  DO NOT close this window while using the app!
echo.

set COGNODB_URI=bolt+s://db-c557d48d.databases.cognodb.com
set COGNODB_USERNAME=cognodb
set COGNODB_PASSWORD=0530d9e5153be3a70f7ca7de1ecb0e13
set PATH=%PATH%;%USERPROFILE%\maven\apache-maven-3.9.16\bin

echo  Clearing old processes on Port 8080...
powershell -Command "$p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue; if ($p) { Stop-Process -Id $p.OwningProcess -Force -ErrorAction SilentlyContinue }"
timeout /t 2 /nobreak >nul

start /b cmd /c "timeout /t 20 /nobreak >nul && start http://localhost:8080"

mvn spring-boot:run

echo.
echo  Server stopped. Press any key to exit.
pause
