package com.imagesmith.optimizer.ai;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OptimizationRuleInitializer implements CommandLineRunner {
    private final OptimizationRuleStore rules;

    public OptimizationRuleInitializer(OptimizationRuleStore rules) {
        this.rules = rules;
    }

    @Override
    public void run(String... args) {
        if (rules.count() > 0) {
            return;
        }
        rules.saveAll(List.of(
                rule("DOCKER-012", "runtime-size", "all", "Separate build dependencies from the runtime image with a multi-stage build.", "Dockerfile contains build tooling or package managers in the final stage.", "https://docs.docker.com/build/building/multi-stage/"),
                rule("SECURITY-006", "security", "all", "Run the application as a non-root user in the final image.", "The final image does not establish a non-root USER.", "https://docs.docker.com/reference/dockerfile/#user"),
                rule("JAVA-004", "runtime-size", "java", "Use a JRE or a validated jlink runtime instead of shipping a full JDK.", "A Java runtime image contains build-only JDK tooling.", "https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html"),
                rule("PYTHON-003", "dependencies", "python", "Build native wheels separately and keep compilers out of the runtime image.", "Python dependency installation requires native build tooling.", "https://packaging.python.org/en/latest/guides/packaging-binary-extensions/"),
                rule("NODE-002", "dependencies", "node", "Install production dependencies separately from development dependencies.", "The final Node image contains devDependencies or package-manager caches.", "https://docs.npmjs.com/cli/v10/commands/npm-ci")
        ));
    }

    private OptimizationRuleEntity rule(String code, String category, String language, String description,
                                         String applicability, String sourceUrl) {
        return new OptimizationRuleEntity(code, category, language, description, applicability, sourceUrl, "v1");
    }
}