package org.apache.sling.dm.jcr.jackrabbit.api;

import org.apache.sling.dm.jcr.jackrabbit.server.impl.CustomAccessManagerFactoryTracker;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

public interface ISlingServerRepositoryManagerHelper {
	  /**
     * Returns the registered {@link LoginModulePlugin} services. If there are
     * no {@link LoginModulePlugin} services registered, this method returns an
     * empty array. <code>null</code> is never returned from this method.
     */
    public LoginModulePlugin[] getLoginModules();

    public  CustomAccessManagerFactoryTracker getAccessManagerFactoryTracker();
}
