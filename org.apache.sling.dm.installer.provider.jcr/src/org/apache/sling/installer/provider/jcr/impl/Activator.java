package org.apache.sling.installer.provider.jcr.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
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
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Sling JCR Install Service");
		properties.put(UpdateHandler.PROPERTY_SCHEMES, new String[]{JcrInstaller.URL_SCHEME});
		properties.put(Constants.SERVICE_RANKING,100);
	    
		Component component = dm.createComponent()
				.setInterface(UpdateHandler.class.getName(), properties)
				.setImplementation(JcrInstaller.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(JcrInstaller.class.getName()))
				.add(createServiceDependency().setService(SlingRepository.class).setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
				.add(createServiceDependency().setService(OsgiInstaller.class).setRequired(true))
	            ;
		dm.add(component);

	}

}