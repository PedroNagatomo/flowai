package com.flowai.engine;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
@Slf4j
public class CronService {

    private final CronParser parser = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    );

    /**
     * Calcula a próxima execução de um cron a partir de um instante.
     * Retorna Optional.empty() se o cron for inválido.
     */
    public Optional<Instant> nextExecution(String cronExpression, String timezone, Instant from) {
        try {
            var cron = parser.parse(cronExpression);
            cron.validate();

            ZoneId zone = timezone == null || timezone.isBlank()
                    ? ZoneId.of("America/Sao_Paulo")
                    : ZoneId.of(timezone);

            var executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime fromZdt = ZonedDateTime.ofInstant(from, zone);

            return executionTime.nextExecution(fromZdt)
                    .map(ZonedDateTime::toInstant);
        } catch (Exception e) {
            log.warn("⚠️ Cron inválido '{}': {}", cronExpression, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isValid(String cronExpression) {
        try {
            var cron = parser.parse(cronExpression);
            cron.validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}