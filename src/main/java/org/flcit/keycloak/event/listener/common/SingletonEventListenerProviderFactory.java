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

package org.flcit.keycloak.event.listener.common;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public abstract class SingletonEventListenerProviderFactory implements EventListenerProviderFactory {

    private EventListenerProvider listener;

    /**
     * @return
     */
    public abstract EventListenerProvider generate();

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return listener != null ? listener : generateIntern();
    }

    private synchronized EventListenerProvider generateIntern() {
        if (listener == null) {
            listener = generate();
        }
        return listener;
    }

    private synchronized void destroy() {
        if (listener instanceof Destroyable) {
            ((Destroyable) listener).destroy();
        }
        listener = null;
    }

    @Override
    public void init(Scope config) {
        // DO NOTHING
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // DO NOTHING
    }

    @Override
    public void close() {
        destroy();
    }

}
