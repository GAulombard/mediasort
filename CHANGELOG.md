# Changelog

All notable changes to this project will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

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
