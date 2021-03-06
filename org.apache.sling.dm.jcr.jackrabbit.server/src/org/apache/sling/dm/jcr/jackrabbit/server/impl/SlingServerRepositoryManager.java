package org.apache.sling.dm.jcr.jackrabbit.server.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.management.DynamicMBean;
import javax.sql.DataSource;

import org.amdatu.multitenant.Tenant;
import org.apache.commons.io.IOUtils;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository2;
import org.apache.sling.jcr.base.AbstractSlingRepositoryManager;
import org.apache.sling.jcr.jackrabbit.server.impl.jmx.StatisticsMBeanImpl;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import aQute.bnd.maven.support.Pom.Dependency;

/**
 * The <code>SlingServerRepository</code> creates and configures <tt>Jackrabbit</tt> repository instances. 
 */
public class SlingServerRepositoryManager extends AbstractSlingRepositoryManager implements ManagedService {
    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";	    
    
	/*
	 * DM fields
	 * 
	 */
    private volatile DataSource  cnxJtaDataSource;
    private volatile BundleContext componentBundleContext;
    private volatile Tenant tenant;
    private volatile DependencyManager dm;
    private volatile ServiceUserMapper serviceUserMapper;
    
  
    /*
     * 
     * Other fields
     * 
     */
    
    /**
     * The name of the configuration property defining the file system directory
     * where the repository files are located (value is "home").
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     */
    //@Property(value = "")
    public static final String REPOSITORY_HOME_DIR = "home";

    //@Property(value = "")
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    // For backwards compatibility loginAdministrative is still enabled
    // In future releases, this default may change to false.
    public static final boolean DEFAULT_LOGIN_ADMIN_ENABLED = true;

    //@Property
    public static final String PROPERTY_DEFAULT_WORKSPACE = "defaultWorkspace";

    //@Property(boolValue = DEFAULT_LOGIN_ADMIN_ENABLED)
    public static final String PROPERTY_LOGIN_ADMIN_ENABLED = "admin.login.enabled";

    public static final String DEFAULT_ADMIN_USER = "admin";

    //@Property(value = DEFAULT_ADMIN_USER)
    public static final String PROPERTY_ADMIN_USER = "admin.name";
    
    private NamespaceMapper[] namespaceMappers;

    private String adminUserName;

    private Map<String, ServiceRegistration> statisticsServices = new ConcurrentHashMap<String, ServiceRegistration>();

    private Map<Long, NamespaceMapper> namespaceMapperRefs = new TreeMap<Long, NamespaceMapper>();


	private Dictionary<String, ?> properties;


	private Component component;


	private boolean startRepositoryInStandalone;
    
    
	public SlingServerRepositoryManager() {
		super();
	}
	
	// ----------------- ManagedService ------------------------------------
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

	// ----------------- DM methods/callbacks ------------------------------
	private void init(Component component) {
    	if (this.properties == null) {
    		this.properties = component.getServiceProperties();
    		if (this.properties == null)
    			this.properties = new Hashtable<>();
    	}
		final String dsName = PropertiesUtil.toString(properties.get(SlingServerRepositoryManagerConfiguration.DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME),SlingServerRepositoryManagerConfiguration.DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME);
		final String dsFilter = String.format("(name=%s)",dsName);
		component.add(dm.createServiceDependency().setService(DataSource.class,dsFilter).setRequired(true));
	}
	
	/**
     * This method must be called if overwritten by implementations !!
     */
    private void activate(Component component) throws Exception {
    	if (this.properties == null)
    		this.properties = component.getServiceProperties();
    	this.startRepositoryInStandalone = PropertiesUtil.toBoolean(this.properties.get(SlingServerRepositoryManagerConfiguration.START_REPOSITORY_AS_STANDALONE), SlingServerRepositoryManagerConfiguration.DEFAULT_START_REPOSITORY_AS_STANDALONE);

    	final String defaultWorkspace = null;//PropertiesUtil.toString(properties.get(PROPERTY_DEFAULT_WORKSPACE), null);
        final boolean disableLoginAdministrative = false;//!PropertiesUtil.toBoolean(properties.get(PROPERTY_LOGIN_ADMIN_ENABLED), DEFAULT_LOGIN_ADMIN_ENABLED);

        this.adminUserName = DEFAULT_ADMIN_USER;//PropertiesUtil.toString(properties.get(PROPERTY_ADMIN_USER), DEFAULT_ADMIN_USER);
        
        super.stop();//Reset first
        super.start(componentBundleContext, defaultWorkspace, disableLoginAdministrative);
    }

	/**
     * This method must be called if overwritten by implementations !!
     *
     * @param componentContext
     */
    private void deactivate() {
        super.stop();
        this.adminUserName = null;
        this.namespaceMapperRefs.clear();
        this.namespaceMappers = null;
    }  
    
    @SuppressWarnings("unused")
    private void bindNamespaceMapper(ServiceRegistration<NamespaceMapper> serviceRef, NamespaceMapper namespaceMapper) {
        synchronized (this.namespaceMapperRefs) {
            this.namespaceMapperRefs.put((Long)serviceRef.getReference().getProperty(Constants.SERVICE_ID), namespaceMapper);
            this.namespaceMappers = this.namespaceMapperRefs.values().toArray(
                    new NamespaceMapper[this.namespaceMapperRefs.size()]);
        }
    }

    @SuppressWarnings("unused")
    private void unbindNamespaceMapper(ServiceRegistration<NamespaceMapper> serviceRef, NamespaceMapper namespaceMapper) {
        synchronized (this.namespaceMapperRefs) {
            this.namespaceMapperRefs.remove(serviceRef.getReference().getProperty(Constants.SERVICE_ID));
            this.namespaceMappers = this.namespaceMapperRefs.values().toArray(
                    new NamespaceMapper[this.namespaceMapperRefs.size()]);
        }
    }      
    
    // ---------- Repository Management ----------------------------------------

    @Override
    protected Repository acquireRepository() {
        InputStream ins = null;
        try {

            RepositoryConfig crc;
    		URL url = FrameworkUtil
    				.getBundle(this.getClass())
    				.getBundleContext()
    				.getBundle()
    				.getResource("repository_template.xml");
    		InputStream input = url.openStream();
    		StringWriter writer = new StringWriter();
    		IOUtils.copy(input, writer);
    		input.close();
    		final String xml = writer.toString();
            InputSource is = new InputSource( new StringReader( xml ) );
            java.util.Properties properties = new java.util.Properties();
            properties.put("rep.home","tenant_"+tenant.getPID()+"/home");
            properties.put("wsp.name","workspace");
            properties.put("TENANT_PID",tenant.getPID());
            properties.put("TENANT_DB_SCHEMA","tenant_"+tenant.getPID().toLowerCase());
            boolean isPlatformTenant = tenant.getPID().equals(org.amdatu.multitenant.Constants.PID_VALUE_PLATFORM);
			if (isPlatformTenant) {
            	properties.put("TENANT_SCHEMA","tenant_PLATFORM");
            }
            else {
            	properties.put("TENANT_SCHEMA","tenant_"+tenant.getPID());
            }
			final String dsName = PropertiesUtil.toString(properties.get(SlingServerRepositoryManagerConfiguration.DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME),SlingServerRepositoryManagerConfiguration.DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME);
            properties.put("DATASOURCE_NAME",dsName);
			crc = RepositoryConfig.create(is,properties);
			

            Repository acquireRepository = registerStatistics(RepositoryImpl.create(crc));
			return acquireRepository;
        } catch (IOException ioe) {

            log.error("acquireRepository: IO problem starting repository from repository_template.xml template", ioe);

        } catch (RepositoryException re) {

            log.error("acquireRepository: Repository problem starting repository from repository_template.xml template",
                re);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // got no repository ....
        return null;
    }
    

	boolean pathExists(Repository repo, String path) throws RepositoryException 
    {
    	Session session =null;
    	try {
			session = repo.login(new SimpleCredentials(USER, PASSWORD.toCharArray()));
			return  session.nodeExists(path);
		} finally {
			if (session != null)
				session.logout();
		}
    }
    
    void ensurePath(Repository repo, String path) throws RepositoryException 
    {
    	String nodeName = null;
    	if (path.indexOf('/') >= 0)
    		nodeName = path.split("/")[1];
    	Session session =null;
    	try {
			session = repo.login(new SimpleCredentials(USER, PASSWORD.toCharArray()));
			if (!session.nodeExists(path)) {
				session.getRootNode().addNode(nodeName);
				session.save();
			}
		} finally {
			if (session != null)
				session.logout();
		}
    }    
    
    void rename(Repository repo, String path, String newName) throws RepositoryException 
    {
    	Session session = null;
    	try {
			session = repo.login(new SimpleCredentials(USER, PASSWORD.toCharArray()));
			Node node = session.getNode(path);
			node.getSession().move(node.getPath(), node.getParent().getPath() + "/" + newName);
			node.getSession().save();
		} finally {
			if (session != null)
				session.logout();
		}
    }
    
    
    private Repository registerStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            try {
                RepositoryImpl repositoryImpl = (RepositoryImpl) repository;
                StatisticsMBeanImpl mbean = new StatisticsMBeanImpl(repositoryImpl);
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                String mbeanName = StatisticsMBeanImpl.getMBeanName(repositoryImpl);
                properties.put("jmx.objectname", mbeanName);
                properties.put(Constants.SERVICE_VENDOR, "Apache");
                statisticsServices.put(
                    mbeanName,
                    componentBundleContext.registerService(DynamicMBean.class.getName(), mbean,
                        properties));
            } catch (Exception e) {
                log.error("Unable to register statistics ", e);
            }
        }
        return repository;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Dictionary<String, Object> getServiceRegistrationProperties() {
    	if (!tenant.getPID().equals(org.amdatu.multitenant.Constants.PID_VALUE_PLATFORM)) {
    		return new Hashtable<String, Object>();// Don't remote tenant repo other than PLATFORM
    	}
    	else {//Do it
    		Hashtable<String, Object> props = new Hashtable<String, Object>();
    		props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES,Repository.class.getName());
    		return props;
    	}
    }

    /**
     * Returns the Jackrabbit {@code RepositoryManager} interface implemented by
     * the Jackrabbit Repository in addition to the {@code SlingRepository} and
     * {@code Repository} interfaces implemented by the base class.
     *
     * @since bundle version 2.2.0 replacing the previously overwriting of the
     *        now final {@code AbstractSlingRepository.registerService} method.
     */
    @Override
    protected String[] getServiceRegistrationInterfaces() {
        return new String[] {
            SlingRepository.class.getName(), Repository.class.getName(), RepositoryManager.class.getName()
        };
    }

    @Override
    protected AbstractSlingRepository2 create(Bundle usingBundle) {
        return new SlingServerRepository(this, usingBundle, this.adminUserName);
    }

    @Override
    protected void destroy(AbstractSlingRepository2 repositoryServiceInstance) {
        // nothing to do
    }
/*
    @Override
    protected NamespaceMapper[] getNamespaceMapperServices() {
        return this.namespaceMappers;
    }*/

    @Override
    protected ServiceUserMapper getServiceUserMapper() {
        return this.serviceUserMapper;
    }

    @Override
    protected void disposeRepository(Repository repository) {
        unregisterStatistics(repository);

        if (repository instanceof RepositoryImpl) {

            try {
                ((RepositoryImpl) repository).shutdown();
            } catch (Throwable t) {
                log.error("deactivate: Unexpected problem shutting down repository", t);
            }

        } else {
            log.error("Repository is not a RepositoryImpl, nothing to do");
        }
    }

    private void unregisterStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            String mbeanName = StatisticsMBeanImpl.getMBeanName((RepositoryImpl) repository);
            try {
                ServiceRegistration serviceRegistration = statisticsServices.get(mbeanName);
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            } catch (Exception e) {
                log.warn("Failed to unregister statistics JMX bean {} ", e.getMessage());
            }
            statisticsServices.remove(mbeanName);
        }
    }

    // ---------- Helper -------------------------------------------------------

    private static void copyFile(Bundle bundle, String entryPath, File destFile) throws FileNotFoundException,
            IOException {
        if (destFile.canRead()) {
            // nothing to do, file exists
            return;
        }

        // copy from property
        URL entryURL = bundle.getEntry(entryPath);
        if (entryURL == null) {
            throw new FileNotFoundException(entryPath);
        }

        // check for a file property
        InputStream source = entryURL.openStream();
        copyStream(source, destFile);
    }

    private static void copyStream(InputStream source, File destFile) throws FileNotFoundException, IOException {
        OutputStream dest = null;

        try {

            // ensure path to parent folder of licFile
            destFile.getParentFile().mkdirs();

            dest = new FileOutputStream(destFile);
            byte[] buf = new byte[2048];
            int rd;
            while ((rd = source.read(buf)) >= 0) {
                dest.write(buf, 0, rd);
            }

        } finally {
            if (dest != null) {
                try {
                    dest.close();
                } catch (IOException ignore) {
                }
            }

            // licSource is not null
            try {
                source.close();
            } catch (IOException ignore) {
            }
        }
    }
}