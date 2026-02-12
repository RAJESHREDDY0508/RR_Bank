@REM Maven Wrapper startup batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM

@echo off

@REM Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set MAVEN_CMD_LINE_ARGS=%*

@REM Find the project base dir
set MAVEN_PROJECTBASEDIR=%~dp0

@REM Use Maven directly
set MAVEN_CMD=mvn

%MAVEN_CMD% %MAVEN_CMD_LINE_ARGS%

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@REM End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

exit /B %ERROR_CODE%
