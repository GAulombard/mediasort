// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Installs mediasort globally so it can be run from anywhere.
 * Copies the running JAR to ~/.mediasort/bin/ and generates a platform wrapper script.
 */
public class Installer {

    public void install() throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path jarSource = findCurrentJar();
        Path installDir = Path.of(System.getProperty("user.home"), ".mediasort", "bin");

        Files.createDirectories(installDir);

        Path targetJar = installDir.resolve("mediasort.jar");
        boolean isUpdate = Files.exists(targetJar);
        Files.copy(jarSource, targetJar, StandardCopyOption.REPLACE_EXISTING);
        System.out.printf("%s JAR: %s%n", isUpdate ? "Updated" : "Installed", targetJar);

        if (isWindows) {
            generateBatchWrapper(installDir);
        }
        // Always generate a shell wrapper: needed on Unix and for Git Bash on Windows
        generateShellWrapper(installDir);

        if (!tryAddToPath(installDir, isWindows)) {
            printPathInstructions(installDir, isWindows);
        }

        System.out.println("\nInstallation complete. You can now run: mediasort --help");
    }

    private Path findCurrentJar() throws IOException {
        try {
            var location = Installer.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Path.of(location.toURI());
            if (!path.toString().endsWith(".jar")) {
                throw new IOException(
                    "Not running from a JAR file. Build the JAR first: mvn package -DskipTests");
            }
            return path;
        } catch (URISyntaxException e) {
            throw new IOException("Cannot determine JAR location", e);
        }
    }

    private void generateBatchWrapper(Path installDir) throws IOException {
        Path script = installDir.resolve("mediasort.bat");
        Files.writeString(script, "@echo off\r\njava -jar \"%~dp0mediasort.jar\" %*\r\n");
        System.out.println("Generated: " + script);
    }

    private void generateShellWrapper(Path installDir) throws IOException {
        Path script = installDir.resolve("mediasort");
        Files.writeString(script,
            "#!/usr/bin/env bash\nexec java -jar \"$(dirname \"$0\")/mediasort.jar\" \"$@\"\n");
        script.toFile().setExecutable(true);
        System.out.println("Generated: " + script);
    }

    private boolean tryAddToPath(Path installDir, boolean isWindows) {
        boolean ok = false;
        if (isWindows) {
            try { ok = addToPathWindows(installDir); } catch (Exception ignored) {}
        }
        // Always update ~/.bashrc so Git Bash (and Unix) users can run mediasort
        try { ok = addToPathUnix() || ok; } catch (Exception ignored) {}
        return ok;
    }

    private boolean addToPathWindows(Path installDir) throws IOException, InterruptedException {
        String dir = installDir.toAbsolutePath().toString();

        // Read current user-level PATH from the registry
        Process query = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", "PATH")
                .start();
        String output = new String(query.getInputStream().readAllBytes());
        query.waitFor();

        String currentPath = "";
        for (String line : output.lines().toList()) {
            if (line.contains("PATH")) {
                // Line format: "    PATH    REG_EXPAND_SZ    <value>"
                int idx = line.lastIndexOf("REG_EXPAND_SZ");
                if (idx == -1) idx = line.lastIndexOf("REG_SZ");
                if (idx != -1) {
                    currentPath = line.substring(idx).replaceFirst("REG_(?:EXPAND_)?SZ\\s+", "").trim();
                }
            }
        }

        if (currentPath.contains(dir)) {
            System.out.println("PATH already contains: " + dir);
            return true;
        }

        String newPath = currentPath.isEmpty() ? dir : currentPath + ";" + dir;
        int exit = new ProcessBuilder(
            "reg", "add", "HKCU\\Environment", "/v", "PATH",
            "/t", "REG_EXPAND_SZ", "/d", newPath, "/f"
        ).start().waitFor();

        if (exit == 0) {
            System.out.println("Added to user PATH (restart terminal to apply): " + dir);
            return true;
        }
        return false;
    }

    private boolean addToPathUnix() throws IOException {
        // Use $HOME so the line works on Unix and in Git Bash on Windows
        String exportLine = "export PATH=\"$HOME/.mediasort/bin:$PATH\"";
        Path rcFile = Path.of(System.getProperty("user.home"), ".bashrc");

        if (Files.exists(rcFile) && Files.readString(rcFile).contains(".mediasort/bin")) {
            System.out.println("PATH already configured in ~/.bashrc");
            return true;
        }

        Files.writeString(rcFile, "\n# mediasort\n" + exportLine + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("Added to ~/.bashrc: " + exportLine);
        System.out.println("Run: source ~/.bashrc  (or open a new terminal)");
        return true;
    }

    private void printPathInstructions(Path installDir, boolean isWindows) {
        String dir = installDir.toAbsolutePath().toString();
        System.out.println("\nAdd this directory to your PATH manually:");
        if (isWindows) {
            System.out.printf("  CMD/PowerShell: setx PATH \"%%PATH%%;%s\"%n", dir);
            System.out.println("  Git Bash: echo 'export PATH=\"$HOME/.mediasort/bin:$PATH\"' >> ~/.bashrc && source ~/.bashrc");
        } else {
            System.out.println("  echo 'export PATH=\"$HOME/.mediasort/bin:$PATH\"' >> ~/.bashrc");
            System.out.println("  source ~/.bashrc");
        }
    }
}
