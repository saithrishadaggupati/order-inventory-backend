package com.orderinventory.orderinventory.service;

import com.orderinventory.orderinventory.entity.IdempotencyKey;
import com.orderinventory.orderinventory.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    // tries to "claim" a key by inserting it first. if the unique constraint on
    // idempotency_key blocks it, that means someone already claimed this key -
    // we return empty so the caller knows to fetch and return the original response
    // instead of processing again. this runs in its own transaction so the claim
    // is committed immediately, separate from whatever the actual order logic does.
    // note: this method intentionally does NOT catch the constraint violation itself -
    // once hibernate flags a transaction rollback-only, catching the exception here
    // doesn't help; spring still fails on commit. the exception has to escape this
    // transactional boundary and get caught by the non-transactional caller instead.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claimKeyOrThrow(String key) {
        IdempotencyKey record = new IdempotencyKey();
        record.setIdempotencyKey(key);
        idempotencyKeyRepository.save(record);
        idempotencyKeyRepository.flush();
    }

    // safe to call from anywhere - wraps the transactional call and interprets
    // the constraint violation as "someone already claimed this key"
    public boolean tryClaimKey(String key) {
        try {
            claimKeyOrThrow(key);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    public void saveResponse(String key, int status, String body) {
        IdempotencyKey record = idempotencyKeyRepository.findByIdempotencyKey(key)
                .orElseThrow(() -> new RuntimeException("Idempotency key not found: " + key));
        record.setResponseStatus(status);
        record.setResponseBody(body);
        idempotencyKeyRepository.save(record);
    }

    public Optional<IdempotencyKey> getExisting(String key) {
        return idempotencyKeyRepository.findByIdempotencyKey(key);
    }
}