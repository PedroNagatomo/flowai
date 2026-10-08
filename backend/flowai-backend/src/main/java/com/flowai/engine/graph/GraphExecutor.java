package com.flowai.engine.graph;

import com.flowai.engine.*;
import com.flowai.entity.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GraphExecutor {

    private final Map<ActionType, ActionExecutor> actionExecutors;
    private final ConditionEvaluator conditionEval;

    public GraphExecutor(
            List<ActionExecutor> executors,
            ConditionEvaluator conditionEval
    ) {
        this.actionExecutors = executors.stream()
                .collect(Collectors.toMap(ActionExecutor::supportedType, Function.identity()));
        this.conditionEval = conditionEval;
    }

    public List<NodeResult> execute(GraphDefinition graph, Workflow wf, ExecutionContext ctx) {
        List<NodeResult> results = new ArrayList<>();

        WorkflowNode entry = graph.findEntryNode();
        if (entry == null) {
            log.warn("⚠️ Grafo sem nó inicial");
            return results;
        }

        visit(entry, graph, ctx, results, new HashSet<>());
        return results;
    }

    private void visit(
            WorkflowNode node,
            GraphDefinition graph,
            ExecutionContext ctx,
            List<NodeResult> results,
            Set<String> visited
    ) {
        if (node == null) return;
        if (visited.contains(node.getId())) {
            log.warn("⚠️ Loop detectado no nó {}, abortando", node.getId());
            return;
        }
        visited.add(node.getId());

        log.debug("🔷 Executando nó {} ({})", node.getId(), node.getType());

        if ("TRIGGER".equals(node.getType())) {
            results.add(new NodeResult(node.getId(), "TRIGGER", true, "trigger"));
            for (String nextId : node.getNextList()) {
                visit(graph.findNode(nextId), graph, ctx, results, visited);
            }
            return;
        }

        if ("IF".equals(node.getType())) {
            executeIfNode(node, graph, ctx, results, visited);
            return;
        }

        ActionResult result = executeActionNode(node, ctx);
        results.add(new NodeResult(node.getId(), node.getType(), result.success(), result.message()));

        if (!result.success()) {
            log.warn("⚠️ Nó {} falhou, parando essa branch", node.getId());
            return;
        }

        for (String nextId : node.getNextList()) {
            visit(graph.findNode(nextId), graph, ctx, results, visited);
        }
    }

    @SuppressWarnings("unchecked")
    private void executeIfNode(
            WorkflowNode node,
            GraphDefinition graph,
            ExecutionContext ctx,
            List<NodeResult> results,
            Set<String> visited
    ) {
        var config = node.getConfigOrEmpty();
        var condition = (Map<String, Object>) config.get("condition");

        boolean conditionMet;
        if (condition == null) {
            log.warn("⚠️ IF {} sem condição, indo pro 'else'", node.getId());
            conditionMet = false;
        } else {
            conditionMet = conditionEval.evaluate(List.of(condition), ctx);
        }

        results.add(new NodeResult(node.getId(), "IF", true,
                "condição " + (conditionMet ? "verdadeira" : "falsa")));

        List<String> branch = conditionMet ? node.getThenBranches() : node.getElseBranches();
        for (String nextId : branch) {
            visit(graph.findNode(nextId), graph, ctx, results, visited);
        }
    }

    private ActionResult executeActionNode(WorkflowNode node, ExecutionContext ctx) {
        ActionType type;
        try {
            type = ActionType.from(node.getType());
        } catch (IllegalArgumentException e) {
            return ActionResult.failure("Tipo de nó desconhecido: " + node.getType());
        }

        ActionExecutor executor = actionExecutors.get(type);
        if (executor == null) {
            return ActionResult.failure("Executor não encontrado para: " + type);
        }

        Map<String, Object> config = new HashMap<>(node.getConfigOrEmpty());
        config.putIfAbsent("outputVariable", node.getId());

        try {
            return executor.execute(new ActionConfig(type, config), ctx);
        } catch (Exception e) {
            log.error("❌ Erro executando nó {}", node.getId(), e);
            return ActionResult.failure(e.getMessage());
        }
    }
}