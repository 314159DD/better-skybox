@echo off
rem Launches RuneLite with the GPU Skybox plugin built in.
rem Jagex account login: the client reads %USERPROFILE%\.runelite\credentials.properties itself and
rem refreshes the tokens. Do NOT export JX_* variables here, that bypasses the refresh and gives a 401.
setlocal
cd /d "%~dp0"
echo Running from %CD%
call "%~dp0gradlew.bat" run --quiet --console=plain
endlocal
