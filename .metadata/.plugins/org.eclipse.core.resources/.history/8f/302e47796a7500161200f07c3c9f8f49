package org.apache.sling.discovery.commons;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.commons.providers.spi.base.OakBacklogClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//IdMapService
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,IdMapService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(IdMapService.class.getName(), properties)
				.setImplementation(IdMapService.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	             .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
		        .add(createServiceDependency()
		            	.setService(DiscoveryLiteConfig.class)
		            	.setRequired(true));
		 dm.add(component);
		 
		 //OakBacklogClusterSyncService
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,OakBacklogClusterSyncService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ ClusterSyncService.class.getName(), OakBacklogClusterSyncService.class.getName() }, properties)
				.setImplementation(OakBacklogClusterSyncService.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(IdMapService.class).setRequired(true))
				.add(createServiceDependency().setService(DiscoveryLiteConfig.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true));
		dm.add(component);	
		
		 //SyncTokenService
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,SyncTokenService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ ClusterSyncService.class.getName(), SyncTokenService.class.getName() }, properties)
				.setImplementation(SyncTokenService.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(DiscoveryLiteConfig.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true));
		dm.add(component);		
	}

}