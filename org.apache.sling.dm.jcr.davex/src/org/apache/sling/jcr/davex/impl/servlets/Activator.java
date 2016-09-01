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
		//DeleteAcesServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION ,"Sling JcrRemoting Servlet");
		Component component = dm.createComponent()
				.setInterface(SlingDavExServlet.class.getName(), properties)
				.setImplementation(SlingDavExServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(SlingDavExServlet.class.getName()))
				.add(createServiceDependency().setService(SlingRepository.class)
						.setCallbacks("bindRepository", "unbindRepository")
						.setRequired(false))
				.add(createServiceDependency().setService(org.osgi.service.http.HttpService.class)
						.setCallbacks("bindHttpService", "unbindHttpService")
						.setRequired(false))
				.add(createServiceDependency().setService(org.apache.sling.auth.core.AuthenticationSupport.class)
						.setCallbacks("bindAuthSupport", "unbindAuthSupport")
						.setRequired(false))
	            ;
		dm.add(component);
	}

}