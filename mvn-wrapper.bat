@echo off
REM Set JAVA_HOME for this session
set JAVA_HOME=C:\Program Files\Java\jdk-17
set MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

REM Run Maven
"%MAVEN_HOME%\bin\mvn.cmd" %*
