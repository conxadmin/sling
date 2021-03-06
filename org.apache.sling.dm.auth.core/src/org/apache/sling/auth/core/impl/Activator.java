package org.apache.sling.auth.core.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletRequestListener;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.impl.engine.EngineSlingAuthenticator;
import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
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
		//BundleAuthenticationRequirementImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,BundleAuthenticationRequirementImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(new String[]{BundleAuthenticationRequirement.class.getName()}, properties)
				.setImplementation(BundleAuthenticationRequirementImpl.class)
	            .add(createServiceDependency()
	                	.setService(Authenticator.class)
	                	.setRequired(true))
	            ;
		 dm.add(component);

		 
		 //LoginServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Servlet for logging in users through the authenticator service.");
		properties.put("sling.servlet.methods", "GET,POST");
		properties.put("sling.servlet.paths", LoginServlet.SERVLET_PATH);
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(LoginServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(Authenticator.class).setRequired(true));
		dm.add(component);	 
		
		 //LogoutServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,LogoutServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Servlet for logging out users through the authenticator service.");
		properties.put("sling.servlet.methods", "GET,POST" );
		properties.put("sling.servlet.paths", LogoutServlet.SERVLET_PATH);
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(LogoutServlet.class)
				.add(createServiceDependency().setService(Authenticator.class).setRequired(true));
		dm.add(component);	 

		 //SlingAuthenticator
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingAuthenticator.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "org.apache.sling.engine.impl.auth.SlingAuthenticator");
	    properties.put("auth.sudo.cookie","sling.sudo");
	    properties.put("auth.sudo.parameter","sudo");
	    properties.put("auth.annonymous",true);
	    properties.put("auth.http","preemptive");
	    properties.put("auth.http.realm","Sling (Development)");
	    properties.put("auth.uri.suffix","/j_security_check");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(), Authenticator.class.getName(), AuthenticationSupport.class.getName(), ServletRequestListener.class.getName()}, properties)
				.setImplementation(SlingAuthenticator.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true));
		dm.add(component);	
		
		//EngineSlingAuthenticator
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Request Authenticator (Legacy Bridge)");
		component = dm.createComponent()
				.setInterface(new String[]{org.apache.sling.engine.auth.Authenticator.class.getName()}, properties)
				.setImplementation(EngineSlingAuthenticator.class)
				.add(createServiceDependency().setService(org.apache.sling.api.auth.Authenticator.class).setRequired(true));
		dm.add(component);	
	}

}