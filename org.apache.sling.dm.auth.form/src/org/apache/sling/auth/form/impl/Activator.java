package org.apache.sling.auth.form.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//AuthenticationFormServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,AuthenticationFormServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Form Based Authentication Handler");
		properties.put("sling.servlet.paths",AuthenticationFormServlet.SERVLET_PATH);
		properties.put("sling.auth.requirements","-" + AuthenticationFormServlet.SERVLET_PATH);
		Component component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(AuthenticationFormServlet.class);
		 dm.add(component);
		 
		//FormAuthenticationHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,FormAuthenticationHandler.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Form Based Authentication Handler");
		
		properties.put("path","/");
	    properties.put("authtype","FORM");
	    properties.put("service.ranking",0);
	    properties.put("jaas.controlFlag","sufficient");
	    properties.put("jaas.realmName","jackrabbit.oak");
	    properties.put("jaas.ranking","1000");
	    properties.put("form.login.form","/system/sling/form/login");
	    properties.put("form.auth.storage","cookie");
	    properties.put("form.auth.name","sling.formauth");
	    properties.put("form.credentials.name","sling.formauth");
	    properties.put("form.auth.timeout",30);
	    properties.put("form.token.file","cookie-tokens.bin");
	    properties.put("form.token.fastseed",false);
	    properties.put("form.use.include",false);
	    properties.put("form.onexpire.login",false);
	    
        
		component = dm.createComponent()
				.setInterface(new String[] {AuthenticationHandler.class.getName(),AuthenticationFeedbackHandler.class.getName()}, properties)
				.setImplementation(FormAuthenticationHandler.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
	}

}