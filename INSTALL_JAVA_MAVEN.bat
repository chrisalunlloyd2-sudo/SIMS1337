@echo off
REM Java 17 + Maven Installation Script
REM Run this AFTER downloading the files

echo ========================================
echo JAVA 17 + MAVEN INSTALLATION
echo ========================================
echo.

REM Check if already installed
java --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Java is already installed
    java --version
) else (
    echo [!] Java NOT found. Please install manually:
    echo.
    echo 1. Download Java 17 JDK:
    echo    https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe
    echo.
    echo 2. Install to: C:\Program Files\Java\jdk-17
    echo.
    echo 3. Add to PATH:
    echo    C:\Program Files\Java\jdk-17\bin
    echo.
)

echo.
mvn --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Maven is already installed
    mvn --version
) else (
    echo [!] Maven NOT found. Please install manually:
    echo.
    echo 1. Download Maven 3.9.6:
    echo    https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
    echo.
    echo 2. Extract to: C:\Program Files\Apache\Maven
    echo.
    echo 3. Add to PATH:
    echo    C:\Program Files\Apache\Maven\apache-maven-3.9.6\bin
    echo.
)

echo.
echo ========================================
echo AFTER INSTALLATION, RUN:
echo ========================================
echo.
echo cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
echo mvn clean test
echo.
echo ========================================
pause
