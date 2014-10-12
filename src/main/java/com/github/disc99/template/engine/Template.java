package com.github.disc99.template.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.disc99.template.util.Beans;
import com.github.disc99.template.util.Strings;

/**
 * Minimal template engine
 */
public class Template {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");

    private static final Pattern EACH_EXPRESSION_PATTERN = Pattern.compile("\\{\\{# (.*?)\\}\\}(.*?)\\{\\{/\\}\\}", Pattern.DOTALL);

    private static final String FUNCTION_SEPARATOR = "\\|";

    private static final Map<String, Function<String, String>> FUNCTIONS;
    static {
        FUNCTIONS = new HashMap<>();
        FUNCTIONS.put("capitalize", (value) -> Strings.capitalize(value));
        FUNCTIONS.put("uncapitalize", (value) -> Strings.uncapitalize(value));
    }

	private static final Function<String, Function<String, String>> FUNCTION_RESOLVER
		= (functionName) -> FUNCTIONS.get(functionName.trim());

    private final String source;

    public Template(String source) {
        this.source = source;
    }

    public String render(Object model) {
        return new Context(model).render(source);
    }

    private static class Context {

        final Object model;

        Context(Object model) {
            this.model = model;
        }

        String render(String source) {
            Matcher matcher = EACH_EXPRESSION_PATTERN.matcher(source);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String variableName = matcher.group(1);
                String body = matcher.group(2);
                matcher.appendReplacement(buffer, expand(variableName, body));
            }
            matcher.appendTail(buffer);
            return renderExpressions(buffer.toString());
        }

        @SuppressWarnings("rawtypes")
        private String expand(String variableName, String body) {
            Object property = Beans.getProperty(model, variableName);
            if (property == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            if (Iterable.class.isAssignableFrom(property.getClass())) {
                for (Object o : (Iterable) property) {
                    builder.append(new Context(o).render(body));
                }
            }
            return builder.toString();
        }

        private String renderExpressions(String source) {
            Matcher matcher = EXPRESSION_PATTERN.matcher(source);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String expression = matcher.group(1);
                matcher.appendReplacement(buffer, evaluate(expression));
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }

        private String evaluate(String expression) {
            return parse(expression).evaluate(model);
        }

        private Expression parse(String expression) {

            if (Strings.isNullOrEmpty(expression)) {
                return Expression.EMPTY;
            }

            List<String> tokens =Arrays.asList(expression.split(FUNCTION_SEPARATOR));
            if (tokens.isEmpty()) {
                return Expression.EMPTY;
            }

            String variableName = tokens.get(0).trim();
            List<Function<String, String>> functions = Collections.emptyList();
            if (tokens.size() > 1) {
                List<String> functionNames = tokens.subList(1, tokens.size());
                functions = functionNames.stream()
                		.map(FUNCTION_RESOLVER)
                		.filter(Objects::nonNull)
                		.collect(Collectors.toList());
            }

            return new SimpleExpression(variableName, functions);

        }

    }

    @FunctionalInterface
    private static interface Expression {

        Expression EMPTY = model -> "";

        String evaluate(Object model);

    }

    private static class SimpleExpression implements Expression {

        final String variableName;

        final List<Function<String, String>> functions;

        SimpleExpression(String variableName, List<Function<String, String>> functions) {
            this.variableName = variableName;
            this.functions = functions;
        }

        /** {@inheritDoc} */
        @Override
        public String evaluate(Object model) {
            String variable = Objects.toString(Beans.getProperty(model, variableName), "");
            if (functions.isEmpty()) {
                return variable;
            }
            
            for (Function<String, String> function : functions) {
                variable = function.apply(variable);
            }
            return variable;
        }

    }

}