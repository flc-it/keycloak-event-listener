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

package org.flcit.keycloak.event.listener.async;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.flcit.commons.core.util.PropertyUtils;
import org.keycloak.Config.Scope;

/**
 * 
 * @since 
 * @author Florian Lestic
 */
public final class ExecutorBuilder {

    private ExecutorBuilder() { }

    /**
     * @param properties
     * @param prefix
     * @return
     */
    public static ExecutorService build(Properties properties, String prefix) {
        return build(
                PropertyUtils.getNumber(properties, prefix + "core-pool-size", 1, Integer.class),
                PropertyUtils.getNumber(properties, prefix + "maximum-pool-size", 10, Integer.class),
                PropertyUtils.getNumber(properties, prefix + "queue-capacity", Integer.class),
                PropertyUtils.getNumber(properties, prefix + "keep-alive-time", 60000L, Long.class),
                TimeUnit.valueOf(properties.getProperty(prefix + "keep-alive-time-unit", TimeUnit.MILLISECONDS.name())),
                PropertyUtils.getBoolean(properties, prefix + "allow-core-thread-timeout", false)
                );
    }

    public static ExecutorService build(Scope config, String prefix) {
        return build(
                config.getInt(prefix + "core-pool-size", 1),
                config.getInt(prefix + "maximum-pool-size", 10),
                config.getInt(prefix + "queue-capacity"),
                config.getLong(prefix + "keep-alive-time", 60000L),
                TimeUnit.valueOf(config.get(prefix + "keep-alive-time-unit", TimeUnit.MILLISECONDS.name())),
                config.getBoolean(prefix + "allow-core-thread-timeout", false)
                );
    }

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueCapacity
     * @param keepAliveTime
     * @param keepAliveTimeUnit
     * @param allowCoreThreadTimeout
     * @return
     */
    public static ExecutorService build(int corePoolSize, int maximumPoolSize, Integer queueCapacity, long keepAliveTime, TimeUnit keepAliveTimeUnit, boolean allowCoreThreadTimeout) {
        if (corePoolSize == maximumPoolSize) {
            return queueCapacity != null ? build(corePoolSize, queueCapacity) : build(corePoolSize);
        }
        return queueCapacity != null ? build(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, allowCoreThreadTimeout, queueCapacity)
                : build(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, allowCoreThreadTimeout);
    }

    private static ExecutorService build(int nbThreads) {
        return Executors.newFixedThreadPool(nbThreads);
    }

    private static ExecutorService build(int nbThreads, int queueCapacity) {
        return new ThreadPoolExecutor(nbThreads, nbThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity));
    }

    private static ExecutorService build(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeout) {
        return build(corePoolSize, maximumPoolSize, keepAliveTime, unit, allowCoreThreadTimeout, new SynchronousQueue<>());
    }

    private static ExecutorService build(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeout, int queueCapacity) {
        return build(corePoolSize, maximumPoolSize, keepAliveTime, unit, allowCoreThreadTimeout, new LinkedBlockingQueue<>(queueCapacity));
    }

    private static ExecutorService build(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeout, BlockingQueue<Runnable> workQueue) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        executor.allowCoreThreadTimeOut(allowCoreThreadTimeout);
        return executor;
    }

}
