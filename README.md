# mediasort

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> Java 21+ CLI tool to sort photos and videos by year and month.

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
# Verify installation
java -version   # should show 21+
mvn -version    # should show 3.x+
```

## Installation

```bash
git clone https://github.com/GAulombard/mediasort.git
cd mediasort
chmod +x run.sh   # Linux/Mac only
```

## Global installation (optional)

```bash
# Build the JAR
mvn clean package

# Install mediasort into ~/.mediasort/bin and add to PATH automatically
java -jar target/mediasort.jar --install

# Then from anywhere:
mediasort /photos --dry-run
```

## Usage

```bash
# 1. Simulate first (recommended) — default destination: /photos/mediasort/
./run.sh /path/to/source --dry-run

# 2. Real copy with explicit destination
./run.sh /path/to/source /path/to/destination

# 3. Move (deletes originals)
./run.sh /path/to/source /path/to/destination --move

# 4. Sort by year only (no month subdirectory)
./run.sh /path/to/source /path/to/destination --no-month

# 5. With more threads and detailed logs
./run.sh /path/to/source /path/to/destination --verbose --threads=8

# 6. Exclude screenshots (case-insensitive)
./run.sh /path/to/source --exclude-pattern screenshot

# 7. Multiple exclusion patterns
./run.sh /path/to/source --exclude-pattern screenshot --exclude-pattern IMG_BURST

# 8. Delete excluded files from source
./run.sh /path/to/source --exclude-pattern screenshot --delete-excluded
```

Windows:

```bat
run.bat "C:\Photos" --dry-run
run.bat "C:\Photos" "D:\Backup" --move --verbose
run.bat "C:\Photos" --exclude-pattern screenshot --delete-excluded
```

## Output structure

```
destination/
├── 2019/
│   ├── 03_march/
│   └── 08_august/
├── 2023/
│   └── 12_december/
└── unknown/    ← files with no detectable date
```

## Before / After

**Before:**
```
photos/
├── IMG_20191205_183426.jpg
├── IMG_20200614_092341.jpg
└── VID_20210301_120000.mp4
```

**After:**
```
photos/mediasort/
├── 2019/12_december/IMG_20191205_183426.jpg
├── 2020/06_june/IMG_20200614_092341.jpg
└── 2021/03_march/VID_20210301_120000.mp4
```

## Date sources (priority order)

1. **Google Takeout JSON** — sidecar `.json` file with the same name
2. **EXIF** — embedded metadata via `metadata-extractor`
3. **Filename** — patterns `IMG_YYYYMMDD`, `VID-YYYYMMDD`, `YYYYMMDD_HHmmss`, `Screenshot_YYYY-MM-DD`
4. **Modification date** — fallback (unreliable, reported as warning)

## Supported formats

| Type    | Extensions                                        |
|---------|---------------------------------------------------|
| Images  | JPEG, JPG, PNG, GIF, BMP, TIFF, WEBP, HEIC, HEIF |
| Videos  | MP4, MOV, AVI, MKV, 3GP                          |
| Sidecar | JSON (Google Takeout)                             |

## All options

| Option | Description | Default |
|--------|-------------|---------|
| `<source>` | Source directory (required) | — |
| `<destination>` | Destination directory | `<source>/mediasort/` |
| `--move` | Move instead of copy | false |
| `--no-month` | Sort by year only | false |
| `--dry-run` | Simulate, no files written | false |
| `--threads=N` | Number of virtual threads | 4 |
| `--verbose` | Detailed logs | false |
| `--exclude-pattern <pat>` | Skip files whose name contains `<pat>` (repeatable, case-insensitive) | — |
| `--delete-excluded` | Delete excluded files from source | false |
| `--install` | Install mediasort globally (`~/.mediasort/bin`) | — |
| `--rebuild` | Force JAR recompilation | false |
| `--help` | Show help | — |

## Roadmap

- [x] Google Takeout JSON sidecar support
- [ ] Duplicate detection
- [x] Dry-run mode
- [x] Global install (`~/.mediasort/bin`)
- [x] Default destination to `<source>/mediasort/` when not specified
- [ ] GUI (maybe, one day)

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR. All PRs must target the `develop` branch.

## License

MIT © 2026 Hodor — see [LICENSE](LICENSE) for the full text.
