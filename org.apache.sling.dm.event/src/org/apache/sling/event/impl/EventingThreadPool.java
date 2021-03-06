/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.event.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


/**
 * The configurable eventing thread pool.
 */
public class EventingThreadPool implements ThreadPool, ManagedService {

    private ThreadPoolManager threadPoolManager;

    /** The real thread pool used. */
    private org.apache.sling.commons.threads.ThreadPool threadPool;

	private Dictionary<String, ?> props;

    public static final int DEFAULT_POOL_SIZE = 10;

    public static final String PROPERTY_POOL_SIZE = "minPoolSize";

    public EventingThreadPool() {
        // default constructor
    }

    public EventingThreadPool(final ThreadPoolManager tpm, final int poolSize) {
        this.threadPoolManager = tpm;
        this.configure(poolSize);
    }

    public void release() {
        this.deactivate();
    }
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.props = properties;
	}
    

    /**
     * Activate this component.
     */
    protected void activate() {
    	if (this.props == null)
    		this.props = new Hashtable<>();
    	
        final int maxPoolSize = PropertiesUtil.toInteger(props.get(PROPERTY_POOL_SIZE), DEFAULT_POOL_SIZE);
        this.configure(maxPoolSize);
    }

    private void configure(final int maxPoolSize) {
        final ModifiableThreadPoolConfig config = new ModifiableThreadPoolConfig();
        config.setMinPoolSize(maxPoolSize);
        config.setMaxPoolSize(config.getMinPoolSize());
        config.setQueueSize(-1); // unlimited
        config.setShutdownGraceful(true);
        config.setPriority(ThreadPriority.NORM);
        config.setDaemon(true);
        this.threadPool = threadPoolManager.create(config, "Apache Sling Job Thread Pool");
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate() {
        this.threadPoolManager.release(this.threadPool);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#execute(java.lang.Runnable)
     */
    @Override
    public void execute(final Runnable runnable) {
        threadPool.execute(runnable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getConfiguration()
     */
    @Override
    public ThreadPoolConfig getConfiguration() {
        return threadPool.getConfiguration();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getName()
     */
    @Override
    public String getName() {
        return threadPool.getName();
    }


}
