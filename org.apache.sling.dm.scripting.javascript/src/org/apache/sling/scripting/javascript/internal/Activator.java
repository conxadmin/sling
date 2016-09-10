package org.apache.sling.scripting.javascript.internal;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.javascript.RhinoHostObjectProvider;
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
		//RhinoJavaScriptEngineFactory
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,RhinoJavaScriptEngineFactory.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Javascript engine based on Rhino");

		
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),RhinoJavaScriptEngineFactory.class.getName()}, properties)
				.setImplementation(RhinoJavaScriptEngineFactory.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(RhinoHostObjectProvider.class)
	                	.setCallbacks("addHostObjectProvider", "removeHostObjectProvider")
	                	.setRequired(false))
	            .add(createServiceDependency()
	                	.setService(DynamicClassLoaderManager.class)
	                	.setCallbacks("bindDynamicClassLoaderManager", "unbindDynamicClassLoaderManager")
	                	.setRequired(false))
	            ;
		 dm.add(component);

		 
	}

}