package org.apache.sling.serviceusermapping.impl;


import java.beans.EventHandler;
import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestListener;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.impl.engine.EngineSlingAuthenticator;
import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.bundle.FindHook;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.service.event.EventAdmin;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//MappingConfigAmendment
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(MappingConfigAmendment.class.getName(), properties)
				.setImplementation(MappingConfigAmendment.class)
				.setCallbacks(null,"configure",null, null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(MappingConfigAmendment.class.getName()))
	            ;
		 dm.add(component);
		 
		//MappingInventoryPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("felix.inventory.printer.format",new String[]{"JSON","TEXT"});
	    properties.put("felix.inventory.printer.name","slingserviceusers");
	    properties.put("felix.inventory.printer.title","Sling Service User Mappings");
	    properties.put("felix.inventory.printer.webconsole","true");
		component = dm.createComponent()
				.setInterface(InventoryPrinter.class.getName(), properties)
				.setImplementation(MappingInventoryPrinter.class)
				.add(createConfigurationDependency().setPid(MappingInventoryPrinter.class.getName()))
				.add(createServiceDependency().setService(ServiceUserMapperImpl.class).setRequired(true))
	            ;
		 dm.add(component);
		 
		//MappingInventoryPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("felix.inventory.printer.format",new String[]{"JSON","TEXT"});
	    properties.put("felix.inventory.printer.name","slingserviceusers");
	    properties.put("felix.inventory.printer.title","Sling Service User Mappings");
	    properties.put("felix.inventory.printer.webconsole","true");
		component = dm.createComponent()
				.setInterface(InventoryPrinter.class.getName(), properties)
				.setImplementation(MappingInventoryPrinter.class)
				.add(createConfigurationDependency().setPid(MappingInventoryPrinter.class.getName()))
				.add(createServiceDependency().setService(ServiceUserMapperImpl.class).setRequired(true))
	            ;
		 dm.add(component);
		 
		 
		//ServiceUserMappedBundleFilter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{org.osgi.framework.hooks.service.EventListenerHook.class.getName(),org.osgi.framework.hooks.service.FindHook.class.getName()}, properties)
				.setImplementation(ServiceUserMappedBundleFilter.class)
	            ;
		 dm.add(component);
		 
		//ServiceUserMapperImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Configuration for the service mapping service names to names of users.");
		component = dm.createComponent()
				.setInterface(new String[]{ServiceUserMapper.class.getName(),ServiceUserMapperImpl.class.getName()}, properties)
				.setImplementation(ServiceUserMapperImpl.class)
				.setCallbacks(null,"configure","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(ServiceUserMapperImpl.class.getName()))
				.add(createServiceDependency().setService(MappingConfigAmendment.class)
						.setCallbacks("bindAmendment", "unbindAmendment")
						.setRequired(false))
				.add(createServiceDependency().setService(ServiceUserValidator.class)						
						.setCallbacks("bindServiceUserValidator", "unbindServiceUserValidator")
						.setRequired(false))
				;
		 dm.add(component);
	}

}