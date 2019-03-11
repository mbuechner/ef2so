/*
 * Copyright 2018, 2019 Michael Büchner, Deutsche Digitale Bibliothek.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
public class ProcessorFactory {

    private final static int MAX_POOL_SIZE = 128; // no. of max. parallel requests
    private final List<Processor> pool = Collections.synchronizedList(new ArrayList<>());
    private final static Logger LOG = LoggerFactory.getLogger(ProcessorFactory.class);
        
    private static final class InstanceHolder {
        static final ProcessorFactory INSTANCE = new ProcessorFactory();
    }

    private ProcessorFactory() {
        LOG.info("Initializing pool with {} processors...", MAX_POOL_SIZE);
        for (int i = 0; i < MAX_POOL_SIZE; ++i) {
            pool.add(new Processor());
        }
        LOG.info("Done initializing pool.");
    }

    public Processor getFreeProcessor() {
        final Iterator<Processor> iterator = pool.iterator();
        synchronized (pool) {
            while (iterator.hasNext()) {
                final Processor p = iterator.next();
                if (p.isFree()) {
                    p.setOccupied();
                    return p;
                }
            }
        }
        throw new IllegalStateException("Could not process request, because there's no processor left.");
    }

    public static ProcessorFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }
}
