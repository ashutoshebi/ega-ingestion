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
package uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.utils;

import java.util.concurrent.TimeUnit;

public class BackoffDelayer extends Delayer {

    private final long delay;

    private final long maxDelay;

    private final TimeUnit timeUnit;

    // The delayFactor is a value which increases exponentially
    // in order to provide an exponential backoff delay.
    private long delayFactor = 1;

    /**
     * The BackoffDelayer first waits for the specified amount of time,
     * then the amount-to-be-waited is increased exponentially.
     *
     * @param delay     A call to {@link #delay()} first waits "delay" amount of time,
     *                  then it exponentially increases the original "delay" amount.
     * @param maxDelay  A call to {@link #delay()} never waits more than
     *                  "maxDelay" amount of time.
     * @param timeUnit  Should it wait for seconds, milliseconds, etc.?
     */
    public BackoffDelayer(final long delay, final long maxDelay, final TimeUnit timeUnit) {
        this.delay = delay;
        this.maxDelay = maxDelay;
        this.timeUnit = timeUnit;
    }

    public BackoffDelayer(final long delay, final long maxDelay) {
        this(delay, maxDelay, TimeUnit.SECONDS);
    }

    @Override
    public void delay() {
        final long  actualDelay = Math.min(delay * delayFactor, maxDelay);
        doWait(actualDelay, timeUnit);
        delayFactor *= 2;
    }

}
