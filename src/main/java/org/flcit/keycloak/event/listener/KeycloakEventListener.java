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

package org.flcit.keycloak.event.listener;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flcit.keycloak.event.listener.client.EventClient;
import org.flcit.keycloak.event.listener.client.KeycloakEventListenerHttpClient;
import org.flcit.keycloak.event.listener.client.KeycloakEventListenerJmsProducer;
import org.flcit.keycloak.event.listener.client.KeycloakEventListenerKafkaProducer;
import org.flcit.keycloak.event.listener.common.Destroyable;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public class KeycloakEventListener implements EventListenerProvider, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakEventListener.class);

    private final List<EventClient> clients;

    KeycloakEventListener(Properties properties) {
        this.clients = Arrays.asList(
                new KeycloakEventListenerHttpClient(properties),
                new KeycloakEventListenerKafkaProducer(properties),
                new KeycloakEventListenerJmsProducer(properties)
                );
    }

    @Override
    public void close() {
        // DO NOTHING
    }

    @Override
    public void onEvent(Event event) {
        if (LOG.isInfoEnabled()) {
            LOG.info(event.toString());
        }
        for (EventClient client: clients) {
            client.send(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (LOG.isInfoEnabled()) {
            LOG.info(event.toString());
        }
        for (EventClient client: clients) {
            client.send(event);
        }
    }

    @Override
    public void destroy() {
        for (EventClient client: clients) {
            client.close();
        }
    }

}
