package org.apache.sling.discovery.base;


import java.util.HashMap;
import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistryImpl;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistryImpl;
import org.apache.sling.discovery.base.connectors.ping.TopologyConnectorServlet;
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
		//AnnouncementRegistryImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID, AnnouncementRegistryImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(AnnouncementRegistry.class.getName(), properties)
				.setImplementation(AnnouncementRegistryImpl.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))	      
	            .add(createServiceDependency()
	                	.setService(BaseConfig.class)
	                	.setRequired(true));
		dm.add(component);
	
		//ConnectorRegistry
		properties = new Properties();
		properties.put(Constants.SERVICE_PID, ConnectorRegistryImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(ConnectorRegistry.class.getName(), properties)
				.setImplementation(ConnectorRegistryImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(AnnouncementRegistry.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(BaseConfig.class)
	                	.setRequired(true));
		dm.add(component);	
		

		//TopologyConnectorServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID, TopologyConnectorServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(TopologyConnectorServlet.class.getName(), properties)
				.setImplementation(TopologyConnectorServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(AnnouncementRegistry.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ClusterViewService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(HttpService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(BaseConfig.class)
	                	.setRequired(true));
		dm.add(component);	
	}
}