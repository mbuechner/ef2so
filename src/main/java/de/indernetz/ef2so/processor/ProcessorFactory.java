/*
 * Copyright 2018 Michael Büchner, Deutsche Digitale Bibliothek.
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
package de.indernetz.ef2so.processor;

public class ProcessorFactory {

    private final static int MAX_POOL_SIZE = 100;
    private final Processor[] pool;

    private static final class InstanceHolder {

        static final ProcessorFactory INSTANCE = new ProcessorFactory();
    }

    private ProcessorFactory() {
        pool = new Processor[MAX_POOL_SIZE];
        for (int i = 0; i < MAX_POOL_SIZE; ++i) {
            pool[i] = new Processor();
        }
    }

    public Processor getFreeProcessor() {
        for (int i = 0; i < MAX_POOL_SIZE; ++i) {
            if (pool[i].isFree()) {
                return pool[i];
            }
        }
        throw new IllegalStateException("Could not process request, because there's no processor left.");
    }

    public static ProcessorFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }
}
