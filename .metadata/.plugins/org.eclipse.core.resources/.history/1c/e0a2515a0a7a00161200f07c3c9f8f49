package org.apache.sling.servlets.resolver.internal;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultErrorHandlerServlet;
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
		//SlingServletResolver
		
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingServletResolver.class.getName());
		properties.put(Constants.SERVICE_VENDOR,"The Apache Software Foundation");
		properties.put("service.description","Sling Servlet Resolver and Error Handler");
		properties.put("event.topics", "org/apache/sling/api/resource/Resource/*,org/apache/sling/api/resource/ResourceProvider/*,javax/script/ScriptEngineFactory/*,org/apache/sling/api/adapter/AdapterFactory/*,org/apache/sling/scripting/core/BindingsValuesProvider/*");
	    
		Component component = dm.createComponent()
				.setInterface(new String[]{/*ManagedService.class.getName(), */ServletResolver.class.getName(), SlingScriptResolver.class.getName(), ErrorHandler.class.getName(), SlingRequestListener.class.getName()}, properties)
				.setImplementation(SlingServletResolver.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(Servlet.class)
						.setCallbacks("bindServlet", "bindServlet")
						.setRequired(false))
	            ;
		 dm.add(component);
		 
		//DefaultErrorHandlerServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR,"The Apache Software Foundation");
		properties.put("sling.servlet.paths","sling/servlet/errorhandler/default");
		properties.put("sling.servlet.prefix",-1);
	    
		component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(DefaultErrorHandlerServlet.class)
	            ;
		 dm.add(component);
	}
}