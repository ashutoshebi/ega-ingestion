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

public class LinearDelayer extends Delayer {

    private final long delay;

    private final TimeUnit timeUnit;

    /**
     * LinearDelayer always waits for the specified, constant amount of time.
     *
     * @param delay     A call to {@link #delay()} always waits for
     *                  the specified, "delay" amount of time.
     * @param timeUnit  Should it wait for seconds, milliseconds, etc.?
     */
    public LinearDelayer(final long delay, final TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public LinearDelayer(final long delay) {
        this(delay, TimeUnit.SECONDS);
    }

    @Override
    public void delay() {
        doWait(delay, timeUnit);
    }

}
