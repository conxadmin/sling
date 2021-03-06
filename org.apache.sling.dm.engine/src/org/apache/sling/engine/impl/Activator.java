package org.apache.sling.engine.impl;


import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.GenericServlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.engine.impl.debug.RequestProgressTrackerLogFilter;
import org.apache.sling.engine.impl.log.RequestLogger;
import org.apache.sling.engine.impl.log.RequestLoggerFilter;
import org.apache.sling.engine.impl.log.RequestLoggerService;
import org.apache.sling.engine.impl.parameters.RequestParameterSupportConfigurer;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.amdatu.multitenant.Tenant;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		final ServiceReference<Tenant> tenantSR = dm.getBundleContext().getServiceReference(Tenant.class);
		String tenantPid = "";
		if (tenantSR != null)
			tenantPid = arg0.getService(tenantSR).getPID();
		
		//RequestProgressTrackerLogFilter
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,RequestProgressTrackerLogFilter.Config.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "RequestProgressTracker dump filter");
		properties.put("minDurationMs",0);
		properties.put("maxDurationMs",2147483647);
		properties.put("sling.filter.scope","REQUEST");
		properties.put("compactLogFormat",false);
		Component component = dm.createComponent()
				.setInterface(Filter.class.getName(), properties)
				.setImplementation(RequestProgressTrackerLogFilter.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(org.apache.sling.settings.SlingSettingsService.class)
	                	.setRequired(true))
	            ;

		//SlingMainServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingMainServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Sling Servlet");
		
		properties.put(SlingMainServlet.PROP_CONTEXT_PATH_PREFIX,tenantPid);
	    properties.put("sling.max.calls",1000);
	    properties.put("sling.max.inclusions",50);
	    properties.put("sling.trace.allow",false);
	    properties.put("sling.max.record.requests",20);
	    properties.put("sling.additional.response.headers","X-Content-Type-Options=nosniff,X-Frame-Options=SAMEORIGIN");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),GenericServlet.class.getName()}, properties)
				.setImplementation(SlingMainServlet.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ErrorHandler.class)
						 .setCallbacks("setErrorHandler", "unsetErrorHandler").setRequired(false))
				.add(createServiceDependency().setService(ServletResolver.class)
						 .setCallbacks("setServletResolver", "unsetServletResolver").setRequired(false))
				.add(createServiceDependency().setService(MimeTypeService.class)
						 .setCallbacks("setMimeTypeService", "unsetMimeTypeService").setRequired(false))
				.add(createServiceDependency().setService(AuthenticationSupport.class)
						 .setCallbacks("setAuthenticationSupport", "unsetAuthenticationSupport").setRequired(false))				
				.add(createServiceDependency().setService(org.apache.sling.api.adapter.AdapterManager.class)
						 .setCallbacks("bindAdapterManager", "unbindAdapterManager").setRequired(false))
				;
		dm.add(component);


		//SlingSettingsServiceImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(SlingSettingsService.class.getName(), properties)
				.setImplementation(SlingSettingsServiceImpl.class)
	            ;
		dm.add(component);	

		//RequestLogger
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,RequestLogger.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Request Logger");
	    properties.put(RequestLogger.PROP_ACCESS_LOG_ENABLED, true);
	    
	    properties.put("request.log.output","logs/request.log");
	    properties.put("request.log.outputtype",0);
	    properties.put("request.log.enabled",true);
	    properties.put("access.log.output","logs/access.log");
	    properties.put("access.log.outputtype",0);
	    properties.put("access.log.enabled",true);
	    
	    
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Object.class.getName()}, properties)
				.setImplementation(RequestLogger.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
	            ;
		dm.add(component);		
		
		//RequestLoggerService
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,RequestLoggerService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Factory for configuration based request/access loggers");
		
	    properties.put("request.log.service.output","request.log");
	    properties.put("request.log.service.outputtype",0);
	    properties.put("request.log.service.onentry",false);
	    
		component = dm.createComponent()
				.setInterface(RequestLoggerService.class.getName(), properties)
				.setImplementation(RequestLoggerService.class)
				.setCallbacks("init","setup","shutdown", null)//init, start, stop and destroy.
	            ;
		dm.add(component);	
		
		//RequestParameterSupportConfigurer
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,RequestParameterSupportConfigurer.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Configures Sling's request parameter handling.");
		
	    properties.put("sling.default.parameter.encoding","ISO-8859-1");
	    properties.put("sling.default.max.parameters",10000);
	    properties.put("file.threshold",256000);
	    properties.put("file.max",-1);
	    properties.put("request.max",-1);
	    properties.put("sling.default.parameter.checkForAdditionalContainerParameters",false);
	    
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),RequestParameterSupportConfigurer.class.getName()}, properties)
				.setImplementation(RequestParameterSupportConfigurer.class)
				.setCallbacks(null,"configure","configure", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(org.apache.sling.settings.SlingSettingsService.class)
	                	.setRequired(true))				
	            ;
		dm.add(component);	
		
		//RequestLoggerFilter
        final String contextName = SlingMainServlet.SERVLET_CONTEXT_NAME +" "+tenantPid;
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,RequestLoggerFilter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(EngineConstants.SLING_FILTER_SCOPE, EngineConstants.FILTER_SCOPE_REQUEST);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextName + ")");
		properties.put(Constants.SERVICE_RANKING ,32768);
		properties.put(Constants.SERVICE_DESCRIPTION,"Request Logger Filter");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Filter.class.getName()}, properties)
				.setImplementation(RequestLoggerFilter.class)
	            ;
		dm.add(component);	
	}
}