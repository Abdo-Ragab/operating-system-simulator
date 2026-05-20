@echo off
cd /d "%~dp0"

echo === Compiling ===
if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src\os\*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo === Launching OS Simulator ===
java -cp bin os.Main

pause