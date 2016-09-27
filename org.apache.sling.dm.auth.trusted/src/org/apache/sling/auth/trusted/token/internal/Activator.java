package org.apache.sling.auth.trusted.token.internal;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.trusted.token.api.TrustedTokenService;
import org.apache.sling.auth.trusted.token.api.cluster.ClusterTrackingService;
import org.apache.sling.auth.trusted.token.api.http.cache.DynamicContentResponseCache;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.internal.http.cache.DynamicContentResponseCacheImpl;
import org.apache.sling.auth.trusted.token.internal.memory.CacheManagerServiceImpl;
import org.apache.sling.jcr.api.SlingRepository;
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
		//TrustedAuthenticationHandler
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,TrustedAuthenticationHandler.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling TrustedAuthenticationHandler");
		properties.put("path","/");
		Component component = dm.createComponent()
				.setInterface(AuthenticationHandler.class.getName(), properties)
				.setImplementation(TrustedAuthenticationHandler.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(TrustedTokenService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(DynamicContentResponseCache.class)
	                	.setRequired(true))	            
	            ;
		 dm.add(component);
		 
		//TrustedAuthenticationServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,TrustedAuthenticationServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Trusted Authentication Servlet");
		properties.put("path","/system/trustedauth");
		properties.put("sakai.auth.trusted.destination.default","/dev");
		properties.put("sakai.auth.trusted.nouserlocationformat","/system/trustedauth-nouser?u={0}");
	    properties.put("sakai.auth.trusted.path.registration","/system/trustedauth");
        
		component = dm.createComponent()
				.setInterface(new String[] {Servlet.class.getName()}, properties)
				.setImplementation(TrustedAuthenticationServlet.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(SlingRepository.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(HttpService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(TrustedTokenService.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
		
		//TrustedAuthenticationServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,TrustedTokenServiceImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("sakai.auth.trusted.token.usesession",false);
		properties.put("sakai.auth.trusted.token.securecookie",false);
		properties.put("sakai.auth.trusted.token.ttl",1200000);
	    properties.put("sakai.auth.trusted.token.name","sakai-trusted-authn");
	    properties.put("sakai.auth.trusted.token.storefile","sling/cookie-keystore.bin");
	    properties.put("sakai.auth.trusted.server.secret","default-setting-change-before-use");
	    properties.put("sakai.auth.trusted.server.enabled",true);
	    properties.put("sakai.auth.trusted.server.safe-hostsaddress","localhost;127.0.0.1;0:0:0:0:0:0:0:1%0");
	    properties.put("sakai.auth.trusted.wrapper.class.names","org.sakaiproject.nakamura.formauth.FormAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.saml.SamlAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.rest.RestAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.cas.CasAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.http.usercontent.UserContentAuthenticationTokenServiceWrapper");
	    properties.put("sakai.auth.trusted.header","");
	    properties.put("sakai.auth.trusted.request-parameter","");
	    properties.put("sakai.auth.trusted.server.safe-authentication-addresses","");
	    properties.put("sakai.auth.trusted.token.debugcookies",false);
		component = dm.createComponent()
				.setInterface(new String[] {TrustedTokenService.class.getName()}, properties)
				.setImplementation(TrustedTokenServiceImpl.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(ClusterCookieServer.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(TokenStore.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ClusterTrackingService.class)
	                	.setRequired(false))
	            .add(createServiceDependency()
	                	.setService(CacheManagerService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(EventAdmin.class)
	                	.setRequired(true))
	            ;
		dm.add(component);	
		
		
		//=== Memory cache
		//CacheManagerServiceImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,CacheManagerServiceImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Cache Manager Service Implementation");
		properties.put("cache-config","sling/ehcacheConfig.xml");
		properties.put("bind-address","127.0.0.1");
	    
		component = dm.createComponent()
				.setInterface(new String[] {CacheManagerService.class.getName()}, properties)
				.setImplementation(CacheManagerServiceImpl.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(ClusterCookieServer.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(TokenStore.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(ClusterTrackingService.class)
	                	.setRequired(false))
	            .add(createServiceDependency()
	                	.setService(CacheManagerService.class)
	                	.setRequired(true))
	            .add(createServiceDependency()
	                	.setService(EventAdmin.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
		
		//=== Http cache
		//DynamicContentResponseCacheImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,DynamicContentResponseCacheImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Nakamura Dynamic Response Cache");
		properties.put("disable.cache.for.dev.mode",false);
		properties.put("bypass.cache.for.localhost",true);
		component = dm.createComponent()
				.setInterface(new String[] {CacheManagerService.class.getName()}, properties)
				.setImplementation(DynamicContentResponseCacheImpl.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(CacheManagerService.class))
	            ;
		dm.add(component);
	}

}