package org.apache.sling.auth.trusted.token.internal;


import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.trusted.token.api.TrustedTokenService;
import org.apache.sling.auth.trusted.token.api.cluster.ClusterTrackingService;
import org.apache.sling.auth.trusted.token.api.http.cache.DynamicContentResponseCache;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.internal.cluster.ClusterTrackingFilter;
import org.apache.sling.auth.trusted.token.internal.cluster.ClusterTrackingServiceImpl;
import org.apache.sling.auth.trusted.token.internal.http.cache.DynamicContentResponseCacheImpl;
import org.apache.sling.auth.trusted.token.internal.memory.CacheManagerServiceImpl;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;

public class MTActivator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//TrustedAuthenticationServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,TrustedAuthenticationServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Trusted Authentication Servlet");
		properties.put("path","/system/trustedauth");
		properties.put("sakai.auth.trusted.destination.default","/dev");
		properties.put("sakai.auth.trusted.nouserlocationformat","/system/trustedauth-nouser?u={0}");
	    properties.put("sakai.auth.trusted.path.registration","/system/trustedauth");
        
		Component component = dm.createComponent()
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

		//=== Cluster
		//ClusterTrackingFilter
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ClusterTrackingFilter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Custer Tracking Filter");
		properties.put("filter.scope","request");
		properties.put("filter.order",new Integer[]{10});
		component = dm.createComponent()
				.setInterface(new String[] {Filter.class.getName()}, properties)
				.setImplementation(ClusterTrackingFilter.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
				.add(createServiceDependency()
	                	.setService(ClusterTrackingService.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
	}
}