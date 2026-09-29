package com.imagesmith.optimizer.repository;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RepositoryInspectionService {
    private static final Pattern DOCKERFILE_NAME = Pattern.compile("Dockerfile(?:\\..*)?");
    private static final Pattern INSTRUCTION = Pattern.compile("^\\s*([A-Z][A-Z0-9_-]*)\\b.*");
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(".git", "node_modules", "target", "build", "dist", ".next", "coverage", "vendor");
    private static final int MAX_TREE_NODES = 260;

    public Inspection inspect(RepositoryEntity repository, String sessionName) {
        Path checkout = null;
        try {
            checkout = Files.createTempDirectory("imagesmith-repository-");
            cloneRepository(repository.getUrl(), checkout);
            return inspectRepository(checkout, repository, sessionName);
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

    private Inspection inspectRepository(Path checkout, RepositoryEntity repository, String sessionName) throws IOException {
        List<DockerfileReport> reports = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        Set<Path> directories = new LinkedHashSet<>();
        try (var paths = Files.walk(checkout)) {
            for (Path path : paths.filter(path -> !isIgnored(path, checkout)).sorted().toList()) {
                if (Files.isDirectory(path)) {
                    directories.add(path);
                } else if (Files.isRegularFile(path)) {
                    files.add(path);
                    if (DOCKERFILE_NAME.matcher(path.getFileName().toString()).matches()) {
                        reports.add(readDockerfile(checkout, path));
                    }
                }
            }
        }
        RepositorySummary summary = summarize(files, directories, checkout);
        return new Inspection(
                sessionName,
                repository.getName(),
                repository.getUrl(),
                summary,
                categories(files, reports, checkout),
                reports,
                dockerAssets(files, reports, checkout),
                buildTree(checkout, files));
    }

    private boolean isIgnored(Path path, Path checkout) {
        Path relative = checkout.relativize(path);
        for (Path part : relative) {
            if (IGNORED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private RepositorySummary summarize(List<Path> files, Set<Path> directories, Path checkout) throws IOException {
        long lineCount = 0;
        for (Path file : files) {
            lineCount += countLines(file);
        }
        return new RepositorySummary(files.size(), lineCount, Math.max(0, directories.size() - 1));
    }

    private long countLines(Path path) throws IOException {
        if (Files.size(path) > 2_000_000 || isBinary(path)) {
            return 0;
        }
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.count();
        } catch (java.nio.charset.MalformedInputException exception) {
            return 0;
        }
    }

    private boolean isBinary(Path path) throws IOException {
        byte[] sample = Files.readAllBytes(path);
        int length = Math.min(sample.length, 4096);
        for (int index = 0; index < length; index++) {
            if (sample[index] == 0) {
                return true;
            }
        }
        return false;
    }

    private List<CategoryReport> categories(List<Path> files, List<DockerfileReport> dockerfiles, Path checkout) {
        Map<String, Set<String>> detected = new LinkedHashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            String relative = checkout.relativize(file).toString().replace('\\', '/');
            if (name.equals("package.json") || name.equals("next.config.js") || name.equals("next.config.ts")) {
                add(detected, "Frontend", name.equals("package.json") ? "Node.js" : "Next.js");
            }
            if (name.equals("pom.xml") || name.equals("build.gradle") || name.equals("build.gradle.kts")) {
                add(detected, "Backend", name.equals("pom.xml") ? "Java / Maven" : "Java / Gradle");
            }
            if (name.equals("requirements.txt") || name.equals("pyproject.toml") || name.equals("manage.py")) {
                add(detected, "Backend", "Python");
            }
            if (name.equals("go.mod")) add(detected, "Backend", "Go");
            if (name.equals("Cargo.toml")) add(detected, "Backend", "Rust");
            if (name.equals("terraform.tf") || name.endsWith(".tf")) add(detected, "Infrastructure", "Terraform");
            if (name.equals("docker-compose.yml") || name.equals("docker-compose.yaml")) add(detected, "Infrastructure", "Docker Compose");
            if (relative.startsWith("frontend/") || relative.startsWith("web/")) add(detected, "Frontend", "Web application");
            if (relative.startsWith("services/") || relative.startsWith("server/") || relative.startsWith("backend/")) add(detected, "Backend", "Service layer");
        }
        if (!dockerfiles.isEmpty()) add(detected, "Containers", "Dockerfile");
        return detected.entrySet().stream().map(entry -> new CategoryReport(entry.getKey(), List.copyOf(entry.getValue()))).toList();
    }

    private void add(Map<String, Set<String>> detected, String category, String detail) {
        detected.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(detail);
    }

    private List<String> dockerAssets(List<Path> files, List<DockerfileReport> dockerfiles, Path checkout) {
        Set<String> assets = new LinkedHashSet<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            if (DOCKERFILE_NAME.matcher(name).matches() || name.equals(".dockerignore") || name.startsWith("docker-compose")) {
                assets.add(checkout.relativize(file).toString().replace('\\', '/'));
            }
        }
        dockerfiles.stream().flatMap(report -> report.baseImages().stream()).map(image -> "base image: " + image).forEach(assets::add);
        return List.copyOf(assets);
    }

    private TreeNode buildTree(Path checkout, List<Path> files) {
        TreeBuilder root = new TreeBuilder(checkout.getFileName().toString(), "directory");
        int[] count = {1};
        for (Path file : files) {
            Path relative = checkout.relativize(file);
            TreeBuilder current = root;
            Path currentPath = checkout;
            for (Path part : relative) {
                if (count[0] >= MAX_TREE_NODES) break;
                currentPath = currentPath.resolve(part);
                current = current.child(part.toString(), Files.isDirectory(currentPath) ? "directory" : "file");
                count[0]++;
            }
            if (count[0] >= MAX_TREE_NODES) break;
        }
        return root.toRecord();
    }

    private static class TreeBuilder {
        private final String name;
        private final String type;
        private final Map<String, TreeBuilder> children = new LinkedHashMap<>();

        private TreeBuilder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        private TreeBuilder child(String childName, String childType) {
            return children.computeIfAbsent(childName, ignored -> new TreeBuilder(childName, childType));
        }

        private TreeNode toRecord() {
            return new TreeNode(name, type, children.values().stream().map(TreeBuilder::toRecord).toList());
        }
    }

    private DockerfileReport readDockerfile(Path checkout, Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<String> instructions = lines.stream()
                .map(INSTRUCTION::matcher)
                .filter(java.util.regex.Matcher::matches)
                .map(matcher -> matcher.group(1))
                .toList();
        List<String> baseImages = lines.stream()
                .filter(line -> line.trim().toUpperCase().startsWith("FROM "))
                .map(line -> line.trim().substring(5).split("\\s+AS\\s+", 2)[0])
                .toList();
        return new DockerfileReport(checkout.relativize(path).toString(), lines.size(), instructions, baseImages);
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

    public record Inspection(String sessionName, String repositoryName, String repositoryUrl,
                              RepositorySummary summary, List<CategoryReport> categories,
                              List<DockerfileReport> dockerfiles, List<String> dockerAssets, TreeNode tree) {
        public boolean foundDockerfile() {
            return !dockerfiles.isEmpty();
        }
    }

    public record RepositorySummary(int fileCount, long lineCount, int directoryCount) {}
    public record CategoryReport(String name, List<String> details) {}
    public record DockerfileReport(String path, int lineCount, List<String> instructions, List<String> baseImages) {}
    public record TreeNode(String name, String type, List<TreeNode> children) {}

    public static class RepositoryInspectionException extends RuntimeException {
        public RepositoryInspectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}