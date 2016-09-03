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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.support.TopicMatcher;
import org.apache.sling.event.impl.support.TopicMatcherHelper;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalQueueConfiguration
    implements QueueConfiguration, Comparable<InternalQueueConfiguration>, ManagedService {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The name of the queue. */
    private String name;

    /** The queue type. */
    private Type type;

    /** Number of retries. */
    private int retries;

    /** Retry delay. */
    private long retryDelay;

    /** Thread priority. */
    private ThreadPriority priority;

    /** The maximum number of parallel processes (for non ordered queues) */
    private int maxParallelProcesses;

    /** The ordering. */
    private int serviceRanking;

    /** The matchers for topics. */
    private TopicMatcher[] matchers;

    /** The configured topics. */
    private String[] topics;

    /** Keep jobs. */
    private boolean keepJobs;

    /** Valid flag. */
    private boolean valid = false;

    /** Optional thread pool size. */
    private int ownThreadPoolSize;

    /** Prefer creation instance. */
    private boolean preferCreationInstance;

    private String pid;

	private Hashtable params;

    /**
     * Create a new configuration from a config
     */
    public static InternalQueueConfiguration fromConfiguration(final Map<String, Object> params) {
        final InternalQueueConfiguration c = new InternalQueueConfiguration();
        c.activate();
        return c;
    }

    public InternalQueueConfiguration() {
        // nothing to do, see activate
    }

    /**
     * Create a new queue configuration
     */

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.params = new Hashtable<>();
	}
	
    protected void activate() {
    	if (this.params == null)
    		this.params = new Hashtable<>();
    	
        this.name = PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_NAME), null);
        try {
            this.priority = ThreadPriority.valueOf(ThreadPriority.class,PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_PRIORITY), ConfigurationConstants.DEFAULT_PRIORITY));
        } catch ( final IllegalArgumentException iae) {
            logger.warn("Invalid value for queue priority. Using default instead of : {}", params.get(ConfigurationConstants.PROP_PRIORITY));
            this.priority = ThreadPriority.valueOf(ThreadPriority.class,ConfigurationConstants.DEFAULT_PRIORITY);
        }
        try {
            this.type = Type.valueOf(Type.class,PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_TYPE), ConfigurationConstants.DEFAULT_TYPE));
        } catch ( final IllegalArgumentException iae) {
            logger.error("Invalid value for queue type configuration: {}", params.get(ConfigurationConstants.PROP_TYPE));
            this.type = null;
        }
        this.retries = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_RETRIES), ConfigurationConstants.DEFAULT_RETRIES);
        this.retryDelay = PropertiesUtil.toLong(params.get(ConfigurationConstants.PROP_RETRY_DELAY), ConfigurationConstants.DEFAULT_RETRY_DELAY);

        // Float values are treated as percentage.  int values are treated as number of cores, -1 == all available
        // Note: the value is based on the core count at startup.  It will not change dynamically if core count changes.
        int cores = ConfigurationConstants.NUMBER_OF_PROCESSORS;
        final double inMaxParallel = PropertiesUtil.toDouble(params.get(ConfigurationConstants.PROP_MAX_PARALLEL),
                ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        logger.debug("Max parallel for queue {} is {}", this.name, inMaxParallel);
        if ((inMaxParallel == Math.floor(inMaxParallel)) && !Double.isInfinite(inMaxParallel)) {
            // integral type
            if ((int) inMaxParallel == 0) {
                logger.warn("Max threads property for {} set to zero.", this.name);
            }
            this.maxParallelProcesses = (inMaxParallel <= -1 ? cores : (int) inMaxParallel);
        } else {
            // percentage (rounded)
            if ((inMaxParallel > 0.0) && (inMaxParallel < 1.0)) {
                this.maxParallelProcesses = (int) Math.round(cores * inMaxParallel);
            } else {
                logger.warn("Invalid queue max parallel value for queue {}. Using {}", this.name, cores);
                this.maxParallelProcesses =  cores;
            }
        }
        logger.debug("Thread pool size for {} was set to {}", this.name, this.maxParallelProcesses);

        // ignore parallel setting for ordered queues
        if ( this.type == Type.ORDERED ) {
            this.maxParallelProcesses = 1;
        }
        final String[] topicsParam = PropertiesUtil.toStringArray(params.get(ConfigurationConstants.PROP_TOPICS));
        this.matchers = TopicMatcherHelper.buildMatchers(topicsParam);
        if ( this.matchers == null ) {
            this.topics = null;
        } else {
            this.topics = topicsParam;
        }
        this.keepJobs = PropertiesUtil.toBoolean(params.get(ConfigurationConstants.PROP_KEEP_JOBS), ConfigurationConstants.DEFAULT_KEEP_JOBS);
        this.serviceRanking = PropertiesUtil.toInteger(params.get(Constants.SERVICE_RANKING), 0);
        this.ownThreadPoolSize = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_THREAD_POOL_SIZE), ConfigurationConstants.DEFAULT_THREAD_POOL_SIZE);
        this.preferCreationInstance = PropertiesUtil.toBoolean(params.get(ConfigurationConstants.PROP_PREFER_RUN_ON_CREATION_INSTANCE), ConfigurationConstants.DEFAULT_PREFER_RUN_ON_CREATION_INSTANCE);
        this.pid = (String)params.get(Constants.SERVICE_PID);
        this.valid = this.checkIsValid();
    }

    /**
     * Check if this configuration is valid,
     * If it is invalid, it is ignored.
     */
    private boolean checkIsValid() {
        if ( type == null ) {
            return false;
        }
        boolean hasMatchers = false;
        if ( this.matchers != null ) {
            for(final TopicMatcher m : this.matchers ) {
                if ( m != null ) {
                    hasMatchers = true;
                    break;
                }
            }
        }
        if ( !hasMatchers ) {
            return false;
        }
        if ( name == null || name.length() == 0 ) {
            return false;
        }
        if ( retries < -1 ) {
            return false;
        }
        if ( maxParallelProcesses < 1 ) {
            return false;
        }
        return true;
    }

    public boolean isValid() {
        return this.valid;
    }

    /**
     * Check if the queue processes the event.
     * @param topic The topic of the event
     * @return The queue name or <code>null</code>
     */
    public String match(final String topic) {
        if ( this.matchers != null ) {
            for(final TopicMatcher m : this.matchers ) {
                if ( m != null ) {
                    final String rep = m.match(topic);
                    if ( rep != null ) {
                        return this.name.replace("{0}", rep);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the name of the queue.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRetryDelayInMs()
     */
    @Override
    public long getRetryDelayInMs() {
        return this.retryDelay;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxRetries()
     */
    @Override
    public int getMaxRetries() {
        return this.retries;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getType()
     */
    @Override
    public Type getType() {
        return this.type;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxParallel()
     */
    @Override
    public int getMaxParallel() {
        return this.maxParallelProcesses;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getTopics()
     */
    @Override
    public String[] getTopics() {
        return this.topics;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRanking()
     */
    @Override
    public int getRanking() {
        return this.serviceRanking;
    }

    public String getPid() {
        return this.pid;
    }

    @Override
    public boolean isKeepJobs() {
        return this.keepJobs;
    }

    @Override
    public int getOwnThreadPoolSize() {
        return this.ownThreadPoolSize;
    }

    @Override
    public boolean isPreferRunOnCreationInstance() {
        return this.preferCreationInstance;
    }

    @Override
    public String toString() {
        return "Queue-Configuration(" + this.hashCode() + ") : {" +
            "name=" + this.name +
            ", type=" + this.type +
            ", topics=" + (this.matchers == null ? "[]" : Arrays.toString(this.matchers)) +
            ", maxParallelProcesses=" + this.maxParallelProcesses +
            ", retries=" + this.retries +
            ", retryDelayInMs=" + this.retryDelay +
            ", keepJobs=" + this.keepJobs +
            ", preferRunOnCreationInstance=" + this.preferCreationInstance +
            ", ownThreadPoolSize=" + this.ownThreadPoolSize +
            ", serviceRanking=" + this.serviceRanking +
            ", pid=" + this.pid +
            ", isValid=" + this.isValid() + "}";
    }

    @Override
    public int compareTo(final InternalQueueConfiguration other) {
        if ( this.serviceRanking < other.serviceRanking ) {
            return 1;
        } else if ( this.serviceRanking > other.serviceRanking ) {
            return -1;
        }
        return 0;
    }

    @Override
    public ThreadPriority getThreadPriority() {
        return this.priority;
    }
}
