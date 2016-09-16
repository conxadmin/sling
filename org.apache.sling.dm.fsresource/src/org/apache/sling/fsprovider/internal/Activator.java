/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.fsprovider.internal;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;

public class Activator  extends DependencyActivatorBase {

    
	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {
	}

	@Override
	public void init(BundleContext context, DependencyManager dm) throws Exception {
		//QuartzScheduler
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,FsResourceProvider.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.description", "Sling Filesystem Resource Provider");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ResourceProvider.class.getName()}, properties)
				.setImplementation(FsResourceProvider.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(EventAdmin.class)
	                	.setRequired(true))
				;
		 dm.add(component);
	}
}
