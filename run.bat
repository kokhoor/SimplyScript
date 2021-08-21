@echo off
SET _CLASSPATH=%CLASSPATH%
SET JAVA_HOME=C:\jdk11
SET CLASSPATH=.
for %%f in (target\*.jar) do call cpappend.bat %%f
for %%f in (target\libs\*.jar) do call cpappend.bat %%f
echo %CLASSPATH%
%JAVA_HOME%\bin\java -cp %CLASSPATH% my.com.solutionx.simplyscript.CLI %1 %2 %3 %4 %5 %6 %7 %8 %9
set CLASSPATH=%_CLASSPATH%