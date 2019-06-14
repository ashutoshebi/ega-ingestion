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

import org.assertj.core.data.Percentage;
import org.junit.Test;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration.DelayType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayerTest {

    @Test
    public void linearDelayer_WaitsAlwaysForTheSameAmountOfTime() {
        final long configuredDelayTime = 500;
        final long maxDelayIsNotUsedInLinear = -1;
        final Delayer delayer = Delayer.create(DelayType.LINEAR, configuredDelayTime,
                maxDelayIsNotUsedInLinear, TimeUnit.MILLISECONDS);

        final Runnable callingDelayTwice = () -> {
            delayer.delay();
            delayer.delay();
        };

        // The linear delays are always constant:
        assertThatRunnableTakesGivenSeconds(callingDelayTwice, 500+500);
    }

    @Test
    public void backoffDelayer_WaitsForAnExponentiallyIncreasingAmountOfTime() {
        final long initialDelayTime = 100;
        final long maxDelay = 20000;
        final DelayConfiguration config = new DelayConfiguration(DelayType.BACKOFF,
                initialDelayTime, maxDelay, TimeUnit.MILLISECONDS);
        final Delayer delayer = Delayer.create(config);

        final Runnable callingDelayFourTimes = () -> {
            delayer.delay();
            delayer.delay();
            delayer.delay();
            delayer.delay();
        };

        // The backoff delays are increasing exponentially:
        assertThatRunnableTakesGivenSeconds(callingDelayFourTimes, 100+200+400+800);
    }

    @Test
    public void backoffDelayer_NeverWaitsMoreThanMaxDelay() {
        final long initialDelayTime = 100;
        final long maxDelayToBeRespected = 150;
        final Delayer delayer = Delayer.create(DelayType.BACKOFF,
                initialDelayTime, maxDelayToBeRespected, TimeUnit.MILLISECONDS);

        final Runnable callingDelayTwice = () -> {
            delayer.delay();
            delayer.delay();
        };

        // The initialDelayTime==100 should be doubled to 200
        // (because we increase the delay times exponentially),
        // but the maxDelayToBeRespected is set to 150,
        // so the second delay will only be 150:
        assertThatRunnableTakesGivenSeconds(callingDelayTwice, 100+150);
    }

    private void assertThatRunnableTakesGivenSeconds(final Runnable runnable, final int expectedTimeInMilliSeconds) {
        final long startTime = System.nanoTime();

        runnable.run();

        final long endTime = System.nanoTime();
        final long timeElapsed = endTime - startTime;
        final long expectedTimeInNanoSeconds = expectedTimeInMilliSeconds * ((long) 1e6);

        assertThat(timeElapsed).isCloseTo(expectedTimeInNanoSeconds, Percentage.withPercentage(10));
    }

}