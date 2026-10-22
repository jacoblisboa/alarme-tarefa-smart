@echo off
cd /d "%~dp0"

start /b javaw -jar "-Djna.library.path=C:\Program Files\VideoLAN\VLC" -jar alarme-tarefa-smart.jar
