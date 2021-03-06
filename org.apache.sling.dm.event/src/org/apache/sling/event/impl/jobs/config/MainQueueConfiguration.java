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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the configuration for the main queue.
 *
 */
public class MainQueueConfiguration implements ManagedService {

    public static final String MAIN_QUEUE_NAME = "<main queue>";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private InternalQueueConfiguration mainConfiguration;

	private Dictionary<String, ?> props;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    protected void activate() {
        this.update();
    }
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.props = properties;
	}

    /**
     * Configure this component.
     * @param props Configuration properties
     */
    protected void update() {
    	if (this.props != null) {
	        // create a new dictionary with the missing info and do some sanity puts
	        final Map<String, Object> queueProps =valueOf((Dictionary<String, Object>)this.props);// new HashMap<String, Object>(props);
	        queueProps.put(ConfigurationConstants.PROP_TOPICS, "*");
	        queueProps.put(ConfigurationConstants.PROP_NAME, MAIN_QUEUE_NAME);
	        queueProps.put(ConfigurationConstants.PROP_TYPE, InternalQueueConfiguration.Type.UNORDERED);
	
	        // check max parallel - this should never be lower than 2!
	        final int maxParallel = PropertiesUtil.toInteger(queueProps.get(ConfigurationConstants.PROP_MAX_PARALLEL),
	                ConfigurationConstants.DEFAULT_MAX_PARALLEL);
	        if ( maxParallel < 2 ) {
	            this.logger.debug("Ignoring invalid setting of {} for {}. Setting to minimum value: 2",
	                    maxParallel, ConfigurationConstants.PROP_MAX_PARALLEL);
	            queueProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, 2);
	        }
	        this.mainConfiguration = InternalQueueConfiguration.fromConfiguration(queueProps);
    	}
    }

    /**
     * Return the main queue configuration object.
     * @return The main queue configuration object.
     */
    public InternalQueueConfiguration getMainConfiguration() {
        return this.mainConfiguration;
    }
    
    public static <K, V> Map<K, V> valueOf(Dictionary<K, V> dictionary) {
    	  if (dictionary == null) {
    	    return null;
    	  }
    	  Map<K, V> map = new HashMap<K, V>(dictionary.size());
    	  Enumeration<K> keys = dictionary.keys();
    	  while (keys.hasMoreElements()) {
    	    K key = keys.nextElement();
    	    map.put(key, dictionary.get(key));
    	  }
    	  return map;
    	}
}
