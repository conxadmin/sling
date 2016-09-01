/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.dm.jcr.jackrabbit.server.impl;

import org.apache.sling.dm.jcr.jackrabbit.server.plugins.security.CustomPluggableDefaultAccessManager;
import org.apache.sling.jcr.jackrabbit.server.impl.security.PluggableDefaultAccessManager;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPluginFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks the existence of an <code>AccessManagerPluginFactory</code>.
 */
public class CustomAccessManagerFactoryTracker extends ServiceTracker {

    private AccessManagerPluginFactory factory;
    private BundleContext bundleContext;
    private Set<CustomPluggableDefaultAccessManager> consumers = new HashSet<CustomPluggableDefaultAccessManager>();

    private static final Logger log = LoggerFactory.getLogger(CustomAccessManagerFactoryTracker.class);


    public CustomAccessManagerFactoryTracker(BundleContext bundleContext) {
        super(bundleContext, AccessManagerPluginFactory.class.getName(), null);
        this.bundleContext = bundleContext;
    }

    @Override
    public Object addingService(ServiceReference serviceReference) {
        log.info("AccessManager service added.");
        closeSessions();
        this.factory = (AccessManagerPluginFactory) bundleContext.getService(serviceReference);
        return super.addingService(serviceReference);
    }

    @Override
    public void removedService(ServiceReference serviceReference, Object o) {
        log.warn("AccessManager service removed.");
        this.factory = null;
        closeSessions();
        super.removedService(serviceReference, o);
    }

    private void closeSessions() {
        log.warn("Closing all sessions");
        // Make a copy of consumers list to avoid concurrent modification
        Set<CustomPluggableDefaultAccessManager> closing;
        synchronized (consumers) {
            closing = new HashSet<CustomPluggableDefaultAccessManager>(consumers);
        }
        for (CustomPluggableDefaultAccessManager consumer : closing) {
            try {
                consumer.endSession();
            } catch (Throwable throwable) {
                log.warn("Error closing a PluggableDefaultAccessManager", throwable);
            }
        }
    }

    @Override
    public void modifiedService(ServiceReference serviceReference, Object o) {
        log.debug("AccessManager service modified.");
        super.modifiedService(serviceReference, o);
        this.factory = (AccessManagerPluginFactory) o;
    }

    public AccessManagerPluginFactory getFactory(CustomPluggableDefaultAccessManager consumer) {
        log.debug("Registering PluggableDefaultAccessManager instance");
        synchronized (consumers) {
            this.consumers.add(consumer);
        }
        return factory;
    }

    public void unregister(CustomPluggableDefaultAccessManager consumer) {
        log.debug("Unregistering PluggableDefaultAccessManager instance");
        synchronized (consumers) {
            this.consumers.remove(consumer);
        }
    }
}
