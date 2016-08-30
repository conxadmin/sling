package org.apache.sling.jcr.resource.internal;


import java.util.Properties;

import javax.jcr.Repository;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.jcr.resource.internal.scripting.JcrObjectsBindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//JcrResourceProvider
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "This provider adds JCR resources to the resource tree");
		properties.put(ResourceProvider.PROPERTY_NAME,"JCR");
        properties.put(ResourceProvider.PROPERTY_ROOT,"/");
        properties.put(ResourceProvider.PROPERTY_MODIFIABLE,true);
        properties.put(ResourceProvider.PROPERTY_ADAPTABLE,true);
        properties.put(ResourceProvider.PROPERTY_AUTHENTICATE,true);
        properties.put(ResourceProvider.PROPERTY_ATTRIBUTABLE,true);
        properties.put(ResourceProvider.PROPERTY_REFRESHABLE,true);
	    
		Component component = dm.createComponent()
				.setInterface(ResourceProvider.class.getName(), properties)
				.setImplementation(JcrResourceProvider.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency()
						.setPid(JcrResourceProvider.class.getName()))
				.add(createServiceDependency().setService(Repository.class)
						.setCallbacks("bindRepository", "unbindRepository")
						.setRequired(false))
	            ;
		dm.add(component);
		
		//JcrResourceProvider
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "This service provides path mappings for JCR nodes.");
	    
		component = dm.createComponent()
				.setInterface(PathMapper.class.getName(), properties)
				.setImplementation(PathMapper.class)
				.setCallbacks(null,"activate", null, null)//init, start, stop and destroy.
				.add(createConfigurationDependency()
						.setPid(JcrResourceProvider.class.getName()))
	            ;
		dm.add(component);
		
		//JcrObjectsBindingsValuesProvider
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling CurrentNode BindingsValuesProvider");
	    
		component = dm.createComponent()
				.setInterface(BindingsValuesProvider.class.getName(), properties)
				.setImplementation(JcrObjectsBindingsValuesProvider.class)
	            ;
		dm.add(component);
		
		//JcrResourceResolverFactoryImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling JcrResourceResolverFactory Implementation");
	    
		component = dm.createComponent()
				.setInterface(JcrResourceResolverFactory.class.getName(), properties)
				.setImplementation(JcrResourceResolverFactoryImpl.class)
				.add(createServiceDependency().setService(DynamicClassLoaderManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//JcrSystemUserValidator
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Enforces the usage of JCR system users for all user mappings being used in the 'Sling Service User Mapper Service'");
	    
		component = dm.createComponent()
				.setInterface(ServiceUserValidator.class.getName(), properties)
				.setImplementation(JcrSystemUserValidator.class)
				.add(createServiceDependency().setService(DynamicClassLoaderManager.class).setRequired(true))
				.add(createConfigurationDependency()
						.setPid(JcrSystemUserValidator.class.getName()))
	            ;
		dm.add(component);


	}

}