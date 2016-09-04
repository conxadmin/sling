package org.apache.sling.scripting.jsp.internal;


import java.beans.EventHandler;
import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestListener;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.impl.engine.EngineSlingAuthenticator;
import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.core.impl.BindingsValuesProvidersByContextImpl;
import org.apache.sling.scripting.jsp.JspScriptEngineFactory;
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
		//RhinoJavaScriptEngineFactory
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,JspScriptEngineFactory.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"JSP Script Handler");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ScriptEngineFactory.class.getName(),EventHandler.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(JspScriptEngineFactory.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency().setService(ServletContext.class).setRequired(true)
	            		.setCallbacks("bindSlingServletContext", "unbindSlingServletContext"))
	            .add(createServiceDependency().setService(ClassLoaderWriter.class).setRequired(true))
	            .add(createServiceDependency().setService(JavaCompiler.class).setRequired(true))
	            .add(createServiceDependency()
	                	.setService(DynamicClassLoaderManager.class)
	                	.setCallbacks("bindDynamicClassLoaderManager", "unbindDynamicClassLoaderManager")
	                	.setRequired(false))
	            ;
		 dm.add(component);
	}

}