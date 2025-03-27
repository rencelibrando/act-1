@echo off
mkdir bin 2>nul
javac -cp "lib\javax.mail.jar;lib\activation.jar" -d bin src\*.java
echo Compilation complete! 