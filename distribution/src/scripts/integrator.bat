@echo off

rem ---------------------------------------------------------------------------
rem   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
rem
rem   Licensed under the Apache License, Version 2.0 (the "License");
rem   you may not use this file except in compliance with the License.
rem   You may obtain a copy of the License at
rem
rem   http://www.apache.org/licenses/LICENSE-2.0
rem
rem   Unless required by applicable law or agreed to in writing, software
rem   distributed under the License is distributed on an "AS IS" BASIS,
rem   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem   See the License for the specific language governing permissions and
rem   limitations under the License.

rem ---------------------------------------------------------------------------
rem Startup Script for Integrator
rem
rem Environment Variable Prerequisites
rem
rem   JAVA_HOME       Must point at your Java Development Kit installation.
rem
rem   JAVA_OPTS       (Optional) Java runtime options used when the commands
rem                   is executed.
rem ---------------------------------------------------------------------------

rem ----- if JAVA_HOME is not set we're not happy -----------------------------

:checkJava

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto checkServer

:noJavaHome
echo "You must set the JAVA_HOME variable before running Ballerina."
goto end

rem ----- set BALLERINA_HOME ----------------------------
:checkServer
rem %~sdp0 is expanded pathname of the current script under NT with spaces in the path removed
rem set BALLERINA_HOME=%~sdp0..
set BALLERINA_HOME=%~sdp0..\wso2\ballerina
SET curDrive=%cd:~0,1%
SET ballerinaDrive=%BALLERINA_HOME:~0,1%
if not "%curDrive%" == "%ballerinaDrive%" %ballerinaDrive%:

goto updateClasspath

:noServerHome
echo BALLERINA_HOME is set incorrectly or BALLERINA could not be located. Please set BALLERINA_HOME.
goto end

rem ----- update classpath -----------------------------------------------------
:updateClasspath

setlocal EnableDelayedExpansion
set BALLERINA_CLASSPATH=
FOR %%C in ("%BALLERINA_HOME%\bre\lib\bootstrap\*.jar") DO set BALLERINA_CLASSPATH=!BALLERINA_CLASSPATH!;"%BALLERINA_HOME%\bre\lib\bootstrap\%%~nC%%~xC"

set BALLERINA_CLASSPATH="%JAVA_HOME%\lib\tools.jar";%BALLERINA_CLASSPATH%;

FOR %%D in ("%BALLERINA_HOME%\bre\lib\*.jar") DO set BALLERINA_CLASSPATH=!BALLERINA_CLASSPATH!;"%BALLERINA_HOME%\bre\lib\%%~nD%%~xD"

rem ----- Process the input command -------------------------------------------

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).

:setupArgs
if ""%1""==""debug""    goto commandDebug
if ""%1""==""-debug""   goto commandDebug
if ""%1""==""--debug""  goto commandDebug
if not ""%1""=="""" goto doneStart
goto noBalFile


rem TODO: Add build command once it is available in ballerina

rem ----- commandDebug ---------------------------------------------------------
:commandDebug
shift
set DEBUG_PORT=%1
if "%DEBUG_PORT%"=="" goto noDebugPort
if not "%JAVA_OPTS%"=="" echo Warning !!!. User specified JAVA_OPTS will be ignored, once you give the --debug option.
set JAVA_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%DEBUG_PORT%
shift
set BAL_LOCATION=%1
if "%BAL_LOCATION%"=="" goto noBalFile
set COMMANDLINE=run %BAL_LOCATION%

echo Please start the remote debugging client to continue...
goto runServer

:noDebugPort
echo Please specify the debug port after the --debug option
goto end

:doneStart
set COMMANDLINE=run %1
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal
goto runServer

:noBalFile
echo Please specify the directory which contains program files or the location of compiled ballerina program
goto end

rem ----------------- Execute The Requested Command ----------------------------

:runServer

set CMD=%*

rem ---------- Add jars to classpath ----------------

set BALLERINA_CLASSPATH=.\bre\lib\bootstrap;%BALLERINA_CLASSPATH%

set JAVA_ENDORSED=".\bre\lib\bootstrap\endorsed";"%JAVA_HOME%\jre\lib\endorsed";"%JAVA_HOME%\lib\endorsed"

set CMD_LINE_ARGS=-Xbootclasspath/a:%BALLERINA_XBOOTCLASSPATH% -Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%BALLERINA_HOME%\heap-dump.hprof"  -Dcom.sun.management.jmxremote -classpath %BALLERINA_CLASSPATH% %JAVA_OPTS% -Djava.endorsed.dirs=%JAVA_ENDORSED%  -Dballerina.home="%BALLERINA_HOME%"  -Djava.command="%JAVA_HOME%\bin\java" -Djava.opts="%JAVA_OPTS%" -Djava.io.tmpdir="%BALLERINA_HOME%\tmp" -Denable.nonblocking=false -Dtransports.netty.conf="%BALLERINA_HOME%\bre\conf\netty-transports.yml" -Dfile.encoding=UTF8 -Dballerina.version=0.95.5 -Djava.util.logging.config.file="%BALLERINA_HOME%\bre\conf\logging.properties" -Djava.util.logging.manager="org.ballerinalang.logging.BLogManager"


:runJava
"%JAVA_HOME%\bin\java" %CMD_LINE_ARGS% org.ballerinalang.launcher.Main %COMMANDLINE%

:end
