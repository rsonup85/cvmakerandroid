@rem  
@if "%DEBUG%" == "" @echo off  
set DIRNAME=%~dp0  
if "%DIRNAME%" == "" set DIRNAME=.  
set APP_BASE_NAME=%~n0  
set APP_HOME=%DIRNAME%  
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar  
set CMD_LINE_ARGS=%*  
java -jar "%WRAPPER_JAR%" %CMD_LINE_ARGS% 
