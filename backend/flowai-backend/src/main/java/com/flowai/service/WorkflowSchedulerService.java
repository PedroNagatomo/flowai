package com.flowai.service;

import com.flowai.engine.CronService;
import com.flowai.entity.Workflow;
import com.flowai.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowSchedulerService {

    private final WorkflowRepository workflowRepo;
    private final WorkflowExecutorService executor;
    private final CronService cronService;

    /**
     * Tick a cada 30 segundos. Busca workflows SCHEDULE ativos com next_run_at <= now
     * e dispara cada um.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    @Transactional
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        var due = workflowRepo.findDueWorkflows(now);

        if (due.isEmpty()) return;

        log.debug("⏰ Scheduler tick: {} workflow(s) prontos pra rodar", due.size());

        for (Workflow wf : due) {
            try {
                executeScheduled(wf);
            } catch (Exception e) {
                log.error("❌ Erro ao agendar workflow {}: {}", wf.getId(), e.getMessage());
            }
        }
    }

    private void executeScheduled(Workflow wf) {
        String cron = extractCron(wf);
        String timezone = extractTimezone(wf);
        LocalDateTime now = LocalDateTime.now();

        log.info("⏰ Disparando workflow agendado: {} ({})", wf.getName(), wf.getId());

        // Atualiza next_run_at ANTES de executar (evita re-disparo se execução falhar)
        wf.setLastRunAt(now);
        Instant base = now.atZone(ZoneId.systemDefault()).toInstant();
        cronService.nextExecution(cron, timezone, base)
                .ifPresent(next -> wf.setNextRunAt(LocalDateTime.ofInstant(next, ZoneId.systemDefault())));
        workflowRepo.save(wf);

        // Dispara assíncrono
        executor.executeAsync(wf.getId(), Map.of(
                "scheduled", true,
                "triggeredAt", now.toString()
        ));
    }

    @SuppressWarnings("unchecked")
    private String extractCron(Workflow wf) {
        var trigger = (Map<String, Object>) wf.getDefinition().get("trigger");
        var config = (Map<String, Object>) trigger.get("config");
        return (String) config.get("cron");
    }

    @SuppressWarnings("unchecked")
    private String extractTimezone(Workflow wf) {
        var trigger = (Map<String, Object>) wf.getDefinition().get("trigger");
        var config = (Map<String, Object>) trigger.get("config");
        Object tz = config.get("timezone");
        return tz == null ? "America/Sao_Paulo" : tz.toString();
    }

    /**
     * Recalcula next_run_at quando um workflow é ativado.
     * Chamado pelo WorkflowService no update.
     */
    public void reschedule(Workflow wf) {
        if (!Boolean.TRUE.equals(wf.getIsActive())) {
            wf.setNextRunAt(null);
            return;
        }
        if (!"SCHEDULE".equals(getTriggerType(wf))) {
            wf.setNextRunAt(null);
            return;
        }

        String cron = extractCron(wf);
        String tz = extractTimezone(wf);

        if (cron == null || cron.isBlank() || !cronService.isValid(cron)) {
            log.warn("⚠️ Workflow {} tem cron inválido: {}", wf.getId(), cron);
            wf.setNextRunAt(null);
            return;
        }

        Instant now = Instant.now();
        cronService.nextExecution(cron, tz, now).ifPresentOrElse(
                next -> {
                    wf.setNextRunAt(LocalDateTime.ofInstant(next, ZoneId.systemDefault()));
                    log.info("📅 Workflow {} reagendado para {}", wf.getId(), wf.getNextRunAt());
                },
                () -> wf.setNextRunAt(null)
        );
    }

    @SuppressWarnings("unchecked")
    private String getTriggerType(Workflow wf) {
        var trigger = (Map<String, Object>) wf.getDefinition().get("trigger");
        return (String) trigger.get("type");
    }
}