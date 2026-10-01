package com.imagesmith.optimizer.ai;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeRetriever {
    private static final int MAX_RULES = 3;
    private final OptimizationRuleStore rules;

    public KnowledgeRetriever(OptimizationRuleStore rules) {
        this.rules = rules;
    }

    public List<Map<String, Object>> retrieve(String runtime, Set<String> findingCategories) {
        String normalizedRuntime = runtime == null ? "" : runtime.toLowerCase(Locale.ROOT);
        Set<String> normalizedCategories = new LinkedHashSet<>();
        findingCategories.forEach(category -> normalizedCategories.add(category.toLowerCase(Locale.ROOT)));
        return rules.findByActiveTrueOrderByRuleCodeAsc().stream()
                .filter(rule -> appliesToRuntime(rule, normalizedRuntime))
                .sorted((left, right) -> Integer.compare(score(right, normalizedCategories), score(left, normalizedCategories)))
                .limit(MAX_RULES)
                .map(this::toKnowledge)
                .toList();
    }

    private boolean appliesToRuntime(OptimizationRuleEntity rule, String runtime) {
        return "all".equalsIgnoreCase(rule.getLanguage())
                || runtime.isBlank()
                || runtime.contains(rule.getLanguage().toLowerCase(Locale.ROOT));
    }

    private int score(OptimizationRuleEntity rule, Set<String> categories) {
        return categories.stream().anyMatch(category -> rule.getCategory().toLowerCase(Locale.ROOT).contains(category)) ? 2 : 1;
    }

    private Map<String, Object> toKnowledge(OptimizationRuleEntity rule) {
        return Map.of(
                "id", rule.getRuleCode(),
                "category", rule.getCategory(),
                "language", rule.getLanguage(),
                "rule", rule.getDescription(),
                "applicability", rule.getApplicability(),
                "sourceUrl", rule.getSourceUrl(),
                "version", rule.getDocumentationVersion());
    }
}