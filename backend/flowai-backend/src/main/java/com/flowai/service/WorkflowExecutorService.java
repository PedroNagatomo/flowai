package com.flowai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowai.engine.*;
import com.flowai.engine.graph.GraphDefinition;
import com.flowai.engine.graph.GraphExecutor;
import com.flowai.engine.graph.NodeResult;
import com.flowai.entity.Workflow;
import com.flowai.entity.WorkflowExecution;
import com.flowai.repository.WorkflowExecutionRepository;
import com.flowai.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkflowExecutorService {

    private final WorkflowRepository workflowRepo;
    private final WorkflowExecutionRepository execRepo;
    private final ConditionEvaluator conditionEval;
    private final GraphExecutor graphExecutor;
    private final ObjectMapper mapper;
    private final Map<ActionType, ActionExecutor> actionExecutors;

    public WorkflowExecutorService(
            WorkflowRepository workflowRepo,
            WorkflowExecutionRepository execRepo,
            ConditionEvaluator conditionEval,
            GraphExecutor graphExecutor,
            ObjectMapper mapper,
            List<ActionExecutor> executors
    ) {
        this.workflowRepo = workflowRepo;
        this.execRepo = execRepo;
        this.conditionEval = conditionEval;
        this.graphExecutor = graphExecutor;
        this.mapper = mapper;
        this.actionExecutors = executors.stream()
                .collect(Collectors.toMap(ActionExecutor::supportedType, Function.identity()));
        log.info("✅ Executores carregados: {}", actionExecutors.keySet());
    }

    @Async
    public void executeAsync(UUID workflowId, Map<String, Object> payload) {
        try {
            Workflow wf = workflowRepo.findById(workflowId).orElse(null);
            if (wf == null) {
                log.warn("⚠️ Workflow não encontrado: {}", workflowId);
                return;
            }
            execute(wf, payload);
        } catch (Exception e) {
            log.error("❌ Erro na execução assíncrona", e);
        }
    }

    @Transactional
    public WorkflowExecution execute(Workflow wf, Map<String, Object> payload) {
        var exec = WorkflowExecution.builder()
                .workflowId(wf.getId())
                .status("PENDING")
                .triggerPayload(payload)
                .build();
        execRepo.save(exec);

        long start = System.currentTimeMillis();

        try {
            var ctx = new ExecutionContext(wf, payload);
            var def = wf.getDefinition();

            // Detecta se é grafo ou lista antiga
            boolean isGraph = def.containsKey("graph") && def.get("graph") != null;

            Map<String, Object> logMap;
            if (isGraph) {
                logMap = executeGraph(def, wf, ctx);
            } else {
                logMap = executeLinear(def, ctx);
            }

            exec.setStatus("SUCCESS");
            exec.setExecutionLog(logMap);
            log.info("✅ Workflow {} executado em {}ms", wf.getId(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("❌ Workflow {} falhou: {}", wf.getId(), e.getMessage());
            exec.setStatus("FAILED");
            exec.setErrorMessage(e.getMessage());
        }

        return finish(exec, start);
    }

    // ============================================
    // ENGINE DE GRAFO
    // ============================================
    private Map<String, Object> executeGraph(Map<String, Object> def, Workflow wf, ExecutionContext ctx) {
        GraphDefinition graph;
        try {
            graph = mapper.convertValue(def.get("graph"), GraphDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Grafo inválido: " + e.getMessage());
        }

        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Grafo sem nós");
        }

        List<NodeResult> results = graphExecutor.execute(graph, wf, ctx);

        boolean anyFailure = results.stream().anyMatch(r -> !r.success());
        if (anyFailure) {
            throw new RuntimeException("Uma ou mais ações falharam. Verifique o log.");
        }

        List<Map<String, Object>> steps = results.stream()
                .map(r -> Map.<String, Object>of(
                        "nodeId", r.nodeId(),
                        "type", r.type(),
                        "success", r.success(),
                        "message", r.message() == null ? "" : r.message()
                ))
                .toList();

        return Map.of("engine", "graph", "steps", steps);
    }

    // ============================================
    // ENGINE LINEAR (compatibilidade)
    // ============================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeLinear(Map<String, Object> def, ExecutionContext ctx) {
        var logs = new ArrayList<Map<String, Object>>();

        // 1. Condições no nível raiz
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) def.get("conditions");
        if (!conditionEval.evaluate(conditions, ctx)) {
            logs.add(Map.of("step", "conditions", "result", "não atendidas, workflow pulado"));
            return Map.of("engine", "linear", "steps", logs);
        }
        logs.add(Map.of("step", "conditions", "result", "OK"));

        // 2. Ações em sequência
        List<Map<String, Object>> actions = (List<Map<String, Object>>) def.get("actions");
        if (actions == null || actions.isEmpty()) {
            throw new IllegalStateException("Workflow sem ações");
        }

        for (int i = 0; i < actions.size(); i++) {
            Map<String, Object> actionMap = actions.get(i);
            String typeStr = (String) actionMap.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) actionMap.getOrDefault("config", Map.of());

            // Se tem "id", injeta no config como outputVariable padrão
            String actionId = (String) actionMap.get("id");
            if (actionId != null) {
                config = new HashMap<>(config);
                config.putIfAbsent("outputVariable", actionId);
            }

            ActionType type = ActionType.from(typeStr);
            ActionExecutor executor = actionExecutors.get(type);
            if (executor == null) {
                throw new IllegalStateException("Executor não encontrado: " + type);
            }

            ActionResult result = executor.execute(new ActionConfig(type, config), ctx);

            logs.add(Map.of(
                    "step", actionId != null ? "node:" + actionId : "action:" + i + ":" + type,
                    "type", type.name(),
                    "success", result.success(),
                    "message", result.message() == null ? "" : result.message()
            ));

            if (!result.success()) {
                throw new RuntimeException("Ação " + type + " falhou: " + result.message());
            }
        }

        return Map.of("engine", "linear", "steps", logs);
    }

    private WorkflowExecution finish(WorkflowExecution exec, long start) {
        exec.setFinishedAt(LocalDateTime.now());
        exec.setDurationMs((int) (System.currentTimeMillis() - start));
        return execRepo.save(exec);
    }
}