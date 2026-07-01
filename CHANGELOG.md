# Changelog

All notable changes to this project will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Fixed
- Filename date pattern matching missed dash-separated WhatsApp names (`IMG-YYYYMMDD-WAxxxx`) and generic `YYYY-MM-DD` names (`WhatsApp Image 2024-02-27 à ...`), causing those files to be sorted into `unknown/` instead of their real date
- `IMG_`/`VID_` filename patterns now also accept a dash separator (`IMG-`, `VID-`)

### Added
- Generic `YYYY-MM-DD` filename fallback pattern used when no more specific pattern matches
- Filename pattern support for Facebook (`FB_IMG_<epoch-ms>`) and Snapchat (`Snapchat-<epoch-ms>`) exports

## [0.0.2] - 2026-06-29

### Added
- `--install` flag: copies JAR to `~/.mediasort/bin/` and generates platform wrapper scripts (`mediasort.bat` for CMD/PowerShell, `mediasort` shell script for Git Bash/Unix); auto-adds directory to PATH via Windows registry and `~/.bashrc`
- Optional `<destination>` argument: defaults to `<source>/mediasort/` when omitted; resolved destination is always printed at startup
- `--exclude-pattern <pat>` flag (repeatable, case-insensitive): skips files whose name contains the pattern; excluded files are tracked separately in statistics
- `--delete-excluded` flag: permanently deletes excluded source files instead of skipping them; dry-run safe
- ASCII progress bar printed to stderr during processing (`[===============>    ] 42/100 (42%)`); hidden in `--verbose` mode

### Fixed
- `--install` on Windows now also generates a Unix shell wrapper (`mediasort`) so Git Bash users can invoke the command directly
- `--install` now updates `~/.bashrc` with `$HOME/.mediasort/bin` (portable path) even on Windows, enabling Git Bash PATH resolution without a registry-dependent session restart
- ASCII progress bar now correctly updates in place on Windows terminals (CMD, PowerShell, Git Bash)
---

## [0.0.1] - 2026-06-28

### Added
- CLI tool to recursively sort photos and videos into `YYYY/MM_month/` directories
- 4 date extraction strategies in priority order:
  1. Google Takeout JSON sidecar (`.json` file next to the media)
  2. EXIF metadata via `metadata-extractor 2.19.0`
  3. Filename patterns: `IMG_YYYYMMDD`, `VID-YYYYMMDD`, `YYYYMMDD_HHmmss`, `Screenshot_YYYY-MM-DD`
  4. File modification date (fallback, flagged with `[WARN]`)
- Supported image formats: JPG, JPEG, PNG, GIF, BMP, WebP, HEIC, HEIF, TIFF, TIF, RAW, CR2, CR3, NEF, ARW, DNG, ORF, RW2
- Supported video formats: MP4, MOV, AVI, MKV, WMV, M4V, 3GP, MTS, M2TS
- CLI options: `--move`, `--no-month`, `--dry-run`, `--threads=N`, `--verbose`, `--rebuild`, `--help`
- Duplicate file handling with `_001`, `_002` suffixes
- Files with no detectable date sorted into `unknown/`
- Parallel processing via Java 21 virtual threads (`Thread.ofVirtual()`)
- Error log file written to destination on failure (`mediasort-errors.log`)
- Statistics summary printed at end of execution
- Fat JAR build via `maven-shade-plugin`
- Launch scripts: `run.sh` (Linux/Mac) and `run.bat` (Windows) — require Java 21+
- MIT license (SPDX headers on all source files)
- GitHub Actions: CI workflow (build + test on every push) and Release workflow
