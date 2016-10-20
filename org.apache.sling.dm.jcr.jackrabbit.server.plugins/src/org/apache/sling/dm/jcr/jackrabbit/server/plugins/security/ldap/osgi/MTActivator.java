package org.apache.sling.dm.jcr.jackrabbit.server.plugins.security.ldap.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;

import java.util.Properties;

import org.amdatu.multitenant.Tenant;
import org.apache.sling.contrib.ldap.api.LdapConnectionManager;
import org.apache.sling.dm.jcr.jackrabbit.server.plugins.security.ldap.LDAPAccessManagerPluginFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPluginFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

public class MTActivator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext context, DependencyManager dm) throws Exception {
	}

	@Override
	public void init(BundleContext context, DependencyManager dm) throws Exception {
        try {
			Properties properties = new Properties();
			properties.put(Constants.SERVICE_PID,LDAPAccessManagerPluginFactory.class.getName());
			dm.add(createComponent().setInterface(AccessManagerPluginFactory.class.getName(), null)
					.setImplementation(LDAPAccessManagerPluginFactory.class)
		            .add(createServiceDependency()
		                	.setService(SlingRepository.class)
		                	.setRequired(true))
		            .add(createServiceDependency()
		                	.setService(LdapConnectionManager.class)
		                	.setRequired(true)))
		            ;     
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();
		}
	}
}