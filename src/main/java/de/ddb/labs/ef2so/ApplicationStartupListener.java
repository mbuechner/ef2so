/*
 * Copyright 2018-2025 Michael Büchner, Deutsche Digitale Bibliothek.
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
package de.ddb.labs.ef2so;

import de.ddb.labs.ef2so.processor.ProcessorFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application startup listener that initializes the ProcessorFactory
 * when the application starts. If initialization fails, the application
 * will not start successfully.
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@WebListener
public class ApplicationStartupListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStartupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ProcessorFactory.getInstance();
        } catch (Exception e) {
            LOG.error("Failed to initialize ProcessorFactory - application startup aborted", e);
            throw new RuntimeException("Failed to initialize ProcessorFactory", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application shutting down.");
    }
}
