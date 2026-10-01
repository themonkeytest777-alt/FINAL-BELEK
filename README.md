# TurboMiner (Fabric 1.21.1)

Mod client : minage rapide, vitesse, fly, minage auto, ghost (noclip) et menu de reglages.

| Touche (pave numerique, NumLock actif) | Action |
|---|---|
| 2 | Ouvrir / fermer le menu (sliders) |
| 7 | Minage rapide ON/OFF |
| 8 | Vitesse ON/OFF |
| 6 | Fly ON/OFF |
| 4 | Minage auto ON/OFF |
| 5 | Ghost ON/OFF (re-appui = tu restes ou tu es, position envoyee au serveur) |
| 1 | NoClip ON/OFF (traverse les blocs, tu casses avec le clic gauche) |
| 9 | Boussole biomes ON/OFF (fleche vers le biome neigeux + liste) |
| + / - | Biome suivant / precedent dans la liste |
| 3 | Revenir au biome neigeux le plus proche (auto) |

Toutes les touches sont modifiables dans Options > Controles > TurboMiner.
Config sauvegardee dans `config/turbominer.json`.

Prerequis : Minecraft 1.21.1 + Fabric Loader 0.15.11+ + Fabric API (1.21.1) dans le dossier mods, Java 21.

Build : push sur GitHub, le workflow `.github/workflows/build.yml` compile et publie le .jar en artefact.
En local : Java 21 + Gradle 8.8+ puis `gradle build` (jar dans `build/libs/`).

Utilise le mod en solo ou sur des serveurs qui l'autorisent : sur la plupart des serveurs multijoueur, ces fonctions sont du cheat et menent au ban.
