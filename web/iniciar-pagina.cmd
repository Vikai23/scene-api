@echo off
rem Pagina de demonstracao da Scene API.
rem Sobe um servidor de arquivos na porta 3000 so para esta pasta e abre o navegador.
rem O servidor e o jwebserver, que ja vem com o JDK (versao 18 ou mais nova): nao precisa instalar nada.
rem A API precisa estar rodando em http://localhost:8080 (SceneApiApplication).
setlocal

set "JWEBSERVER="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jwebserver.exe" set "JWEBSERVER=%JAVA_HOME%\bin\jwebserver.exe"
rem JDKs baixados pelo IntelliJ ficam em %USERPROFILE%\.jdks
if not defined JWEBSERVER for /d %%D in ("%USERPROFILE%\.jdks\*") do if exist "%%D\bin\jwebserver.exe" set "JWEBSERVER=%%D\bin\jwebserver.exe"
if not defined JWEBSERVER set "JWEBSERVER=jwebserver"

echo.
echo Pagina de demonstracao: http://localhost:3000   (origem liberada no CORS da API)
echo A mesma pagina em http://127.0.0.1:3000 aparece bloqueada: essa origem nao esta na lista.
echo Deixe esta janela aberta. Para parar, feche a janela ou aperte Ctrl+C.
echo.

rem Abre o navegador um instante depois, para dar tempo de o servidor subir
start "" /b powershell -NoProfile -Command "Start-Sleep -Seconds 1; Start-Process 'http://localhost:3000'"

"%JWEBSERVER%" --port 3000 --directory "%~dp0."
if errorlevel 1 (
  echo.
  echo Nao consegui iniciar o jwebserver. Confira se ha um JDK 18 ou mais novo instalado
  echo e se a porta 3000 esta livre.
  pause
)
