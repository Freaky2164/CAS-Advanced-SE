@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  CAS-Advanced-SE startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and CAS_ADVANCED_SE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\CAS-Advanced-SE.jar;%APP_HOME%\lib\ojdbc14.jar;%APP_HOME%\lib\QTJava.zip;%APP_HOME%\lib\MRJToolkitStubs.zip;%APP_HOME%\lib\dnsns.jar;%APP_HOME%\lib\acrobat.jar;%APP_HOME%\lib\wordProcessing.jar;%APP_HOME%\lib\joc-v14.jar;%APP_HOME%\lib\localedata.jar;%APP_HOME%\lib\sunjce_provider.jar;%APP_HOME%\lib\jtds-1.1.jar;%APP_HOME%\lib\sunpkcs11.jar;%APP_HOME%\lib\edtftpj.jar;%APP_HOME%\lib\poi-5.5.1.jar;%APP_HOME%\lib\postgresql-42.7.8.jar;%APP_HOME%\lib\acrobat-1.1.jar;%APP_HOME%\lib\edtFTPj-1.5.3.jar;%APP_HOME%\lib\MRJToolkitStubs-1.0.jar;%APP_HOME%\lib\ojdbc11-23.26.2.0.0.jar;%APP_HOME%\lib\sunpkcs11-wrapper-1.4.10.jar;%APP_HOME%\lib\commons-codec-1.20.0.jar;%APP_HOME%\lib\commons-collections4-4.5.0.jar;%APP_HOME%\lib\commons-math3-3.6.1.jar;%APP_HOME%\lib\commons-io-2.21.0.jar;%APP_HOME%\lib\SparseBitSet-1.3.jar;%APP_HOME%\lib\log4j-api-2.24.3.jar;%APP_HOME%\lib\checker-qual-3.49.5.jar


@rem Execute CAS-Advanced-SE
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %CAS_ADVANCED_SE_OPTS%  -classpath "%CLASSPATH%" compucrash.CStart %* & call :exitWithErrorLevel

:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
