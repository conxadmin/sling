package org.apache.sling.scripting.javascript.internal;


import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletRequestListener;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.impl.engine.EngineSlingAuthenticator;
import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.javascript.RhinoHostObjectProvider;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//RhinoJavaScriptEngineFactory
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Javascript engine based on Rhino");

		
		Component component = dm.createComponent()
				.setInterface(ScriptEngineFactory.class.getName(), properties)
				.setImplementation(RhinoJavaScriptEngineFactory.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(RhinoJavaScriptEngineFactory.class.getName()))
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