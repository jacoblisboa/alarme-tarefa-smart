#!/bin/sh
cd "$(dirname "$0")" || exit 1

# No Linux o vlcj encontra a libvlc instalada pelo sistema (ex.: sudo apt install vlc)
exec java -jar alarme-tarefa-smart.jar "$@"
