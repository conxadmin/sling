package org.apache.sling.scripting.jsp.internal;


import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.jsp.JspScriptEngineFactory;
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
		//JspScriptEngineFactory
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,JspScriptEngineFactory.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"JSP Script Handler");
		
	    properties.put("jasper.compilerTargetVM","auto");
	    properties.put("jasper.compilerSourceVM","auto");
	    properties.put("jasper.classdebuginfo",true);
	    properties.put("jasper.enablePooling",true);
	    properties.put("jasper.ieClassId","clsid:8AD9C840-044E-11D1-B3E9-00805F499D93");
	    properties.put("jasper.genStringAsCharArray",false);
	    properties.put("jasper.keepgenerated",true);
	    properties.put("jasper.mappedfile",true);
	    properties.put("jasper.trimSpaces",false);
	    properties.put("jasper.displaySourceFragments",false);
	    properties.put("event.topics","org/apache/sling/api/resource/*");
	    properties.put("felix.webconsole.label","slingjsp");
	    properties.put("felix.webconsole.title","JSP");
	    properties.put("felix.webconsole.category","Sling");
	    properties.put("default.is.session",true);
	    
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ScriptEngineFactory.class.getName(),org.osgi.service.event.EventHandler.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(JspScriptEngineFactory.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
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