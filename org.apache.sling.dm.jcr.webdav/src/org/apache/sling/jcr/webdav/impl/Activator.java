package org.apache.sling.jcr.webdav.impl;


import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.jackrabbit.server.io.CopyMoveHandler;
import org.apache.jackrabbit.server.io.DeleteHandler;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.impl.debug.RequestProgressTrackerLogFilter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.webdav.impl.handler.DefaultHandlerService;
import org.apache.sling.jcr.webdav.impl.handler.DirListingExportHandlerService;
import org.apache.sling.jcr.webdav.impl.servlets.SlingWebDavServlet;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//DefaultHandlerService
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,DefaultHandlerService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put(Constants.SERVICE_RANKING, 1000);
	    properties.put(SlingWebDavServlet.TYPE_COLLECTIONS,SlingWebDavServlet.TYPE_COLLECTIONS_DEFAULT);
	    properties.put(SlingWebDavServlet.TYPE_NONCOLLECTIONS, SlingWebDavServlet.TYPE_NONCOLLECTIONS_DEFAULT);
	    properties.put(SlingWebDavServlet.TYPE_CONTENT, SlingWebDavServlet.TYPE_CONTENT_DEFAULT);
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),IOHandler.class.getName(),PropertyHandler.class.getName(),CopyMoveHandler.class.getName(),DeleteHandler.class.getName()}, properties)
				.setImplementation(DefaultHandlerService.class)
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
	            ;
		dm.add(component);
		
		//DirListingExportHandlerService
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,DirListingExportHandlerService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put(Constants.SERVICE_RANKING, 100);
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName()}, properties)
				.setImplementation(DirListingExportHandlerService.class)
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
	            ;
		dm.add(component);		
		
		
		//SlingWebDavServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingWebDavServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Sling WebDAV Servlet");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(SlingWebDavServlet.class)
				.add(createServiceDependency().setService(IOHandler.class)
						.setCallbacks("bindIOHandler", "unbindIOHandler")
						.setRequired(false))
				.add(createServiceDependency().setService(PropertyHandler.class)
						.setCallbacks("bindPropertyHandler", "unbindPropertyHandler")
						.setRequired(false))
				.add(createServiceDependency().setService(CopyMoveHandler.class)
						.setCallbacks("bindCopyMoveHandler", "unbindCopyMoveHandler")
						.setRequired(false))	
				.add(createServiceDependency().setService(DeleteHandler.class)
						.setCallbacks("bindDeleteHandler", "unbindDeleteHandler")
						.setRequired(false))	
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
				.add(createServiceDependency().setService(HttpService.class)
						.setRequired(true))
				.add(createServiceDependency().setService(MimeTypeService.class)
						.setRequired(true))
	            ;
		dm.add(component);		
	}

}