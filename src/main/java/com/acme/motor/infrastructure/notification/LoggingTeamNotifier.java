package com.acme.motor.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingTeamNotifier implements TeamNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTeamNotifier.class);

    @Override
    public void notify(String destination, String summary) {
        LOGGER.info("Notificação para equipe em {}: {}", destination, summary);
    }
}
