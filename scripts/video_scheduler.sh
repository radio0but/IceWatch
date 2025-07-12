#!/bin/bash

# === Video Scheduler v4 — Playlist concat en boucle sans coupure ===

SCHEDULE_ROOT="/srv/owncast-schedule"
OWNCAST_RTMP="rtmp://localhost:1935/live"
STREAM_KEY="${STREAM_KEY:-changeme}"
FFMPEG_CMD="ffmpeg"
LOG_FILE="/var/log/owncast-video-scheduler.log"
CHECK_INTERVAL=10

CURRENT_SLOT=""
CURRENT_STREAM_PID=""
LIVE_FLAG_PATH=""

declare -A DAY_PREFIX=(
  [Sunday]="1Dimanche"
  [Monday]="2Lundi"
  [Tuesday]="3Mardi"
  [Wednesday]="4Mercredi"
  [Thursday]="5Jeudi"
  [Friday]="6Vendredi"
  [Saturday]="7Samedi"
)

log() {
  echo "[$(date +"%F %T")] $1" | tee -a "$LOG_FILE"
}

initialize_structure() {
  for key in "${!DAY_PREFIX[@]}"; do
    day_folder="${DAY_PREFIX[$key]}"
    for hour in $(seq -w 0 23); do
      mkdir -p "$SCHEDULE_ROOT/$day_folder/$hour/video"
    done
  done
}

start_stream() {
  local dir="$1"
  local video_dir="$dir/video"
  local playlist_file="$(mktemp /tmp/playlist.XXXXXX.txt)"

  mapfile -t videos < <(find "$video_dir" -maxdepth 1 -type f | sort)
  if [[ ${#videos[@]} -eq 0 ]]; then
    log "Aucune vidéo trouvée dans $video_dir."
    return
  fi

  log "--- Playlist pour $dir ---"
  for vid in "${videos[@]}"; do
    echo "file '$vid'" >> "$playlist_file"
    log "Incluse : $vid"
  done
  log "-----------------------------"

  log "Démarrage du stream avec playlist : $playlist_file"

  $FFMPEG_CMD -v verbose -re -stream_loop -1 -f concat -safe 0 -i "$playlist_file" \
    -c:v libx264 -preset veryfast -b:v 1500k \
    -c:a aac -b:a 128k -ar 44100 \
    -f flv "$OWNCAST_RTMP/$STREAM_KEY" >> "$LOG_FILE" 2>&1 &

  CURRENT_STREAM_PID=$!
  CURRENT_SLOT="$dir"
  log "Flux lancé (PID $CURRENT_STREAM_PID)"
}

stop_stream() {
  if [[ -n "$CURRENT_STREAM_PID" ]]; then
    log "Arrêt du stream PID $CURRENT_STREAM_PID..."
    kill "$CURRENT_STREAM_PID" 2>/dev/null
    wait "$CURRENT_STREAM_PID" 2>/dev/null
    CURRENT_STREAM_PID=""
    CURRENT_SLOT=""
  fi
}

get_current_slot() {
  JOUR_EN=$(LC_TIME=C date +%A)
  HEURE=$(date +%H)
  SLOT_DIR="${DAY_PREFIX[$JOUR_EN]}"
  echo "$SCHEDULE_ROOT/$SLOT_DIR/$HEURE"
}

should_stop_for_live() {
  LIVE_FLAG_PATH=$(find "$SCHEDULE_ROOT" -name live -type f | head -n 1)
  [[ -n "$LIVE_FLAG_PATH" ]]
}

main_loop() {
  while true; do
    local slot_path
    slot_path=$(get_current_slot)
    mkdir -p "$slot_path/video"

    if should_stop_for_live; then
      if [[ -n "$CURRENT_STREAM_PID" ]]; then
        log "Mode LIVE détecté : arrêt du stream."
        stop_stream
      fi
      sleep $CHECK_INTERVAL
      continue
    fi

    if [[ -f "$slot_path/play" ]]; then
      if [[ "$CURRENT_SLOT" != "$slot_path" ]]; then
        log "Nouveau créneau horaire avec play : $slot_path"
        stop_stream
        start_stream "$slot_path"
      fi
    elif [[ -n "$CURRENT_SLOT" ]]; then
      log "Pas de fichier play dans $slot_path. On continue avec $CURRENT_SLOT."
    else
      log "Aucun slot actif. En attente de fichier play..."
      sleep $CHECK_INTERVAL
      continue
    fi

    sleep $CHECK_INTERVAL
  done
}

initialize_structure
main_loop
