/*
 * Copyright 2018-2026 Michael Büchner, Deutsche Digitale Bibliothek.
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
package de.ddb.labs.ef2so.processor;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
public class ProcessorFactory {

    private static final int MAX_POOL_SIZE = 128;
    private static final Logger LOG = LoggerFactory.getLogger(ProcessorFactory.class);
    private final Queue<Processor> idleProcessors = new ConcurrentLinkedQueue<>();
    private final Semaphore permits = new Semaphore(MAX_POOL_SIZE);

    private static final class InstanceHolder {

        static final ProcessorFactory INSTANCE = new ProcessorFactory();
    }

    private ProcessorFactory() {
        final long started = System.nanoTime();
        // Validate the transformation at startup and create more processors as needed.
        idleProcessors.add(new Processor());
        LOG.info("Processor pool ready in {} ms (capacity {})",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), MAX_POOL_SIZE);
    }

    public Optional<String> tryProcess(final String input) {
        if (!permits.tryAcquire()) {
            return Optional.empty();
        }
        try {
            final Processor idle = idleProcessors.poll();
            final Processor processor = idle == null ? new Processor() : idle;
            final String result = processor.process(input);
            processor.reset();
            // Only reuse processors whose entire pipeline completed and reset successfully.
            idleProcessors.add(processor);
            return Optional.of(result);
        } finally {
            permits.release();
        }
    }

    public static ProcessorFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }
}
