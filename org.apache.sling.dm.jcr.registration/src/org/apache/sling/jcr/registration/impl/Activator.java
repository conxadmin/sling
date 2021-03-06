package org.apache.sling.jcr.registration.impl;


import java.util.Properties;

import javax.jcr.Repository;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//JndiRegistrationSupport
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,JndiRegistrationSupport.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "JNDI Repository Registration");
	    properties.put("java.naming.factory.initial","org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
	    properties.put("java.naming.provider.url","http://sling.apache.org");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),JndiRegistrationSupport.class.getName()}, properties)
				.setImplementation(JndiRegistrationSupport.class)
				.setCallbacks(null,"doActivate","doDeactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(Repository.class)
						.setCallbacks("bindRepository", "unbindRepository")
						.setRequired(false))
				.add(createServiceDependency().setService(LogService.class)
						.setCallbacks("bindLog", "unbindLog")
						.setRequired(false))
	            ;
		dm.add(component);
		
		//RmiRegistrationSupport
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "RMI based Repository Registration");
	    properties.put("port","1099");
		component = dm.createComponent()
				.setInterface(RmiRegistrationSupport.class.getName(), properties)
				.setImplementation(RmiRegistrationSupport.class)
				.setCallbacks(null,"doActivate","doDeactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(RmiRegistrationSupport.class.getName()))
				.add(createServiceDependency().setService(Repository.class)
						.setCallbacks("bindRepository", "unbindRepository")
						.setRequired(false))
				.add(createServiceDependency().setService(LogService.class)
						.setCallbacks("bindLog", "unbindLog")
						.setRequired(false))
	            ;
		dm.add(component);

	}

}