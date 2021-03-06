package org.apache.sling.dm.jcr.jackrabbit.server.impl;

import org.apache.sling.dm.jcr.jackrabbit.api.ISlingServerRepositoryManagerHelper;
import org.apache.sling.jcr.jackrabbit.server.impl.AccessManagerFactoryTracker;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class SlingServerRepositoryManagerHelper implements ISlingServerRepositoryManagerHelper {
    /**
     * The name of the framework property containing the default sling context
     * name.
     */
    public static final String SLING_CONTEXT_DEFAULT = "sling.context.default";
    
    
    // the service tracker used by the PluggableDefaultLoginModule
    // this field is only set on the first call to getLoginModules()
    private ServiceTracker loginModuleTracker;

    // the tracking count when the moduleCache has been filled
    private int lastTrackingCount = -1;

    // the cache of login module services
    private LoginModulePlugin[] moduleCache;

    // empty list of login modules if there are none registered
    private LoginModulePlugin[] EMPTY = new LoginModulePlugin[0];

    // the name of the default sling context
    private String slingContext;
    private CustomAccessManagerFactoryTracker accessManagerFactoryTracker;


    /*
     * 
     * DM
     * 
     */
	private volatile BundleContext componentBundleContext;  
	
	private void initialize() {
    	//== Init accessManagerFactoryTracker
        // ensure the module cache is not set right now, this may
        // (theoretically) be non-null after the last bundle stop
        moduleCache = null;

        // check the name of the default context, nothing to do if none
        slingContext = componentBundleContext.getProperty(SLING_CONTEXT_DEFAULT);
        if (slingContext == null) {
            slingContext = "default";
        }
        
        if (accessManagerFactoryTracker == null) {
            accessManagerFactoryTracker = new CustomAccessManagerFactoryTracker(componentBundleContext);
        }
        accessManagerFactoryTracker.open();
	}

    private void uninitialize() {
        // drop module cache
        moduleCache = null;

        // close the loginModuleTracker
        if (loginModuleTracker != null) {
            loginModuleTracker.close();
            loginModuleTracker = null;
        }

        if (accessManagerFactoryTracker != null) {
            accessManagerFactoryTracker.close();
            accessManagerFactoryTracker = null;
        }
	}


	  /**
   * Returns the registered {@link LoginModulePlugin} services. If there are
   * no {@link LoginModulePlugin} services registered, this method returns an
   * empty array. <code>null</code> is never returned from this method.
   */
  @Override
  public LoginModulePlugin[] getLoginModules() {
      // fast track cache (cache first, since loginModuleTracker is only
      // non-null if moduleCache is non-null)
	  if (moduleCache == EMPTY)
		  moduleCache = null;
	  
	  if (moduleCache != null
          && lastTrackingCount == loginModuleTracker.getTrackingCount()) {
          return moduleCache;
      }
      // invariant: moduleCache is null or modules have changed

      // tracker may be null if moduleCache is null
      if (loginModuleTracker == null) {
          loginModuleTracker = new ServiceTracker(componentBundleContext,
              LoginModulePlugin.class.getName(), null);
          loginModuleTracker.open();
      }

      if (moduleCache == null || lastTrackingCount < loginModuleTracker.getTrackingCount()) {
          Object[] services = loginModuleTracker.getServices();
          if (services == null || services.length == 0) {
              moduleCache = EMPTY;
          } else {
              moduleCache = new LoginModulePlugin[services.length];
              System.arraycopy(services, 0, moduleCache, 0, services.length);
          }
          lastTrackingCount = loginModuleTracker.getTrackingCount();
      }

      // the module cache is now up to date
      return moduleCache;
  }

  @Override
  public  CustomAccessManagerFactoryTracker getAccessManagerFactoryTracker() {
      return accessManagerFactoryTracker;
  }	
}