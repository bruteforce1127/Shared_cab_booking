package com.kucp1127.sharedcabbooking.service.locking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for distributed locking using Redis/Redisson.
 * Provides thread-safe operations across multiple instances.
 *
 * Lock Strategy:
 * - TTL: 10 seconds (auto-release)
 * - Wait time: 5 seconds (max wait to acquire)
 * - Fair locks for ordering guarantees
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 5;
    private static final long DEFAULT_LEASE_TIME = 10;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private static final String LOCK_PREFIX = "shared-cab:lock:";
    private static final String RIDE_GROUP_LOCK = LOCK_PREFIX + "ride-group:";
    private static final String BOOKING_LOCK = LOCK_PREFIX + "booking:";
    private static final String CAB_LOCK = LOCK_PREFIX + "cab:";

    /**
     * Execute operation with lock on ride group.
     */
    public <T> T executeWithRideGroupLock(Long rideGroupId, Supplier<T> operation) {
        return executeWithLock(RIDE_GROUP_LOCK + rideGroupId, operation);
    }

    /**
     * Execute operation with lock on booking.
     */
    public <T> T executeWithBookingLock(Long bookingId, Supplier<T> operation) {
        return executeWithLock(BOOKING_LOCK + bookingId, operation);
    }

    /**
     * Execute operation with lock on cab.
     */
    public <T> T executeWithCabLock(Long cabId, Supplier<T> operation) {
        return executeWithLock(CAB_LOCK + cabId, operation);
    }

    /**
     * Execute operation with a named lock.
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, operation);
    }

    /**
     * Execute operation with custom lock parameters.
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> operation) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, DEFAULT_TIME_UNIT);

            if (!acquired) {
                log.warn("Failed to acquire lock: {} after {} seconds", lockKey, waitTime);
                throw new LockAcquisitionException("Unable to acquire lock: " + lockKey);
            }

            log.debug("Lock acquired: {}", lockKey);
            return operation.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Lock acquisition interrupted: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    /**
     * Try to execute operation with lock, return empty if lock not acquired.
     */
    public <T> java.util.Optional<T> tryExecuteWithLock(String lockKey, Supplier<T> operation) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(0, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);

            if (!acquired) {
                log.debug("Lock not immediately available: {}", lockKey);
                return java.util.Optional.empty();
            }

            try {
                return java.util.Optional.of(operation.get());
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return java.util.Optional.empty();
        }
    }

    /**
     * Execute void operation with lock.
     */
    public void executeWithLock(String lockKey, Runnable operation) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Check if a lock is currently held.
     */
    public boolean isLocked(String lockKey) {
        return redissonClient.getLock(lockKey).isLocked();
    }

    /**
     * Custom exception for lock acquisition failures.
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
