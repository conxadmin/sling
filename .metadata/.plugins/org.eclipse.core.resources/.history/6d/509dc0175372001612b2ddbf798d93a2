package org.apache.sling.dm.browser.internal;


import java.io.Serializable;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.browser.servlet.BrowserServlet;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//AdapterManagerImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,BrowserServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "JCR Browser");
		properties.put(Constants.SERVICE_DESCRIPTION, "JCR Browser Servlet");
		properties.put("sling.servlet.paths","/services/browser");
		Component component = dm.createComponent()
				.setInterface(new String[]{Serializable.class.getName(),ServletConfig.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(BrowserServlet.class)
	            .add(dm.createConfigurationDependency()
	            		.setPid(BrowserServlet.class.getName()))
				.add(dm.createServiceDependency()
	                	.setService(ResourceResolverFactory.class)
	                	.setRequired(true))
				.add(dm.createServiceDependency()
	                	.setService(MimeTypeService.class)
	                	.setRequired(true))
	            ;
		dm.add(component);
	}
}