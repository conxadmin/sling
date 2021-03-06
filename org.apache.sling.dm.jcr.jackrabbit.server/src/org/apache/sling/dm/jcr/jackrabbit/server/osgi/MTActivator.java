package org.apache.sling.dm.jcr.jackrabbit.server.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;

import java.util.Properties;

import org.amdatu.multitenant.Tenant;
import org.apache.sling.dm.jcr.jackrabbit.api.ISlingServerRepositoryManagerHelper;
import org.apache.sling.dm.jcr.jackrabbit.server.impl.SlingServerRepositoryManager;
import org.apache.sling.dm.jcr.jackrabbit.server.impl.SlingServerRepositoryManagerConfiguration;
import org.apache.sling.dm.jcr.jackrabbit.server.impl.SlingServerRepositoryManagerHelper;
import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

public class MTActivator extends DependencyActivatorBase {
   public static final String SERVER_REPOSITORY_FACTORY_PID = "org.conxworks.contentrepository.sling.jcr.jackrabbit.server.SlingServerRepository";
    // this bundle's context, used by verifyConfiguration
    private BundleContext bundleContext;

	@Override
	public void destroy(BundleContext context, DependencyManager dm) throws Exception {
	}

	@Override
	public void init(BundleContext context, DependencyManager dm) throws Exception {
        bundleContext = context;
        
        try {
			dm.add(createComponent().setInterface(ISlingServerRepositoryManagerHelper.class.getName(), null)
					.setImplementation(new SlingServerRepositoryManagerHelper())
					.setCallbacks(null, "initialize", "uninitialize", null)//init, start, stop and destroy.
					.add(createServiceDependency().setService(LogService.class).setRequired(false))
					);        
			
			Properties properties = new Properties();
			properties.put(Constants.SERVICE_PID,SlingServerRepositoryManagerConfiguration.class.getName());
			dm.add(createComponent().setInterface(ManagedService.class.getName(), null)
					.setImplementation(new SlingServerRepositoryManager())
					.setCallbacks("init" , "activate", "deactivate", null)//init, start, stop and destroy.
					.add(createServiceDependency().setService(LogService.class).setRequired(false))
					.add(createServiceDependency().setService(Tenant.class).setRequired(true))
					.add(createServiceDependency().setService(NamespaceMapper.class)
						 .setCallbacks("bindNamespaceMapper", "unbindNamespaceMapper").setRequired(false))
					.add(createServiceDependency().setService(ServiceUserMapper.class).setRequired(true))
					);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}