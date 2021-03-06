package org.apache.sling.servlets.get.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.servlets.get.impl.impl.info.SlingInfoServlet;
import org.apache.sling.servlets.get.impl.version.VersionInfoServlet;
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
		//DefaultGetServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,DefaultGetServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("service.description","Default GET Servlet");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.prefix", -1);
	    properties.put("sling.servlet.methods",new String[]{"GET","HEAD"});
	    properties.put("index",false);
	    properties.put("index.files",new String[]{"index","index.html"});
	    properties.put("enable.html",true);
	    properties.put("enable.txt",true);
	    properties.put("enable.json",true);
	    properties.put("enable.xml",true);
	    properties.put("json.maximumresults",200);
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(DefaultGetServlet.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
	            ;
		 dm.add(component);
		 
		//RedirectServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,RedirectServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("service.description","Default GET Servlet");
	    properties.put("sling.servlet.resourceTypes","sling:redirect");
	    properties.put("sling.servlet.prefix", -1);
	    properties.put("sling.servlet.methods","GET");
	    properties.put("json.maximumresults",200);
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(RedirectServlet.class)
				.setCallbacks("init","activate",null, null)//init, start, stop and destroy.
	            ;
		 dm.add(component);
		 
		//VersionInfoServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,VersionInfoServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.selectors","V");
	    properties.put("sling.servlet.methods",new String[]{"GET"});
	    properties.put("sling.servlet.extensions","json");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(VersionInfoServlet.class)
	            ;
		 dm.add(component);		 
		 
		//SlingInfoServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingInfoServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Sling Info Servlet");
	    properties.put("sling.servlet.paths","/system/sling/info");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(SlingInfoServlet.class)
	            ;
		 dm.add(component);	
	}
}