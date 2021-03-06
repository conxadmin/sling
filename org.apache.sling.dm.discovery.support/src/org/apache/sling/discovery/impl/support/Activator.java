package org.apache.sling.discovery.impl.support;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//StandardPropertyProvider
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,StandardPropertyProvider.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(StandardPropertyProvider.class.getName(), properties)
				.setImplementation(StandardPropertyProvider.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(HttpService.class)
	                	.setCallbacks("bindHttpService", "unbindHttpService")
	                	.setRequired(false))
	            ;
		dm.add(component);
	}
}