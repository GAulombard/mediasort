# Contributing to mediasort

First off, thank you for considering contributing! Every bit helps, whether it's a bug fix, a new feature, or just improving the docs.

---

## Prerequisites

- **Java 21** — [install via SDKMAN](https://sdkman.io/) (Linux/Mac) or `winget install Microsoft.OpenJDK.21` (Windows)
- **Maven 3.x** — `sdk install maven` or `winget install Apache.Maven`

---

## Fork & clone

```bash
git clone https://github.com/<your-username>/mediasort.git
cd mediasort
```

---

## Branch naming

Always branch off `develop`:

```bash
git checkout develop
git checkout -b feature/my-new-feature   # new feature
git checkout -b fix/bug-description      # bug fix
git checkout -b docs/improve-readme      # documentation
```

Patterns:
- `feature/xxx` — new functionality
- `fix/xxx` — bug fixes
- `docs/xxx` — documentation only

---

## Build locally

```bash
mvn clean package
```

The output JAR will be at `target/mediasort.jar`.

---

## Run locally

```bash
# Simulate (recommended first step)
java -jar target/mediasort.jar /path/to/photos --dry-run

# With verbose output
java -jar target/mediasort.jar /path/to/photos /path/to/destination --verbose

# Windows
java -jar target\mediasort.jar "C:\Photos" "D:\Sorted" --dry-run
```

---

## Pull request rules

- **Target branch is `develop`** — never open a PR directly to `master`
- One feature or fix per PR — keep the scope focused
- Describe **what** and **why** in the PR description (not just what the code does)
- CI must pass before merge — the build runs automatically on every PR

---

## Code style

Standard Java conventions apply. No specific formatter is enforced for now, just keep it readable:

- Classes: `PascalCase`
- Methods and variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Comments in English

---

## Questions?

Open a [GitHub issue](https://github.com/GAulombard/mediasort/issues) — no question is too small.
