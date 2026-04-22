package com.backend.shared.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic retry when an OptimisticLockException occurs.
 * This handles race conditions when users perform rapid concurrent updates
 * (e.g., quickly changing cart quantities).
 *
 * <p>The retry aspect runs at a higher priority than @Transactional, so each
 * retry attempt executes within a fresh transaction.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnOptimisticLock {

    /**
     * Maximum number of retry attempts (not counting the initial attempt).
     */
    int maxRetries() default 3;
}
