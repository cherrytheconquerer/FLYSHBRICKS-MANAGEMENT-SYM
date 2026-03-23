@echo off
echo ===================================================
echo   Bricks Management - Build and Deploy
echo ===================================================

set PROJECT=D:\BricksManagement
set TOMCAT=C:\Users\cherry\Desktop\Tomcat\apache-tomcat-10.1.52
set DEPLOY=%TOMCAT%\webapps\bricks

echo.
echo [1] Cleaning old build...
if exist "%PROJECT%\out" rd /s /q "%PROJECT%\out"
mkdir "%PROJECT%\out"

echo.
echo [2] Compiling Java files...
javac -d "%PROJECT%\out" ^
  -cp "%PROJECT%\lib\jakarta.servlet-api-6.0.0.jar;%PROJECT%\lib\mysql-connector-j-9.6.0.jar" ^
  "%PROJECT%\src\db\DBConnection.java" ^
  "%PROJECT%\src\db\ProductionDAO.java" ^
  "%PROJECT%\src\model\ProductionBatch.java" ^
  "%PROJECT%\src\servlet\ApiServlet.java"

if %ERRORLEVEL% NEQ 0 (
  echo ERROR: Compilation failed! See errors above.
  pause
  exit /b 1
)
echo Compilation successful!

echo.
echo [3] Stopping Tomcat...
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo.
echo [4] Setting up Tomcat folder...
if exist "%DEPLOY%" rd /s /q "%DEPLOY%"
mkdir "%DEPLOY%\frontend"
mkdir "%DEPLOY%\WEB-INF\classes"
mkdir "%DEPLOY%\WEB-INF\lib"

echo.
echo [5] Copying files...
xcopy /E /I /Y "%PROJECT%\frontend" "%DEPLOY%\frontend"
xcopy /E /I /Y "%PROJECT%\out" "%DEPLOY%\WEB-INF\classes"
copy /Y "%PROJECT%\lib\*.jar" "%DEPLOY%\WEB-INF\lib\"
copy /Y "%PROJECT%\WebContent\WEB-INF\web.xml" "%DEPLOY%\WEB-INF\web.xml"

echo.
echo [6] Starting Tomcat...
start "" "%TOMCAT%\bin\startup.bat"
timeout /t 10 /nobreak >nul

echo.
echo ===================================================
echo   Bricks Management - DEPLOYMENT COMPLETE!
echo ===================================================
echo.
echo   Open in browser:
echo   http://localhost:8080/bricks/frontend/index.html
echo.
pause
