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

import java.time.Duration;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.flcit.commons.core.util.AsyncUtils;
import org.flcit.commons.core.util.PropertyUtils;
import org.flcit.keycloak.connector.kafka.KafkaClientBuilder;
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
public class KeycloakEventListenerKafkaProducer implements EventClient {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakEventListenerKafkaProducer.class);

    private static final String PROPERTIES_PREFIX = "kafka.";

    private final boolean bootstrapServers;
    private final String topicEvent;
    private final String topicAdminEvent;
    private final Long sleepTimeout;
    private KafkaProducer<String, Object> kafkaProducer;

    /**
     * @param properties
     */
    public KeycloakEventListenerKafkaProducer(Properties properties) {
        this(
                KafkaClientBuilder.hasBootstrapServers(properties, PROPERTIES_PREFIX),
                properties.getProperty(PROPERTIES_PREFIX + "topic.event"),
                properties.getProperty(PROPERTIES_PREFIX + "topic.admin-event"),
                PropertyUtils.getNumber(properties, PROPERTIES_PREFIX + "sleep-timeout", 30000L, Long.class),
                () -> KafkaClientBuilder.buildProducer(properties, PROPERTIES_PREFIX)
                );
    }

    /**
     * @param config
     */
    public KeycloakEventListenerKafkaProducer(Scope config) {
        this(
                KafkaClientBuilder.hasBootstrapServers(config, PROPERTIES_PREFIX),
                config.get(PROPERTIES_PREFIX + "topic.event"),
                config.get(PROPERTIES_PREFIX + "topic.admin-event"),
                config.getLong(PROPERTIES_PREFIX + "sleep-timeout", 30000L),
                () -> KafkaClientBuilder.buildProducer(config, PROPERTIES_PREFIX)
                );
    }

    private KeycloakEventListenerKafkaProducer(boolean bootstrapServers, String topicEvent, String topicAdminEvent,
            Long sleepTimeout, Supplier<KafkaProducer<String, Object>> kafkaProducer) {
        this.bootstrapServers = bootstrapServers;
        this.topicEvent = topicEvent;
        this.topicAdminEvent = topicAdminEvent;
        this.sleepTimeout = sleepTimeout;
        if (hasConfig()) {
            AsyncUtils.getSyncErrorAsync(
                    kafkaProducer,
                    sleepTimeout,
                    currentKafkaProducer -> this.kafkaProducer = currentKafkaProducer);
        }
        LOG.info("Active : {}", this.hasConfig());
        LOG.info("Topic Event : {}", this.topicEvent);
        LOG.info("Topic Admin Event : {}", this.topicAdminEvent);
        LOG.info("Sleep timeout : {}", this.sleepTimeout);
    }

    @Override
    public void send(Event event) {
        if (StringUtil.isNotBlank(topicEvent)) {
            sendIntern(topicEvent, event);
        }
    }

    @Override
    public void send(AdminEvent event) {
        if (StringUtil.isNotBlank(topicAdminEvent)) {
            sendIntern(topicAdminEvent, event);
        }
    }

    private void sendIntern(String topic, Object event) {
        if (kafkaProducer != null) {
            kafkaProducer.send(new ProducerRecord<>(topic, event));
        } else if (bootstrapServers) {
            LOG.warn("DatalabKafkaProducer is not running, messages are loss :(");
        }
    }

    @Override
    public void close() {
        close(null);
    }

    /**
     * @param waitInSeconds
     */
    public void close(Long waitInSeconds) {
        if (kafkaProducer == null) {
            return;
        }
        if (waitInSeconds != null) {
            this.kafkaProducer.close(Duration.ofSeconds(waitInSeconds));
        } else {
            this.kafkaProducer.close();
        }
    }

    private boolean hasConfig() {
        return this.bootstrapServers
                && (StringUtil.isNotBlank(this.topicEvent) || StringUtil.isNotBlank(this.topicAdminEvent));
    }

    @Override
    public boolean isActive() {
        return this.kafkaProducer != null;
    }

}
