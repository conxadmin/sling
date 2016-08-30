package org.apache.sling.auth.form.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
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
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Form Based Authentication Handler");
		properties.put("sling.servlet.paths","/system/sling/form/login");
		properties.put("sling.auth.requirements","-" + AuthenticationFormServlet.SERVLET_PATH);
		Component component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(AuthenticationFormServlet.class);
		 dm.add(component);
		 
		//FormAuthenticationHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Form Based Authentication Handler");
	    
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(FormAuthenticationHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createConfigurationDependency()
	            		.setPid(FormAuthenticationHandler.class.getName()))
				.add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
	}

}