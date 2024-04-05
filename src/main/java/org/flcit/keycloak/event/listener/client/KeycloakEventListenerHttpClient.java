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

package org.flcit.keycloak.event.listener.client;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.http.client.HttpClient;
import org.flcit.keycloak.connector.http.HttpClientKeycloakBuilder;
import org.flcit.keycloak.connector.http.util.HttpClientUtils;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public class KeycloakEventListenerHttpClient implements EventClient {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakEventListenerHttpClient.class);

    private static final String PROPERTIES_PREFIX = "http.";

    private final String url;
    private final String serviceEvent;
    private final String serviceAdminEvent;
    private final HttpClient httpClient;

    /**
     * @param properties
     */
    public KeycloakEventListenerHttpClient(Properties properties) {
        this(
                properties.getProperty(PROPERTIES_PREFIX + "base-url"),
                properties.getProperty(PROPERTIES_PREFIX + "service.event"),
                properties.getProperty(PROPERTIES_PREFIX + "service.admin-event"),
                () -> HttpClientKeycloakBuilder.build(properties, PROPERTIES_PREFIX)
                );

    }

    /**
     * @param config
     */
    public KeycloakEventListenerHttpClient(Scope config) {
        this(
                config.get(PROPERTIES_PREFIX + "base-url"),
                config.get(PROPERTIES_PREFIX + "service.event"),
                config.get(PROPERTIES_PREFIX + "service.admin-event"),
                () -> HttpClientKeycloakBuilder.build(config, PROPERTIES_PREFIX)
                );
    }

    private KeycloakEventListenerHttpClient(String url, String serviceEvent, String serviceAdminEvent,
            Supplier<HttpClient> httpClient) {
        this.url = url;
        this.serviceEvent = serviceEvent;
        this.serviceAdminEvent = serviceAdminEvent;
        this.httpClient = hasConfig() ? httpClient.get() : null;
        LOG.info("Active : {}", this.isActive());
        LOG.info("Base Url : {}", this.url);
        LOG.info("Service Event : {}", this.serviceEvent);
        LOG.info("Service Admin Event : {}", this.serviceAdminEvent);
    }

    @Override
    public void close() {
        org.apache.http.client.utils.HttpClientUtils.closeQuietly(httpClient);
    }

    @Override
    public void send(Event event) {
        if (StringUtil.isNotBlank(serviceEvent)) {
            send(serviceEvent, event);
        }
    }

    @Override
    public void send(AdminEvent event) {
        if (StringUtil.isNotBlank(serviceAdminEvent)) {
            send(serviceAdminEvent, event);
        }
    }

    private void send(String service, Object value) {
        if (this.httpClient != null) {
            try {
                HttpClientUtils.postJsonNoResponse(httpClient, url + service, value);
            } catch (IOException e) {
                LOG.error("DatalabHttpClient - send", e);
            }
        }
    }

    private boolean hasConfig() {
        return StringUtil.isNotBlank(this.url)
                && (StringUtil.isNotBlank(this.serviceEvent) || StringUtil.isNotBlank(this.serviceAdminEvent));
    }

    @Override
    public boolean isActive() {
        return this.httpClient != null;
    }

}
