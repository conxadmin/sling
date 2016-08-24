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
		properties.put(Constants.SERVICE_DESCRIPTION, "Default Login Form for Form Based Authentication");
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
	    properties.put(AuthenticationHandler.PATH_PROPERTY, "/");
	    properties.put(AuthenticationHandler.TYPE_PROPERTY, HttpServletRequest.FORM_AUTH);
	    properties.put(Constants.SERVICE_RANKING,0);

	    properties.put(LoginModuleFactory.JAAS_CONTROL_FLAG, "sufficient");
	    properties.put(LoginModuleFactory.JAAS_REALM_NAME,"jackrabbit.oak");
	    properties.put(LoginModuleFactory.JAAS_RANKING, 1000);
	    
	    properties.put(FormAuthenticationHandler.PAR_LOGIN_FORM, AuthenticationFormServlet.SERVLET_PATH);
	    properties.put(FormAuthenticationHandler.PAR_AUTH_STORAGE, FormAuthenticationHandler.DEFAULT_AUTH_STORAGE);
	    properties.put(FormAuthenticationHandler.PAR_AUTH_NAME, FormAuthenticationHandler.DEFAULT_AUTH_NAME);
	    properties.put(FormAuthenticationHandler.PAR_CREDENTIALS_ATTRIBUTE_NAME, FormAuthenticationHandler.DEFAULT_CREDENTIALS_ATTRIBUTE_NAME);
	    properties.put(FormAuthenticationHandler.PAR_AUTH_TIMEOUT, FormAuthenticationHandler.DEFAULT_AUTH_TIMEOUT);
	    properties.put(FormAuthenticationHandler.PAR_TOKEN_FILE, FormAuthenticationHandler.DEFAULT_TOKEN_FILE);
	    properties.put(FormAuthenticationHandler.PAR_TOKEN_FAST_SEED, FormAuthenticationHandler.DEFAULT_TOKEN_FAST_SEED);
	    properties.put(FormAuthenticationHandler.PAR_LOGIN_AFTER_EXPIRE, FormAuthenticationHandler.DEFAULT_LOGIN_AFTER_EXPIRE);
	    properties.put(FormAuthenticationHandler.PAR_AUTH_NAME, FormAuthenticationHandler.DEFAULT_AUTH_NAME);
	    properties.put(FormAuthenticationHandler.PAR_AUTH_NAME, FormAuthenticationHandler.DEFAULT_AUTH_NAME);
	    
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(FormAuthenticationHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true));
		dm.add(component);
	}

}