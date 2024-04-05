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

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.JMSSecurityRuntimeException;

import org.flcit.commons.core.util.PropertyUtils;
import org.flcit.commons.core.util.ThreadUtils;
import org.flcit.keycloak.connector.jms.JmsClientBuilder;
import org.flcit.keycloak.connector.jms.util.JmsUtils;
import org.flcit.keycloak.event.listener.async.ExecutorBuilder;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public class KeycloakEventListenerJmsProducer implements EventClient, ExceptionListener {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakEventListenerJmsProducer.class);
    private static final ExceptionListener LOG_LISTENER = exception -> LOG.error("DatalabJmsProducer - onException", exception);

    private static final String PROPERTIES_PREFIX = "jms.";
    private static final String PROPERTIES_SERVER_PREFIX = PROPERTIES_PREFIX + "server.";

    private final boolean serverUrl;
    private final ConnectionFactory connectionFactory;
    private final Destination destinationEvent;
    private final Destination destinationAdminEvent;
    private final Long sleepTimeout;
    private final ExecutorService executor;
    private final ThreadLocal<JMSContext> contexts = ThreadLocal.withInitial(() -> initJMSContext());

    /**
     * @param properties
     */
    public KeycloakEventListenerJmsProducer(Properties properties) {
        this(
                JmsClientBuilder.hasServerUrl(properties, PROPERTIES_SERVER_PREFIX),
                () -> JmsClientBuilder.buildSafelyConnectionFactory(properties, PROPERTIES_SERVER_PREFIX),
                JmsClientBuilder.buildDestination(properties, PROPERTIES_PREFIX + "destination.event."),
                JmsClientBuilder.buildDestination(properties, PROPERTIES_PREFIX + "destination.admin-event."),
                PropertyUtils.getNumber(properties, PROPERTIES_PREFIX + "sleep-timeout", 30000L, Long.class),
                () -> ExecutorBuilder.build(properties, PROPERTIES_PREFIX + "thread-pool.")
                );
    }

    public KeycloakEventListenerJmsProducer(Scope config) {
        this(
                JmsClientBuilder.hasServerUrl(config, PROPERTIES_SERVER_PREFIX),
                () -> JmsClientBuilder.buildSafelyConnectionFactory(config, PROPERTIES_SERVER_PREFIX),
                JmsClientBuilder.buildDestination(config, PROPERTIES_PREFIX + "destination.event."),
                JmsClientBuilder.buildDestination(config, PROPERTIES_PREFIX + "destination.admin-event."),
                config.getLong(PROPERTIES_PREFIX + "sleep-timeout", 30000L),
                () -> ExecutorBuilder.build(config, PROPERTIES_PREFIX + "thread-pool.")
                );
    }

    private KeycloakEventListenerJmsProducer(boolean serverUrl, Supplier<ConnectionFactory> connectionFactory,
            Destination destinationEvent, Destination destinationAdminEvent, Long sleepTimeout,
            Supplier<ExecutorService> executor) {
        this.serverUrl = serverUrl;
        this.destinationEvent = destinationEvent;
        this.destinationAdminEvent = destinationAdminEvent;
        this.sleepTimeout = sleepTimeout;
        if (this.hasConfig()) {
            this.connectionFactory = connectionFactory.get();
            this.executor = executor.get();
        } else {
            this.connectionFactory = null;
            this.executor = null;
        }
        LOG.info("Active : {}", this.isActive());
        LOG.info("Destination Event : {}", this.destinationEvent);
        LOG.info("Destination Admin Event : {}", this.destinationAdminEvent);
        LOG.info("Sleep timeout : {}", this.sleepTimeout);
    }

    private JMSContext initJMSContext() {
        final JMSContext context = connectionFactory.createContext();
        context.setExceptionListener(LOG_LISTENER);
        return context;
    }

    @Override
    public void send(Event event) {
        if (destinationEvent != null) {
            executor.execute(() -> sendInternSafely(destinationEvent, event));
        }
    }

    @Override
    public void send(AdminEvent event) {
        if (destinationAdminEvent != null) {
            executor.execute(() -> sendInternSafely(destinationEvent, event));
        }
    }

    private void sendInternSafely(Destination destination, Object event) {
        makeSafely(() -> sendIntern(destination, event));
    }

    private void sendIntern(Destination destination, Object event) {
        final JMSContext currentContext = contexts.get();
        currentContext.createProducer().send(destination, JmsUtils.getJsonMessage(currentContext, event));
    }

    private void makeSafely(Runnable runnable) {
        do {
            try {
                runnable.run();
                return;
            } catch (JMSSecurityRuntimeException e) {
                throw new IllegalStateException(e);
            } catch (JMSRuntimeException e) {
                if (!catchIsClosed(e)) {
                    LOG.error("JMS Context", e);
                }
                ThreadUtils.sleep(sleepTimeout);
            }
        } while (true);
    }

    private final boolean catchIsClosed(JMSRuntimeException e) {
        if (e.getCause() instanceof javax.jms.IllegalStateException) {
            try {
                contexts.get().close();
            } catch (Exception exc) { /* DO NOTHING */ }
            makeSafely(this::setCurrentJMSContext);
            return true;
        }
        return false;
    }

    private void setCurrentJMSContext() {
        contexts.set(initJMSContext());
    }

    /**
     * 
     */
    public void cleanCurrentJMSContext() {
        contexts.remove();
    }

    @Override
    public void close() {
        close(null);
    }

    /**
     * @param waitInSeconds
     */
    public void close(Long waitInSeconds) {
        if (waitInSeconds != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(waitInSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            executor.shutdownNow();
        }
    }

    @Override
    public void onException(JMSException exception) {
        LOG.error("DatalabJmsProducer - onException", exception);
    }

    private boolean hasConfig() {
        return this.serverUrl
                && (this.destinationEvent != null || this.destinationAdminEvent != null);
    }

    @Override
    public boolean isActive() {
        return this.connectionFactory != null
                && this.executor != null;
    }

}
