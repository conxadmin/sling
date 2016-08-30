package org.apache.sling.jackrabbit.usermanager.impl;


import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo;
import org.apache.sling.jackrabbit.usermanager.ChangeUserPassword;
import org.apache.sling.jackrabbit.usermanager.CreateGroup;
import org.apache.sling.jackrabbit.usermanager.CreateUser;
import org.apache.sling.jackrabbit.usermanager.DeleteAuthorizables;
import org.apache.sling.jackrabbit.usermanager.DeleteGroup;
import org.apache.sling.jackrabbit.usermanager.DeleteUser;
import org.apache.sling.jackrabbit.usermanager.UpdateGroup;
import org.apache.sling.jackrabbit.usermanager.impl.post.ChangeUserPasswordServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.CreateGroupServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.CreateUserServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.DeleteAuthorizableServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.UpdateGroupServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//AuthorizablePrivilegesInfoImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(AuthorizablePrivilegesInfo.class.getName(), properties)
				.setImplementation(AuthorizablePrivilegesInfoImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            ;
		dm.add(component);

		//ChangeUserPasswordServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),ChangeUserPassword.class.getName()}, properties)
				.setImplementation(ChangeUserPasswordServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(ChangeUserPasswordServlet.class.getName()))
	            ;
		dm.add(component);	
		
		//CreateGroupServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),CreateGroup.class.getName()}, properties)
				.setImplementation(CreateGroupServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(CreateGroupServlet.class.getName()))
				.add(createServiceDependency().setService(JcrResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);
		
		//CreateUserServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),CreateUser.class.getName()}, properties)
				.setImplementation(CreateUserServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(CreateUserServlet.class.getName()))
				.add(createServiceDependency().setService(SlingRepository.class).setRequired(true))
	            ;
		dm.add(component);
		
		//DeleteAuthorizableServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),DeleteUser.class.getName(),DeleteGroup.class.getName(), DeleteAuthorizables.class.getName()}, properties)
				.setImplementation(DeleteAuthorizableServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(DeleteAuthorizableServlet.class.getName()))
	            ;
		dm.add(component);
		
		//UpdateGroupServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),UpdateGroup.class.getName()}, properties)
				.setImplementation(UpdateGroupServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(UpdateGroupServlet.class.getName()))
				.add(createServiceDependency().setService(JcrResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);
		
		//AuthorizableResourceProvider
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.description","Resource provider implementation for UserManager resources");
		properties.put("service.vendor","The Apache Software Foundation");
		properties.put("provider.roots","/system/userManager/");
		component = dm.createComponent()
				.setInterface(ResourceProvider.class.getName(), properties)
				.setImplementation(AuthorizableResourceProvider.class)
				.add(createConfigurationDependency().setPid(AuthorizableResourceProvider.class.getName()))
	            ;
		dm.add(component);
	}

}