package com.orderinventory.orderinventory.service;

import com.orderinventory.orderinventory.entity.OutboxEvent;
import com.orderinventory.orderinventory.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    // runs every 5 seconds - picks up anything not yet published and "sends" it.
    // in a real system this would push to a message queue or actually send an email;
    // here we just log it, which is enough to prove the pattern works end to end
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : pending) {
            log.info("Publishing outbox event [{}]: {}", event.getEventType(), event.getPayload());
            event.setPublished(true);
            outboxEventRepository.save(event);
        }
    }
}