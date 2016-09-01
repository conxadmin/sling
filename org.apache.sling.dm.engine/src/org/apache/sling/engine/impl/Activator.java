package org.apache.sling.engine.impl;


import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.GenericServlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.engine.impl.log.RequestLogger;
import org.apache.sling.engine.impl.log.RequestLoggerService;
import org.apache.sling.engine.impl.parameters.RequestParameterSupportConfigurer;
import org.apache.sling.engine.impl.parameters.Util;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.RequestHistoryConsolePlugin;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//SlingMainServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Sling Servlet");

		Component component = dm.createComponent()
				.setInterface(GenericServlet.class.getName(), properties)
				.setImplementation(SlingMainServlet.class)
				.add(createConfigurationDependency()
	                	.setPid(SlingMainServlet.class.getName()))
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ErrorHandler.class)
						 .setCallbacks("setErrorHandler", "unsetErrorHandler").setRequired(true))
				.add(createServiceDependency().setService(ServletResolver.class)
						 .setCallbacks("setServletResolver", "unsetServletResolver").setRequired(true))
				.add(createServiceDependency().setService(MimeTypeService.class)
						 .setCallbacks("setMimeTypeService", "unsetMimeTypeService").setRequired(true))
				.add(createServiceDependency().setService(AuthenticationSupport.class)
						 .setCallbacks("setAuthenticationSupport", "unsetAuthenticationSupport").setRequired(true))				
	            .add(createConfigurationDependency()
	                	.setPid(SlingMainServlet.class.getName()))
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
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Request Logger");
	    properties.put(RequestLogger.PROP_ACCESS_LOG_ENABLED, true);
		component = dm.createComponent()
				.setInterface(Object.class.getName(), properties)
				.setImplementation(RequestLogger.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createConfigurationDependency()
	                	.setPid(RequestLogger.class.getName()));
		dm.add(component);		
		
		//RequestLoggerService
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Factory for configuration based request/access loggers");
		component = dm.createComponent()
				.setInterface(RequestLoggerService.class.getName(), properties)
				.setImplementation(RequestLoggerService.class)
				.setCallbacks(null,"setup","shutdown", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(RequestLoggerService.class.getName()))
	            ;
		dm.add(component);	
		
		//RequestParameterSupportConfigurer
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Configures Sling's request parameter handling.");
		component = dm.createComponent()
				.setInterface(RequestParameterSupportConfigurer.class.getName(), properties)
				.setImplementation(RequestParameterSupportConfigurer.class)
				.setCallbacks(null,"configure","configure", null)//init, start, stop and destroy.
				.add(createConfigurationDependency()
	                	.setPid(RequestParameterSupportConfigurer.class.getName()))
	            .add(createServiceDependency()
	                	.setService(org.apache.sling.settings.SlingSettingsService.class)
	                	.setRequired(true))				
	            ;
		dm.add(component);	
		
		//RequestProgressTrackerLogFilter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "RequestProgressTracker dump filter");
		properties.put(EngineConstants.SLING_FILTER_SCOPE, "RequestProgressTracker dump filter");
		properties.put(EngineConstants.SLING_FILTER_SCOPE, EngineConstants.FILTER_SCOPE_REQUEST);
		component = dm.createComponent()
				.setInterface(Filter.class.getName(), properties)
				.setImplementation(RequestParameterSupportConfigurer.class)
				.setCallbacks(null,"activate", null, null)//init, start, stop and destroy.
				.add(createConfigurationDependency()
	                	.setPid(RequestParameterSupportConfigurer.class.getName()))
	            ;
		dm.add(component);	
	}
}