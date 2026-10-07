package com.flowai.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ConditionEvaluator {

    /**
     * Avalia uma lista de condições (AND lógico entre todas).
     * Se a lista é vazia/null, retorna true (sem condições = sempre executa).
     */
    @SuppressWarnings("unchecked")
    public boolean evaluate(List<Map<String, Object>> conditions, ExecutionContext ctx) {
        if (conditions == null || conditions.isEmpty()) return true;

        Map<String, Object> root = ctx.toMap();
        for (var condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");

            Object actual = resolveField(field, root);
            boolean result = evaluateOne(operator, actual, value);

            log.debug("🔍 Condição {} {} {} → atual={} resultado={}",
                    field, operator, value, actual, result);

            if (!result) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Object resolveField(String path, Map<String, Object> root) {
        if (path == null) return null;
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    private boolean evaluateOne(String operator, Object actual, Object expected) {
        if (operator == null) return true;

        return switch (operator) {
            case "EQUALS" -> Objects.equals(stringify(actual), stringify(expected));
            case "NOT_EQUALS" -> !Objects.equals(stringify(actual), stringify(expected));
            case "CONTAINS" -> stringify(actual).contains(stringify(expected));
            case "NOT_CONTAINS" -> !stringify(actual).contains(stringify(expected));
            case "STARTS_WITH" -> stringify(actual).startsWith(stringify(expected));
            case "ENDS_WITH" -> stringify(actual).endsWith(stringify(expected));
            case "GREATER_THAN" -> compareNumeric(actual, expected) > 0;
            case "LESS_THAN" -> compareNumeric(actual, expected) < 0;
            case "IS_EMPTY" -> actual == null || stringify(actual).isEmpty();
            case "IS_NOT_EMPTY" -> actual != null && !stringify(actual).isEmpty();
            default -> {
                log.warn("⚠️ Operador desconhecido: {}", operator);
                yield true;
            }
        };
    }

    private int compareNumeric(Object a, Object b) {
        try {
            double da = Double.parseDouble(stringify(a));
            double db = Double.parseDouble(stringify(b));
            return Double.compare(da, db);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String stringify(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}