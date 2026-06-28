@echo off
setlocal

:: Check Java 21+
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%g
set JAVA_VER=%JAVA_VER:"=%
for /f "delims=." %%v in ("%JAVA_VER%") do set JAVA_MAJOR=%%v
if "%JAVA_MAJOR%"=="" (
    echo Java not found.
    exit /b 1
)
if %JAVA_MAJOR% LSS 21 (
    echo Java 21+ required. Current version: %JAVA_MAJOR%
    exit /b 1
)

:: Rebuild flag
set REBUILD=0
for %%A in (%*) do (
    if "%%A"=="--rebuild" set REBUILD=1
)

:: Build if needed
if not exist "target\mediasort.jar" set REBUILD=1
if "%REBUILD%"=="1" (
    echo Building JAR...
    call mvn clean package -DskipTests -q
    if errorlevel 1 (
        echo Build failed.
        exit /b 1
    )
    echo Build complete.
)

:: Launch
java -jar target\mediasort.jar %*
