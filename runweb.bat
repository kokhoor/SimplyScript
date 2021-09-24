@echo off
SET _CLASSPATH=%CLASSPATH%
SET JAVA_HOME=C:\jdk11
SET CLASSPATH=.;target\classes
for %%f in (target\*.jar) do call cpappend.bat %%f
for %%f in (target\libs\*.jar) do call cpappend.bat %%f
echo %CLASSPATH%
%JAVA_HOME%\bin\java -cp %CLASSPATH% my.com.solutionx.simplyscript.web.UndertowServer
set CLASSPATH=%_CLASSPATH%