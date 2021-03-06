package org.apache.sling.contrib.ldap.osgi;

import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.contrib.ldap.api.LdapConnectionManager;
import org.apache.sling.contrib.ldap.internal.PoolingLdapConnectionManager;
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
		//PoolingLdapConnectionManager
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,PoolingLdapConnectionManager.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "LDAP Connection Factory");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),LdapConnectionManager.class.getName()}, properties)
				.setImplementation(PoolingLdapConnectionManager.class)
				.add(createConfigurationDependency().setPid(PoolingLdapConnectionManager.PID))
	            ;
		dm.add(component);
	}
}