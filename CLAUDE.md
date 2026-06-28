# mediasort — Spécification Complète

> Outil CLI en Java 21 pour trier automatiquement photos et vidéos par année et mois.

---

## 1. Contexte du Projet

| Élément | Détail |
|---------|--------|
| **Nom** | `mediasort` |
| **Type** | CLI tool (aucune interface graphique) |
| **Langage** | Java 21 |
| **Build** | Maven avec fat JAR (`maven-assembly-plugin`) |
| **License** | MIT |
| **Objectif** | Scanner un dossier source, détecter la date de chaque média, les copier/déplacer dans une arborescence `YYYY/MM_mois` |

### Features Modernes Java 21 à Utiliser

- **Records** : `CliArgs`, `Stats`, classes de données immuables
- **Sealed interfaces** : pour `MediaType` (image vs vidéo)
- **Pattern matching** : `instanceof` et `switch` expressions
- **Text blocks** : strings multi-lignes (aide, logs)
- **Virtual threads** : parallélisme léger via `Thread.ofVirtual()`

---

## 2. Fonctionnalité Principale

Scanner récursivement un dossier source contenant des photos/vidéos, extraire la date de chaque fichier selon 4 stratégies, puis les copier ou déplacer dans un dossier destination organisé par année/mois.

---

## 3. Structure de Sortie

```
destination/
├── 2019/
│   ├── 03_mars/
│   │   ├── photo1.jpg
│   │   └── video2.mov
│   └── 08_août/
├── 2023/
│   └── 12_déc./
├── 2024/
│   └── 06_juin/
└── inconnu/          ← fichiers sans date détectable
```

### Convention de Nommage

- Dossier année : `YYYY` (ex: `2024`)
- Dossier mois : `MM_mois` (ex: `03_march`, `08_august`)
- Mois en anglais, première lettre en minuscule

---

## 4. Sources de Date (Ordre de Priorité)

| # | Source | Détail |
|---|--------|--------|
| 1 | **JSON Google Takeout** | Fichier `.json` portant le même nom que le média (ex: `IMG_1234.jpg` → `IMG_1234.json`) |
| 2 | **Métadonnées EXIF** | Lecture via `metadata-extractor 2.19.0` |
| 3 | **Pattern dans le nom** | Expressions régulières : `IMG_YYYYMMDD`, `VID-YYYYMMDD`, `YYYYMMDD_HHmmss`, `Screenshot_YYYY-MM-DD` |
| 4 | **Date modification** | Fallback : `Files.getLastModifiedTime()` |

---

## 5. Formats Supportés

```java
// Images
jpg, jpeg, png, gif, bmp, webp, heic, heif, tiff, tif
raw, cr2, cr3, nef, arw, dng, orf, rw2

// Vidéos
mp4, mov, avi, mkv, wmv, m4v, 3gp, mts, m2ts
```

---

## 6. Arguments CLI

```
mediasort <source> <destination> [options]
```

| Option | Description | Défaut |
|--------|-------------|--------|
| `<source>` | Dossier contenant les médias | — |
| `<destination>` | Dossier de destination | — |
| `--move` | Déplacer au lieu de copier | `false` (copie) |
| `--no-month` | Tri par année uniquement (pas de sous-dossier mois) | `false` |
| `--dry-run` | Simulation, aucune écriture disque | `false` |
| `--threads=N` | Nombre de threads (virtual threads) | `4` |
| `--verbose` | Logs détaillés | `false` |
| `--rebuild` | Force recompilation du JAR | `false` |
| `--help` | Affiche l'aide | — |

### Aide à Afficher (text block)

```java
"""
Usage: mediasort <source> <destination> [options]

Trie automatiquement vos photos et vidéos par année et mois.

Arguments positionnels :
  source        Dossier contenant les fichiers à trier
  destination   Dossier de destination (créé si absent)

Options :
  --move         Déplacer au lieu de copier (défaut: copie)
  --no-month     Tri par année uniquement
  --dry-run      Simulation (aucune écriture disque)
  --threads=N    Nombre de threads parallèles (défaut: 4)
  --verbose      Affichage détaillé
  --rebuild      Force la recompilation
  --help         Affiche cette aide

Exemples :
  mediasort /photos /backup/photos --dry-run
  mediasort C:\\Photos D:\\Backup --move
  mediasort /data/photos /sorted --verbose --threads=8
"""
```

---

## 7. Gestion des Doublons

Si un fichier du même nom existe déjà dans la destination :
→ Ajouter un suffixe `_001`, `_002`, etc.

```
photo.jpg         → photo.jpg
photo.jpg (existe) → photo_001.jpg
photo.jpg (existe) → photo_002.jpg
```

---

## 8. Statistiques de Fin d'Exécution

Afficher un récapitulatif :

```text
=== STATISTIQUES ===
Fichiers traités : 847
  ├─ Année 2023 : 412
  ├─ Année 2024 : 298
  ├─ Année 2025 : 89
  └─ Inconnu    : 48
Erreurs          : 3
Temps écoulé     : 2.3s
=====================
```

---

## 9. Dépendances Maven

```xml
<dependencies>
    <!-- Métadonnées EXIF -->
    <dependency>
        <groupId>com.drewnoakes</groupId>
        <artifactId>metadata-extractor</artifactId>
        <version>2.19.0</version>
    </dependency>

    <!-- JSON (pour Google Takeout) -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.1</version>
    </dependency>
</dependencies>
```

---

## 10. Structure des Classes Java

```
src/main/java/dev/mediasort/
├── MediaSort.java       ← Point d'entrée, parse CLI, orchestre
├── CliArgs.java         ← Record : arguments CLI validés
├── MediaType.java       ← Sealed interface + implementations
├── FileScanner.java     ← Scan récursif du dossier source
├── DateExtractor.java   ← Extraction date (4 stratégies)
├── FileProcessor.java   ← Copie/déplacement avec doublons
├── Stats.java           ← Record : statistiques
└── Main.java            ← main() → lance MediaSort
```

### Détails par Classe

#### `CliArgs.java` (record)
```java
public record CliArgs(
    Path source,
    Path destination,
    boolean move,
    boolean noMonth,
    boolean dryRun,
    int threads,
    boolean verbose,
    boolean rebuild
) {}
```

#### `MediaType.java` (sealed interface)
```java
public sealed interface MediaType permits MediaType.Image, MediaType.Video {
    record Image(String extension) implements MediaType {}
    record Video(String extension) implements MediaType {}

    static MediaType from(Path file) { /* ... */ }
    static MediaType from(String filename) { /* ... */ }
}
```

#### `DateExtractor.java`
```java
public class DateExtractor {
    public record DateResult(
        Optional<LocalDateTime> date,
        DateSource source,       // enum: JSON, EXIF, FILENAME, MODIFIED
        boolean isReliable       // false si MODIFIED (fallback)
    ) {}

    // Stratégie 1 : JSON Google Takeout
    private Optional<LocalDateTime> fromJson(Path mediaFile) { /* ... */ }

    // Stratégie 2 : EXIF via metadata-extractor
    private Optional<LocalDateTime> fromExif(Path file) { /* ... */ }

    // Stratégie 3 : Pattern nom fichier
    private Optional<LocalDateTime> fromFilename(String filename) { /* ... */ }

    // Stratégie 4 : Fallback date modification
    private Optional<LocalDateTime> fromModified(Path file) { /* ... */ }

    // Méthode principale : essaye dans l'ordre
    public DateResult extract(Path file) { /* ... */ }
}
```

#### `FileProcessor.java`
```java
public class FileProcessor {
    public Path process(
        Path sourceFile,
        Path destinationRoot,
        LocalDateTime date,
        boolean move,
        boolean noMonth,
        boolean verbose
    ) {
        // Détermine le chemin de destination
        // Gère les doublons (_001, _002)
        // Copie ou déplace
        // Met à jour les stats
    }
}
```

#### `Stats.java` (record)
```java
public record Stats(
    int total,
    Map<Integer, Integer> byYear,
    int unknown,
    int errors,
    long elapsedMs
) {}
```

---

## 11. Scripts de Lancement

### `run.sh` (Linux/Mac)

```bash
#!/usr/bin/env bash
set -e

# Vérifie Java 21
check_java() {
    if ! command -v java &> /dev/null; then
        echo "❌ Java non trouvé. Installez Java 21."
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" != "21" ]; then
        echo "❌ Java 21 requis. Version actuelle: $JAVA_VERSION"
        exit 1
    fi
}

# Build si nécessaire
build() {
    if [ ! -f target/mediasort.jar ] || [ "$1" == "--rebuild" ]; then
        echo "📦 Compilation du JAR..."
        mvn clean package -DskipTests -q
    fi
}

check_java
build "$@"
java -jar target/mediasort.jar "$@"
```

### `run.bat` (Windows)

```batch
@echo off
setlocal

:: Vérifie Java 21
java -version 2>nul | findstr "21" >nul
if errorlevel 1 (
    echo Java 21 requis.
    exit /b 1
)

:: Build si nécessaire
if not exist "target\mediasort.jar" (
    echo Compilation...
    call mvn clean package -DskipTests -q
)

:: Lance
java -jar target\mediasort.jar %*
```

---

## 12. Fichier `.gitignore`

```
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
release.properties

# Java
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar

# OS
.DS_Store
Thumbs.db

# IDE
.idea/
*.iml
.vscode/
.settings/
.project
.classpath
```

---

## 13. README.md à Générer

```markdown
# mediasort

> 🗂️ Outil CLI Java 21 pour trier photos et vidéos par année et mois.

## Installation

```bash
git clone https://github.com/votre-user/mediasort.git
cd mediasort
chmod +x run.sh
```

## Prérequis

- Java 21+
- Maven 3.6+

## Utilisation

```bash
# 1. TOUJOURS simuler d'abord
./run.sh /chemin/source /chemin/destination --dry-run

# 2. Copie réelle
./run.sh /chemin/source /chemin/destination

# 3. Déplacement (supprime les originaux)
./run.sh /chemin/source /chemin/destination --move

# 4. Tri par année uniquement
./run.sh /chemin/source /chemin/destination --no-month

# 5. Avec plus de threads
./run.sh /chemin/source /chemin/destination --threads=8 --verbose
```

## Comment ça marche ?

1. Scan récursif du dossier source
2. Extraction de la date via : JSON Takeout → EXIF → Nom fichier → Modification
3. Copie/déplacement vers `destination/YYYY/MM_mois/`
4. Statistiques en fin d'exécution

## Formats supportés

**Images :** JPG, PNG, GIF, BMP, WebP, HEIC, TIFF, RAW (CR2, CR3, NEF, ARW, DNG...)  
**Vidéos :** MP4, MOV, AVI, MKV, WMV, M4V, 3GP, MTS, M2TS

## Licence

MIT

---

## 14. Contraintes Importantes

| Contrainte                  | Détail                                                                                                                                                     |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Java 21**                 | Records, sealed classes, pattern matching, virtual threads, text blocks                                                                                    |
| **Virtual Threads**         | Utiliser `Thread.ofVirtual().start(Runnable)` — pas de `ForkJoinPool`                                                                                      |
| **Aucun framework lourd**   | Pas de Spring, pas de CDI, juste Java standard + 2 deps                                                                                                    |
| **Code commenté / javadoc** | Commentaires en anglais                                                                                                                                    |
| **Gestion d'erreurs**       | Un fichier en erreur = log + continue, ne bloque pas les autres, peut être créer un fichier de log dans le dossier destination pour historiser les erreurs |
| **Universel**               | Fonctionne avec n'importe quel dossier, pas seulement Google Takeout                                                                                       |

---

## 15. Workflow d'Implémentation Recommandé

1. **Créer structure Maven** → `pom.xml`, `.gitignore`, `README.md`
2. **Implémenter基础** → `CliArgs.java`, `MediaType.java`
3. **Implémenter extraction date** → `DateExtractor.java` avec les 4 stratégies
4. **Implémenter scan** → `FileScanner.java`
5. **Implémenter traitement** → `FileProcessor.java` (copie/déplacement, doublons)
6. **Implémenter stats** → `Stats.java`
7. **Assembler** → `MediaSort.java`, `Main.java`
8. **Créer scripts** → `run.sh`, `run.bat`
9. **Tester** → `--dry-run` sur un dossier réel

---

## 16. Validation Attendue

Après implémentation complète, `mediasort` doit :

- ✅ Accepter `<source>`, `<destination>`, et toutes les options CLI
- ✅ Scanner récursivement n'importe quel dossier
- ✅ Détecter les dates via 4 stratégies dans l'ordre
- ✅ Créer l'arborescence `YYYY/MM_mois` dans destination
- ✅ Gérer les doublons avec suffixe `_001`, `_002`
- ✅ Afficher les stats à la fin
- ✅ Fonctionner en mode `--dry-run`
- ✅ Afficher l'aide avec `--help`
- ✅ Tourner avec virtual threads

---

## 17. Release Process

### Règle : toujours mettre à jour le CHANGELOG avant de tagger

Le workflow GitHub Actions `release.yml` lit automatiquement `CHANGELOG.md` pour remplir le corps de la release. Sans entrée correspondante, un message de fallback générique est utilisé.

### Étapes à suivre pour chaque release

**1. Mettre à jour `CHANGELOG.md`**

Déplacer les changements de `[Unreleased]` vers une nouvelle section versionnée :

```markdown
## [Unreleased]       ← toujours laisser vide, prêt pour la suite

## [1.1.0] - 2026-07-15   ← nouvelle section

### Added
- ...

### Fixed
- ...
```

Catégories disponibles : `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.

**2. Commiter le CHANGELOG**

```bash
git add CHANGELOG.md
git commit -m "chore: prepare release v1.1.0"
```

**3. Créer et pousser le tag**

```bash
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

→ Le workflow `release.yml` se déclenche automatiquement, extrait la section `[1.1.0]` du CHANGELOG et crée la GitHub Release avec le JAR en asset.

**4. Alternative manuelle (depuis l'UI GitHub)**

`Actions → Release → Run workflow` → saisir le tag (qui doit déjà exister sur le repo).

### Convention de nommage des tags

- Format : `vMAJOR.MINOR.PATCH` (ex: `v1.0.0`, `v1.2.3`)
- Préfixe `v` obligatoire — le workflow filtre sur `v*`
- Suivre [Semantic Versioning](https://semver.org/) :
  - **MAJOR** : breaking change
  - **MINOR** : nouvelle fonctionnalité rétrocompatible
  - **PATCH** : bugfix

### Commandes utiles

```bash
git tag                          # lister les tags locaux
git tag -d v1.1.0                # supprimer un tag local (si erreur)
git push origin --delete v1.1.0  # supprimer un tag distant
git tag -l "v*"                  # lister uniquement les tags de version
```

---

*Ce document est la source de vérité pour l'implémentation. Tout écart doit être justifié.*
