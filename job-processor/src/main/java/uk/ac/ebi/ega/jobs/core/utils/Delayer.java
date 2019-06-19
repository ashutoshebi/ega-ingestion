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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Factory class to create different types of Delayer instances.
 * See {@link DelayConfiguration.DelayType}.
 */
public abstract class Delayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Delayer.class);

    public static Delayer create(final DelayConfiguration.DelayType delayType,
                                 final long delay,
                                 final long maxDelay,
                                 final TimeUnit timeUnit) {
        switch (delayType) {
            case LINEAR:
                return new LinearDelayer(delay, timeUnit);
            case BACKOFF:
                return new BackoffDelayer(delay, maxDelay, timeUnit);
            default:
                final String message = String.format("Unsupported Delayer type: %s. " +
                                "Possible types are: %s", delayType,
                        Arrays.toString(DelayConfiguration.DelayType.values()));
                throw new IllegalArgumentException(message);
        }
    }

    public static Delayer create(final DelayConfiguration delayConfiguration) {
        return create(delayConfiguration.getDelayType(),
                delayConfiguration.getDelay(),
                delayConfiguration.getMaxDelay(),
                delayConfiguration.getTimeUnit());
    }

    public abstract void delay();

    void doWait(final long amount, final TimeUnit timeUnit) {
        try {
            timeUnit.sleep(amount);
        } catch (InterruptedException e) {
            LOGGER.error("Exception while sleeping: {}", e.getMessage());
        }
    }

}
