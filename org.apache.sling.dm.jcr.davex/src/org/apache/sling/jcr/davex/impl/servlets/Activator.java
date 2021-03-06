package org.apache.sling.jcr.davex.impl.servlets;


import java.util.Properties;

import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//SlingDavExServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingDavExServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION ,"Sling JcrRemoting Servlet");
		properties.put("alias","/server");
		properties.put("dav.create-absolute-uri",true);
		properties.put("dav.protectedhandlers","org.apache.jackrabbit.server.remoting.davex.AclRemoveHandler");
		Component component = dm.createComponent()
				.setInterface(SlingDavExServlet.class.getName(), properties)
				.setImplementation(SlingDavExServlet.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
				.add(createServiceDependency().setService(org.osgi.service.http.HttpService.class)
						.setRequired(true))
				.add(createServiceDependency().setService(org.apache.sling.auth.core.AuthenticationSupport.class)
						.setRequired(true))
	            ;
		dm.add(component);
	}

}