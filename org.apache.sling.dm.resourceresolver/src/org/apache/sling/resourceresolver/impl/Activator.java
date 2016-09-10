package org.apache.sling.resourceresolver.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.legacy.LegacyResourceProviderWhiteboard;
import org.apache.sling.resourceresolver.impl.observation.OsgiObservationBridge;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//ResourceAccessSecurityTracker
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(ResourceAccessSecurityTracker.class.getName(), properties)
				.setImplementation(ResourceAccessSecurityTracker.class)
				.add(createServiceDependency().setService(ResourceAccessSecurity.class,"(access.context=application)")
						 .setCallbacks("bindApplicationResourceAccessSecurity", "unbindApplicationResourceAccessSecurity").setRequired(false))
				.add(createServiceDependency().setService(ResourceAccessSecurity.class,"(access.context=provider)")
						 .setCallbacks("bindProviderResourceAccessSecurity", "unbindProviderResourceAccessSecurity").setRequired(false))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            ;
		dm.add(component);


		//ResourceResolverFactoryActivator
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ResourceResolverFactoryActivator.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Apache Sling Resource Resolver Factory");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Object.class.getName()}, properties)
				.setImplementation(ResourceResolverFactoryActivator.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ResourceDecorator.class)
						 .setCallbacks("bindResourceDecorator", "unbindResourceDecorator").setRequired(false))
				.add(createServiceDependency().setService(EventAdmin.class)
						 .setRequired(true))		
				.add(createServiceDependency().setService(ServiceUserMapper.class)
						.setRequired(true))		
				.add(createServiceDependency().setService(ResourceAccessSecurityTracker.class)
						.setRequired(true))	
	            ;
		dm.add(component);	

		//LegacyResourceProviderWhiteboard
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(Object.class.getName(), properties)
				.setImplementation(LegacyResourceProviderWhiteboard.class)
				.add(createServiceDependency().setService(ResourceProvider.class)
						 .setCallbacks("bindResourceProvider", "unbindResourceProvider").setRequired(true))
				.add(createServiceDependency().setService(ResourceProviderFactory.class)
						 .setCallbacks("bindResourceProviderFactory", "unbindResourceProviderFactory").setRequired(true))
	            ;
		dm.add(component);		

		//OsgiObservationBridge
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,OsgiObservationBridge.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ResourceChangeListener.class.getName()}, properties)
				.setImplementation(OsgiObservationBridge.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);		
	}

}