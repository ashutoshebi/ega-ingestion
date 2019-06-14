/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.ega.jobs.core.utils;

import java.util.concurrent.TimeUnit;

/**
 * Value class which stores values used by
 * {@link Delayer}.
 */
public class DelayConfiguration {

    /**
     * Specifies the type of delay that occurs between subsequent calls to {@link Delayer#delay()}.
     *
     * A linear delay always waits the same constant amount of time.
     * A backoff delay exponentially increases the amount of time-to-be-waited
     * between the multiple calls to {@link Delayer#delay()}.
     */
    private final DelayType delayType;

    /**
     * Specifies the amount of fixed delay when the type of delay is {@link DelayType#LINEAR}.
     */
    private final long delay;

    /**
     * Specifies the maximum amount of delay when the type of delay is {@link DelayType#BACKOFF}.
     */
    private final long maxDelay;

    private final TimeUnit timeUnit;

    public DelayConfiguration(final DelayType delayType,
                              final long delay,
                              final long maxDelay,
                              final TimeUnit timeUnit) {
        this.delayType = delayType;
        this.delay = delay;
        this.maxDelay = maxDelay;
        this.timeUnit = timeUnit;
    }

    public DelayConfiguration(final DelayType delayType,
                              final long delay,
                              final long maxDelay) {
        this(delayType, delay, maxDelay, TimeUnit.SECONDS);
    }

    public enum DelayType {
        LINEAR, BACKOFF
    }

    public DelayType getDelayType() {
        return delayType;
    }

    public long getDelay() {
        return delay;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
