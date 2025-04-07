@echo off
mkdir bin 2>nul
javac -cp "lib\javax.mail.jar;lib\activation.jar;lib\sqlite-jdbc-3.45.1.0.jar;lib\slf4j-api-1.7.36.jar;lib\slf4j-simple-1.7.36.jar" -d bin src\*.java
echo Compilation complete! 