package org.apache.sling.adapter.internal;


import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletRequestListener;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//AdapterManagerImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Sling Adapter Manager");
		Component component = dm.createComponent()
				.setInterface(AdapterManager.class.getName(), properties)
				.setImplementation(AdapterManagerImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(AdapterFactory.class)
						 .setCallbacks("bindAdapterFactory", "unbindAdapterFactory").setRequired(false))
	            .add(createServiceDependency()
	                	.setService(PackageAdmin.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(EventAdmin.class)
	                	.setRequired(true));
		 dm.add(component);
		 
		//AdapterWebConsolePlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Adapter Web Console Plugin");
	    properties.put("felix.webconsole.label","adapters");
	    properties.put("felix.webconsole.title","Sling Adapters");
	    properties.put("felix.webconsole.css","/adapters/res/ui/adapters.css");
	    properties.put("felix.webconsole.configprinter.modes","always");
	    properties.put("felix.webconsole.category","Sling");
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(AdapterWebConsolePlugin.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(AdapterFactory.class)
						 .setCallbacks("bindAdapterFactory", "unbindAdapterFactory").setRequired(false))
	            .add(createServiceDependency()
	                	.setService(PackageAdmin.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(EventAdmin.class)
	                	.setRequired(true));
		dm.add(component);
	}

}