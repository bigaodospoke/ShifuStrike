#!/bin/bash
# Espera o servidor de desenvolvimento subir e manda comandos de teste pelo console.
# Uso: bash run/selftest.sh run/logs/latest.log | ./gradlew runServer
LOG="$1"
for i in $(seq 1 900); do
  if grep -q 'Done (' "$LOG" 2>/dev/null; then break; fi
  sleep 1
done
sleep 5
send() { echo "$1"; sleep "${2:-2}"; }
send "cs mapa limpar tudo"
send "execute in minecraft:overworld positioned 0 -60 0 rotated 90 0 run cs mapa spawn ct"
send "execute in minecraft:overworld positioned 20 -60 0 rotated -90 0 run cs mapa spawn tr"
send "execute in minecraft:overworld run cs mapa bomba a 8 -61 -6 12 -58 -2"
send "execute in minecraft:overworld run cs mapa bomba b 8 -61 2 12 -58 6"
send "execute in minecraft:overworld run cs mapa area -10 -64 -15 30 -40 15"
send "execute in minecraft:overworld positioned 10 -60 40 run cs mapa saida"
send "cs mapa ver"
send "cs diagnostico" 3
send "stop" 1
