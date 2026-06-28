# mediasort

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> Outil CLI Java 21+ pour trier photos et vidéos par année et mois.

## Prérequis

- Java 21+
- Maven 3.6+

## Installation

```bash
git clone https://github.com/GAulombard/mediasort.git
cd mediasort
chmod +x run.sh   # Linux/Mac uniquement
```

## Utilisation

```bash
# 1. Simuler d'abord (recommandé)
./run.sh /chemin/source /chemin/destination --dry-run

# 2. Copie réelle
./run.sh /chemin/source /chemin/destination

# 3. Déplacement (supprime les originaux)
./run.sh /chemin/source /chemin/destination --move

# 4. Tri par année uniquement (sans sous-dossier mois)
./run.sh /chemin/source /chemin/destination --no-month

# 5. Avec plus de threads et logs détaillés
./run.sh /chemin/source /chemin/destination --verbose --threads=8
```

Windows :

```bat
run.bat "C:\Photos" "D:\Backup" --dry-run          # ou ./run.bat...
run.bat "C:\Photos" "D:\Backup" --move --verbose   # ou ./run.bat...
```

## Structure de sortie

```
destination/
├── 2019/
│   ├── 03_march/
│   └── 08_august/
├── 2023/
│   └── 12_december/
└── unknown/    ← fichiers sans date détectable
```

## Sources de date (ordre de priorité)

1. **JSON Google Takeout** — fichier `.json` sidecar portant le même nom
2. **EXIF** — métadonnées embarquées via `metadata-extractor`
3. **Nom de fichier** — patterns `IMG_YYYYMMDD`, `VID-YYYYMMDD`, `YYYYMMDD_HHmmss`, `Screenshot_YYYY-MM-DD`
4. **Date de modification** — fallback (non fiable, signalé en warning)

## Formats supportés

**Images :** JPG, PNG, GIF, BMP, WebP, HEIC, HEIF, TIFF, RAW (CR2, CR3, NEF, ARW, DNG, ORF, RW2)  
**Vidéos :** MP4, MOV, AVI, MKV, WMV, M4V, 3GP, MTS, M2TS

## Options complètes

| Option | Description | Défaut |
|--------|-------------|--------|
| `--move` | Déplacer au lieu de copier | false |
| `--no-month` | Tri par année uniquement | false |
| `--dry-run` | Simulation sans écriture | false |
| `--threads=N` | Nombre de virtual threads | 4 |
| `--verbose` | Logs détaillés | false |
| `--rebuild` | Force la recompilation du JAR | false |
| `--help` | Affiche l'aide | — |

## License

MIT © 2026 Hodor — see [LICENSE](LICENSE) for the full text.
