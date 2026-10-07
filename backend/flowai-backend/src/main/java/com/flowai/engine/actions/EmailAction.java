package com.flowai.engine.actions;

import com.flowai.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailAction implements ActionExecutor {

    private final JavaMailSender mailSender;
    private final TemplateResolver resolver;

    @Value("${spring.mail.username:no-reply@flowai.dev}")
    private String fromAddress;

    @Override
    public ActionType supportedType() {
        return ActionType.SEND_EMAIL;
    }

    @Override
    public ActionResult execute(ActionConfig cfg, ExecutionContext ctx) {
        var c = cfg.config();
        String to = (String) c.get("to");
        String subject = resolver.resolve((String) c.get("subject"), ctx);
        String body = resolver.resolve((String) c.get("body"), ctx);

        if (to == null || to.isBlank()) {
            return ActionResult.failure("Campo 'to' é obrigatório");
        }

        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject == null ? "(sem assunto)" : subject);
            msg.setText(body == null ? "" : body);
            mailSender.send(msg);
            log.info("📧 Email enviado para {}", to);
            return ActionResult.success("Email enviado para " + to);
        } catch (Exception e) {
            log.error("❌ Falha ao enviar email", e);
            return ActionResult.failure("Falha ao enviar email: " + e.getMessage());
        }
    }
}