package org.apache.sling.auth.sso.cas.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.form.impl.AuthenticationFormServlet;
import org.apache.sling.auth.trusted.token.api.TrustedTokenService;
import org.apache.sling.auth.trusted.token.api.cluster.ClusterTrackingService;
import org.apache.sling.auth.trusted.token.api.http.cache.DynamicContentResponseCache;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.internal.http.cache.DynamicContentResponseCacheImpl;
import org.apache.sling.auth.trusted.token.internal.memory.CacheManagerServiceImpl;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//CasAuthenticationHandler
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,CasAuthenticationHandler.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "CasAuthenticationHandler");
		
		properties.put(Constants.SERVICE_RANKING,-5);
	    properties.put(AuthenticationHandler.PATH_PROPERTY,"/");
	    properties.put(AuthenticationHandler.TYPE_PROPERTY,CasAuthenticationHandler.AUTH_TYPE);
	    properties.put(CasAuthenticationHandler.LOGIN_URL,CasAuthenticationHandler.DEFAULT_LOGIN_URL);
	    properties.put(CasAuthenticationHandler.LOGOUT_URL,CasAuthenticationHandler.DEFAULT_LOGOUT_URL);
	    properties.put(CasAuthenticationHandler.SERVER_URL,CasAuthenticationHandler.DEFAULT_SERVER_URL);
	    properties.put(CasAuthenticationHandler.RENEW,CasAuthenticationHandler.DEFAULT_RENEW);
	    properties.put(CasAuthenticationHandler.GATEWAY,CasAuthenticationHandler.DEFAULT_GATEWAY);
		Component component = dm.createComponent()
				.setInterface(new String[]{CasAuthenticationHandler.class.getName(),AuthenticationHandler.class.getName(),AuthenticationFeedbackHandler.class.getName()}, properties)
				.setImplementation(CasAuthenticationHandler.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(SlingRepository.class)
	                	.setRequired(true))
	            ;
		 dm.add(component);
		 
		//CasLoginModulePlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,CasLoginModulePlugin.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[] {LoginModulePlugin.class.getName()}, properties)
				.setImplementation(CasLoginModulePlugin.class)
	            ;
		dm.add(component);
		
		//CasLoginServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,CasLoginServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling CAS Based Authentication Handler");
		properties.put("sling.servlet.paths","/system/sling/cas/login");
		properties.put("sling.servlet.methods",new String[]{"GET","POST"});
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(CasLoginServlet.class)
	            .add(createServiceDependency()
	                	.setService(CasAuthenticationHandler.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(TrustedTokenService.class)
	                	.setRequired(true))	            
		;
		 dm.add(component);
	}

}