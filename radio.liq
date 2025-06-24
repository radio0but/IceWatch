#!/usr/bin/liquidsoap
# Autoriser l’exécution en root
settings.init.allow_root.set(true)
# 1. Logging & Telnet off
set("log.level", 3)
set("server.telnet", false)

# 2. Paramètres
nb_between   = 2
music_dir    = "./Music/"
jingle_dir   = "./jingles/"
intro_file   = jingle_dir ^ "jingle1.mp3"

# 3. Playlists
songs   = playlist(music_dir, mode="random", reload_mode="watch")
jingles = playlist(jingle_dir, mode="random", reload_mode="watch")

# 4. Strict jingle|musique×N (poids intégraux, pas de float)
pattern = rotate(
  weights = [1, nb_between],
  [jingles, songs]
)

# 5. Intro + boucle
stream = sequence([ single(intro_file), pattern ])

# 6. Métadonnées basiques
def tagger(m) =
  [ ("title", m["title"] ?? path.basename(m["filename"])) ]
end
stream = metadata.map(tagger, stream)

# 7. Source infaillible
stream = mksafe(stream)

# 8. Sortie Icecast
#    – on a enlevé `send_icy_metadata` qui n’existe pas en 2.1.3
output.icecast(
  %mp3(bitrate=192),
  host = "__ICECAST_HOST__",
  port     = 8001,
  user     = "source",
  password = "__SOURCE_PASSWORD__",
  mount    = "/radio",
  name     = "Radio Rosemont",
  description = "AutoDJ + jingles",
  genre    = "Campus Radio",
  fallible = false,
  stream
)
