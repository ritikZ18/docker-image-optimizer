package com.imagesmith.optimizer.repository;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RepositoryInspectionService {
    private static final Pattern DOCKERFILE_NAME = Pattern.compile("Dockerfile(?:\\..*)?");
    private static final Pattern INSTRUCTION = Pattern.compile("^\\s*([A-Z][A-Z0-9_-]*)\\b.*");

    public Inspection inspect(RepositoryEntity repository) {
        Path checkout = null;
        try {
            checkout = Files.createTempDirectory("imagesmith-repository-");
            cloneRepository(repository.getUrl(), checkout);
            return inspectDockerfiles(checkout);
        } catch (IOException exception) {
            throw new RepositoryInspectionException("Repository could not be inspected: " + exception.getMessage(), exception);
        } finally {
            deleteRecursively(checkout);
        }
    }

    private void cloneRepository(String url, Path checkout) throws IOException {
        Process process = new ProcessBuilder("git", "clone", "--depth", "1", url, checkout.toString())
                .redirectErrorStream(true)
                .start();
        try {
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("git clone timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IOException(output.isBlank() ? "git clone failed" : output);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("git clone was interrupted", exception);
        }
    }

    private Inspection inspectDockerfiles(Path checkout) throws IOException {
        List<DockerfileReport> reports = new ArrayList<>();
        try (var paths = Files.walk(checkout)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                if (DOCKERFILE_NAME.matcher(path.getFileName().toString()).matches()) {
                    reports.add(readDockerfile(checkout, path));
                }
            }
        }
        return new Inspection(reports);
    }

    private DockerfileReport readDockerfile(Path checkout, Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<String> instructions = lines.stream()
                .map(INSTRUCTION::matcher)
                .filter(java.util.regex.Matcher::matches)
                .map(matcher -> matcher.group(1))
                .toList();
        return new DockerfileReport(checkout.relativize(path).toString(), lines.size(), instructions);
    }

    private void deleteRecursively(Path directory) {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public record Inspection(List<DockerfileReport> dockerfiles) {
        public boolean foundDockerfile() {
            return !dockerfiles.isEmpty();
        }
    }

    public record DockerfileReport(String path, int lineCount, List<String> instructions) {}

    public static class RepositoryInspectionException extends RuntimeException {
        public RepositoryInspectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}