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
package org.apache.sling.event.impl.jobs.config;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class InternalQueueConfigurationFactory
    implements ManagedServiceFactory {
	
	public static final String PID = "org.apache.sling.event.jobs.QueueConfiguration";
	
	private volatile DependencyManager dm;
	
	private final Map<String, Component> components = new ConcurrentHashMap<>();

	@Override
	public String getName() {
		return PID;
	}

	@Override
	public void updated(String pid, Dictionary properties) throws ConfigurationException {
    	Component component = null;
        synchronized (components) {
        	QueueConfiguration service = null;
            if (components.containsKey(pid)) {
            	service = (QueueConfiguration) components.get(pid).getInstance();
            } else {
                String type = (String) properties.get("type");

                if (type == null) {
                    throw new ConfigurationException("message",
                        "Required property 'type' missing");
                }
                
                service = new InternalQueueConfiguration();
        		component = dm.createComponent()
        				.setInterface(InternalQueueConfiguration.class.getName(), properties)
        				.setImplementation(service)
        				.setCallbacks(null,"configure","configure", null)//init, start, stop and destroy.
        	            ;
                components.put(pid, component);
            }
        }
        
        // Calling services from a synchronized block can lead to deadlocks,
        // so Dependency Manager must be called outside.
        if(component != null) {
        	dm.add(component);
        }
        
	}

	@Override
	public void deleted(String pid) {
        Component component = null;
        synchronized (components) {
            component = components.remove(pid);
        }

        // Calling services from a synchronized block can lead to deadlocks,
        // so Dependency Manager must be called outside.
        if(component != null) {
            dm.remove(component);
        }
	}
}
