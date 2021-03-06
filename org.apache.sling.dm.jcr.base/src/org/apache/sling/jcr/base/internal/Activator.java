package org.apache.sling.jcr.base.internal;


import java.util.Properties;

import javax.jcr.Repository;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//JcrInstaller
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,RepositoryPrinterProvider.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    
		Component component = dm.createComponent()
				.setInterface(RepositoryPrinterProvider.class.getName(), properties)
				.setImplementation(RepositoryPrinterProvider.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(Repository.class)
						.setCallbacks("bindRepository", "unbindRepository")
						.setRequired(false))
	            ;
		dm.add(component);

	}

}