#!/usr/bin/env bash
#
# run.sh — scheduler Liquidsoap hebdomadaire inside scheduledShow/
#

# 1️⃣ Répertoire racine (où se trouve ce script)
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2️⃣ Jours de la semaine (0=dimanche … 6=samedi)
DAYS=(dimanche lundi mardi mercredi jeudi vendredi samedi)

# 3️⃣ Création automatique de l’arborescence hebdo/jour/heure
echo "[INFO] Initialisation de l’arborescence hebdomadaire dans $BASE_DIR…"
for day in "${DAYS[@]}"; do
  DAY_DIR="$BASE_DIR/$day"
  mkdir -p "$DAY_DIR"
  chown nobody:nogroup "$DAY_DIR"
  chmod 2775 "$DAY_DIR"

  for h in $(seq -w 1 24); do
    HOUR_DIR="$DAY_DIR/$h"
    mkdir -p "$HOUR_DIR/Music" "$HOUR_DIR/jingles"
    chown -R nobody:nogroup "$HOUR_DIR"
    chmod -R 2775 "$HOUR_DIR"
  done
done
echo "[INFO] Arborescence prête (droits nobody:nogroup, perms 2775)."

# 4️⃣ Variables de suivi
current_day=""
current_hour=""
current_pid=""     # PID du process Liquidsoap en cours
live_mode=0        # 0 = AutoDJ, 1 = live

# 5️⃣ Intervalle de vérif (secondes)
SLEEP_INTERVAL=30

# 6️⃣ Fonction de lancement d’AutoDJ
start_automdj() {
  if [[ -f "$SHOW_DIR/radio.liq" ]]; then
    echo "     🔵 (Re)lancement AutoDJ : liquidsoap radio.liq"
    cd "$SHOW_DIR" || return 1
    liquidsoap radio.liq &
    current_pid=$!
    echo "     → PID $current_pid"
  else
    echo "     → Aucune radio.liq dans $SHOW_DIR, on ne lance pas d’AutoDJ."
  fi
}

# 7️⃣ Boucle principale
while true; do
  # ➊ Jour et heure actuels
  day_idx=$(date +%w)
  day_name=${DAYS[$day_idx]}
  hour_num=$(date +%-H); [ "$hour_num" -eq 0 ] && hour_num=24
  hour=$(printf "%02d" "$hour_num")
  SHOW_DIR="$BASE_DIR/$day_name/$hour"

  # ➋ Changement d’heure/jour → redémarrage AutoDJ (si pas en live)
  if [[ "$day_name" != "$current_day" || "$hour" != "$current_hour" ]]; then
    echo "[$(date)] Changement horaire → ${day_name^} ${hour}h : $SHOW_DIR"
    if [[ -f "$SHOW_DIR/radio.liq" ]]; then
      echo "     🔄 Changement horaire avec radio.liq détecté → redémarrage AutoDJ"
      # si un AutoDJ tournait, on le tue
      if [[ -n "$current_pid" ]] && kill -0 "$current_pid" 2>/dev/null; then
        echo "         Arrêt flux précédent (PID $current_pid)"
        kill "$current_pid" && wait "$current_pid"
        current_pid=""
      fi
      live_mode=0
      start_automdj
      current_day=$day_name
      current_hour=$hour
    else
      echo "     ⚠️ Pas de radio.liq dans $SHOW_DIR → on laisse jouer le flux existant"
      # on ne met pas à jour current_day/current_hour pour retenter plus tard
    fi
  fi

  # ➌ Gestion live ↔ AutoDJ en continu
  if [[ -f "$SHOW_DIR/live" ]]; then
    # si on passe en live et qu’on était en AutoDJ
    if [[ $live_mode -eq 0 ]]; then
      echo "[$(date)] live détecté → bascule en direct."
      if [[ -n "$current_pid" ]] && kill -0 "$current_pid" 2>/dev/null; then
        kill "$current_pid" && wait "$current_pid"
        current_pid=""
      fi
      live_mode=1
    fi
  else
    # si on sort du live et qu’on était en live
    if [[ $live_mode -eq 1 ]]; then
      echo "[$(date)] fin du live → reprise AutoDJ."
      live_mode=0
      start_automdj
      # on peut mettre à jour current_pid, mais pas current_day/hour
    fi
  fi

  sleep "$SLEEP_INTERVAL"
done
