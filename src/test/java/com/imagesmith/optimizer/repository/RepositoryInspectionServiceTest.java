package com.imagesmith.optimizer.repository;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryInspectionServiceTest {
    @Test
    void reportsDockerfileInstructions() throws Exception {
        Path repository = Files.createTempDirectory("imagesmith-test-repository-");
        try {
            Files.writeString(repository.resolve("Dockerfile"), "FROM eclipse-temurin:21\nCOPY . /app\nENTRYPOINT [\\\"java\\\"]\n");
            runGit(repository, "init");
            runGit(repository, "add", "Dockerfile");
            runGit(repository, "-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "fixture");

            var service = new RepositoryInspectionService();
            var inspection = service.inspect(new RepositoryEntity("test", repository.toUri().toString()), "fixture-session");

            assertThat(inspection.foundDockerfile()).isTrue();
            assertThat(inspection.dockerfiles()).singleElement().satisfies(report -> {
                assertThat(report.path()).isEqualTo("Dockerfile");
                assertThat(report.instructions()).containsExactly("FROM", "COPY", "ENTRYPOINT");
                assertThat(report.baseImages()).containsExactly("eclipse-temurin:21");
            });
        } finally {
            try (var paths = Files.walk(repository)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    private void runGit(Path repository, String... arguments) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(java.util.List.of(arguments));
        Process process = new ProcessBuilder(command).directory(repository.toFile()).inheritIO().start();
        assertThat(process.waitFor()).isZero();
    }
}