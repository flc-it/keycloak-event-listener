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

import org.keycloak.events.EventListenerProvider;

import org.flcit.keycloak.event.listener.common.SingletonEventListenerProviderFactory;
import org.flcit.keycloak.event.listener.configuration.Configuration;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public class KeycloakEventListenerProviderFactory extends SingletonEventListenerProviderFactory {

    /**
     *
     */
    public EventListenerProvider generate() {
        return new KeycloakEventListener(Configuration.getProperties());
    }

    @Override
    public String getId() {
        return "datalab-event-listener";
    }

}
