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

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Environment component. This component provides "global settings"
 * to all services, like the application id and the thread pool.
 * @since 3.0
 *
 * This component needs to be immediate to set the global variables
 * (application id and thread pool).
 */
public class EnvironmentComponent implements ManagedService {

    /**
     * Our thread pool.
     */
    private volatile ThreadPool threadPool;

    /** Sling settings service. */
    private volatile SlingSettingsService settingsService;

    /**
     * Activate this component.
     */
    protected void activate() {
        // Set the application id and the thread pool
        Environment.APPLICATION_ID = this.settingsService.getSlingId();
        Environment.THREAD_POOL = this.threadPool;
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate() {
        // Unset the thread pool
        if ( Environment.THREAD_POOL == this.threadPool ) {
            Environment.THREAD_POOL = null;
        }
    }

    protected void bindThreadPool(final EventingThreadPool etp) {
        this.threadPool = etp;
    }

    protected void unbindThreadPool(final EventingThreadPool etp) {
        if ( this.threadPool == etp ) {
            this.threadPool = null;
        }
    }

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		// TODO Auto-generated method stub
		
	}
}
