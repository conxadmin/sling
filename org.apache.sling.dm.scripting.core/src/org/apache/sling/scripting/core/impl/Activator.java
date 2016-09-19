package org.apache.sling.scripting.core.impl;


import java.io.Serializable;
import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;
import org.apache.sling.settings.SlingSettingsService;
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
		//BindingsValuesProvidersByContextImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,BindingsValuesProvidersByContextImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(new String[]{org.apache.sling.scripting.api.BindingsValuesProvidersByContext.class.getName(),org.osgi.util.tracker.ServiceTrackerCustomizer.class.getName()}, properties)
				.setImplementation(BindingsValuesProvidersByContextImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingScriptEngineManager.class)
						.setRequired(true))
				.add(createServiceDependency().setService(EventAdmin.class)
						.setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            ;
		dm.add(component);
		
		//ScriptCacheConsolePlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ScriptCacheConsolePlugin.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put(Constants.SERVICE_DESCRIPTION,"Script Cache");
	    properties.put("felix.webconsole.label","scriptcache");
	    properties.put("felix.webconsole.title","Script Cache Status");
	    properties.put("felix.webconsole.category","Sling");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),ServletConfig.class.getName(),Serializable.class.getName()}, properties)
				.setImplementation(ScriptCacheConsolePlugin.class)
				.add(createServiceDependency().setService(org.apache.sling.scripting.api.ScriptCache.class)
						.setRequired(true))
				.add(createServiceDependency().setService(ScriptCache.class)
						.setRequired(true))
	            ;
		dm.add(component);
		
		//ScriptCacheImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ScriptCacheImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put(Constants.SERVICE_DESCRIPTION,"Script Cache");
	    properties.put("org.apache.sling.scripting.cache.size",65536);
	    properties.put("org.apache.sling.scripting.cache.additional_extensions","js");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ScriptCache.class.getName()}, properties)
				.setImplementation(ScriptCacheImpl.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ScriptEngineFactory.class)
						.setCallbacks("bindScriptEngineFactory", "unbindScriptEngineFactory")
						.setRequired(false))
				.add(createServiceDependency().setService(ResourceResolverFactory.class)
						.setRequired(true))
				.add(createServiceDependency().setService(ThreadPoolManager.class)
						.setRequired(true))
	            ;
		dm.add(component);


		//ScriptEngineManagerFactory
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ScriptEngineManagerFactory.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Object.class.getName()}, properties)
				.setImplementation(ScriptEngineManagerFactory.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ScriptEngineFactory.class)
						.setCallbacks("bindScriptEngineFactory", "unbindScriptEngineFactory")
						.setRequired(false))
	            ;
		dm.add(component);
		
		//SlingScriptAdapterFactory
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Default SlingScriptResolver");
		properties.put("adaptables","org.apache.sling.api.resource.Resource");
		properties.put("adapters",new String[]{"org.apache.sling.api.scripting.SlingScript","javax.servlet.Servlet"});
		properties.put("adapter.condition","If the resource's path ends in an extension registered by a script engine.");

		component = dm.createComponent()
				.setInterface(new String[]{org.apache.sling.api.adapter.AdapterFactory.class.getName(),org.apache.sling.commons.mime.MimeTypeProvider.class.getName()}, properties)
				.setImplementation(SlingScriptAdapterFactory.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingScriptEngineManager.class).setRequired(true))
				.add(createServiceDependency().setService(ScriptCache.class).setRequired(true))
				.add(createServiceDependency().setService(org.apache.sling.scripting.api.BindingsValuesProvidersByContext.class).setRequired(true))

	            ;
		dm.add(component);

	}

}