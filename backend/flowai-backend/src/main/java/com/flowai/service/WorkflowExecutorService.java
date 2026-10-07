package com.flowai.service;

import com.flowai.engine.*;
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
    private final Map<ActionType, ActionExecutor> actionExecutors;

    public WorkflowExecutorService(
            WorkflowRepository workflowRepo,
            WorkflowExecutionRepository execRepo,
            ConditionEvaluator conditionEval,
            List<ActionExecutor> executors
    ) {
        this.workflowRepo = workflowRepo;
        this.execRepo = execRepo;
        this.conditionEval = conditionEval;
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
        var logs = new ArrayList<Map<String, Object>>();

        try {
            var ctx = new ExecutionContext(wf, payload);
            var def = wf.getDefinition();

            // 1. Condições
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) def.get("conditions");
            if (!conditionEval.evaluate(conditions, ctx)) {
                logs.add(Map.of("step", "conditions", "result", "não atendidas, workflow pulado"));
                exec.setStatus("SUCCESS");
                exec.setExecutionLog(Map.of("steps", logs));
                return finish(exec, start);
            }
            logs.add(Map.of("step", "conditions", "result", "OK"));

            // 2. Ações
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) def.get("actions");
            if (actions == null || actions.isEmpty()) {
                throw new IllegalStateException("Workflow sem ações");
            }

            for (Map<String, Object> actionMap : actions) {
                String typeStr = (String) actionMap.get("type");
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) actionMap.getOrDefault("config", Map.of());

                ActionType type = ActionType.from(typeStr);
                ActionExecutor executor = actionExecutors.get(type);
                if (executor == null) {
                    throw new IllegalStateException("Executor não encontrado: " + type);
                }

                ActionResult result = executor.execute(new ActionConfig(type, config), ctx);

                logs.add(Map.of(
                        "step", "action:" + type,
                        "success", result.success(),
                        "message", result.message()
                ));

                if (!result.success()) {
                    throw new RuntimeException("Ação " + type + " falhou: " + result.message());
                }
            }

            exec.setStatus("SUCCESS");
            exec.setExecutionLog(Map.of("steps", logs));
            log.info("✅ Workflow {} executado com sucesso em {}ms", wf.getId(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("❌ Workflow {} falhou: {}", wf.getId(), e.getMessage());
            exec.setStatus("FAILED");
            exec.setErrorMessage(e.getMessage());
            exec.setExecutionLog(Map.of("steps", logs));
        }

        return finish(exec, start);
    }

    private WorkflowExecution finish(WorkflowExecution exec, long start) {
        exec.setFinishedAt(LocalDateTime.now());
        exec.setDurationMs((int) (System.currentTimeMillis() - start));
        return execRepo.save(exec);
    }
}