package org.apache.sling.servlets.filter.internal;


import java.util.Properties;

import javax.servlet.Filter;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.amdatu.multitenant.Tenant;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		final ServiceReference<Tenant> tenantSR = dm.getBundleContext().getServiceReference(Tenant.class);
		String tenantPid = "";
		if (tenantSR != null)
			tenantPid = arg0.getService(tenantSR).getPID();
		
		//MultitenantServletRequestFilter
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,MultitenantServletRequestFilter.class.getName());
		properties.put(Constants.SERVICE_VENDOR,"Conxworks");
		properties.put("service.description","Sling Servlet Multitenant Filter");
	    properties.put("service.ranking",700);
	    properties.put("sling.filter.scope",new String[]{"REQUEST","ERROR"});
	    properties.put("osgi.http.whiteboard.filter.pattern","/"+tenantPid+"/*");
	    properties.put("osgi.http.whiteboard.context.select" ,"(osgi.http.whiteboard.context.name=*)");
	    properties.put("sling.tenant.pid",tenantPid);
		Component component = dm.createComponent()
				.setInterface(Filter.class.getName(), properties)
				.setImplementation(MultitenantServletRequestFilter.class)
				.setCallbacks("initComponent",null,null, null)//init, start, stop and destroy.
	            ;
		 dm.add(component);
	}
}