package org.apache.sling.discovery.impl;


import java.net.URL;
import java.util.Properties;

import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.discovery.impl.cluster.ClusterViewChangeListener;
import org.apache.sling.discovery.impl.cluster.ClusterViewServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventHandler;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//DiscoveryServiceImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(new String[]{ DiscoveryService.class.getName(), DiscoveryServiceImpl.class.getName() }, properties)
				.setImplementation(DiscoveryServiceImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(TopologyEventListener.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(Scheduler.class)
	                	.setRequired(true))	
	            .add(createServiceDependency()
	                	.setService(HeartbeatHandler.class)
	                	.setRequired(true))	
	            .add(createServiceDependency()
	                	.setService(AnnouncementRegistry.class)
	                	.setRequired(true))	
	            .add(createServiceDependency()
	                	.setService(ClusterViewServiceImpl.class)
	                	.setRequired(true))	
	            .add(createServiceDependency()
	                	.setService(Config.class)
	                	.setRequired(true))	
	            .add(createServiceDependency()
	                	.setService(SyncTokenService.class)
	                	.setRequired(true))	
	            ;
		dm.add(component);


		//TopologyWebConsolePlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Web Console Plugin to display Background servlets and ExecutionEngine status");
		properties.put(WebConsoleConstants.PLUGIN_LABEL, TopologyWebConsolePlugin.LABEL);
		properties.put(WebConsoleConstants.PLUGIN_TITLE, TopologyWebConsolePlugin.TITLE);
		properties.put("felix.webconsole.category","Sling");
		properties.put("felix.webconsole.configprinter.modes", new String[]{"zip"});
	    
		component = dm.createComponent()
				.setInterface(new String[]  { TopologyEventListener.class.getName(), Servlet.class.getName() }, properties)
				.setImplementation(TopologyWebConsolePlugin.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency().setService(ClusterViewService.class).setRequired(true))
	            .add(createServiceDependency().setService(AnnouncementRegistry.class).setRequired(true))
	            .add(createServiceDependency().setService(ConnectorRegistry.class).setRequired(true))
	            .add(createServiceDependency().setService(SyncTokenService.class).setRequired(true))
	            .add(createServiceDependency().setService(Config.class).setRequired(true));
		dm.add(component);	

		//Config
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ Config.class.getName(), BaseConfig.class.getName(), DiscoveryLiteConfig.class.getName() }, properties)
				.setImplementation(Config.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            .add(createConfigurationDependency()
	                	.setPid(Config.class.getName()));
		dm.add(component);		
		
		//ClusterViewChangeListener
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(EventHandler.class.getName(), properties)
				.setImplementation(ClusterViewChangeListener.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            .add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
	            .add(createServiceDependency().setService(DiscoveryServiceImpl.class).setRequired(true))
	            .add(createServiceDependency().setService(Config.class).setRequired(true))
	            ;
		dm.add(component);	
		
		//ClusterViewServiceImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ClusterViewService.class.getName(), ClusterViewServiceImpl.class.getName()}, properties)
				.setImplementation(ClusterViewServiceImpl.class)
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(Config.class)
	                	.setRequired(true));
		dm.add(component);	
		
		//VotingHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(EventHandler.class.getName(), properties)
				.setImplementation(VotingHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(Config.class)
	                	.setRequired(true));
		dm.add(component);
		
		
		//HeartbeatHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(HeartbeatHandler.class.getName(), properties)
				.setImplementation(HeartbeatHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(SlingSettingsService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ConnectorRegistry.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(AnnouncementRegistry.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(Scheduler.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(Config.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(VotingHandler.class)
	                	.setRequired(true))	            
	            ;
		dm.add(component);
	}

}