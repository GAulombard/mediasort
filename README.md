# mediasort

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> Outil CLI Java 21+ pour trier photos et vidéos par année et mois.

## Prerequisites

Java 21+ and Maven required

### Linux/Mac/WSL
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk install maven
```

### Windows
```bash
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
Verify installation
java -version   # should show 21+
mvn -version    # should show 3.x+
```

## Installation

```bash
git clone https://github.com/GAulombard/mediasort.git
cd mediasort
chmod +x run.sh   # Linux/Mac uniquement
```

## Installation globale (optionnel)

```bash
# Compiler le JAR
mvn clean package

# Installe mediasort dans ~/.mediasort/bin et ajoute au PATH automatiquement
java -jar target/mediasort.jar --install

# Puis depuis n'importe où :
mediasort /photos --dry-run
```

## Utilisation

```bash
# 1. Simuler d'abord (recommandé) — destination par défaut : /photos/mediasort/
./run.sh /chemin/source --dry-run

# 2. Copie réelle avec destination explicite
./run.sh /chemin/source /chemin/destination

# 3. Déplacement (supprime les originaux)
./run.sh /chemin/source /chemin/destination --move

# 4. Tri par année uniquement (sans sous-dossier mois)
./run.sh /chemin/source /chemin/destination --no-month

# 5. Avec plus de threads et logs détaillés
./run.sh /chemin/source /chemin/destination --verbose --threads=8

# 6. Ignorer les screenshots (insensible à la casse)
./run.sh /chemin/source --exclude-pattern screenshot

# 7. Plusieurs patterns d'exclusion
./run.sh /chemin/source --exclude-pattern screenshot --exclude-pattern IMG_BURST

# 8. Supprimer les fichiers exclus de la source
./run.sh /chemin/source --exclude-pattern screenshot --delete-excluded
```

Windows :

```bat
run.bat "C:\Photos" --dry-run
run.bat "C:\Photos" "D:\Backup" --move --verbose
run.bat "C:\Photos" --exclude-pattern screenshot --delete-excluded
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
| `<source>` | Dossier source (requis) | — |
| `<destination>` | Dossier destination | `<source>/mediasort/` |
| `--move` | Déplacer au lieu de copier | false |
| `--no-month` | Tri par année uniquement | false |
| `--dry-run` | Simulation sans écriture | false |
| `--threads=N` | Nombre de virtual threads | 4 |
| `--verbose` | Logs détaillés | false |
| `--exclude-pattern <pat>` | Ignorer les fichiers dont le nom contient `<pat>` (répétable, insensible à la casse) | — |
| `--delete-excluded` | Supprimer les fichiers exclus de la source | false |
| `--install` | Installer mediasort globalement (`~/.mediasort/bin`) | — |
| `--rebuild` | Force la recompilation du JAR | false |
| `--help` | Affiche l'aide | — |

## License

MIT © 2026 Hodor — see [LICENSE](LICENSE) for the full text.
