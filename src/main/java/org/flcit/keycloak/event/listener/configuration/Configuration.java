/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flcit.keycloak.event.listener.configuration;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public final class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private Configuration() { }

    /**
     * @return
     */
    public static Properties getProperties() {
        final Properties properties = new Properties();
        try {
            properties.load(Configuration.class.getResourceAsStream("/application.properties"));
            return properties;
        } catch (IOException e) {
            LOG.error("Impossible de charger le fichier de properties");
            throw new IllegalStateException(e);
        }
    }

}
