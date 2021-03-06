package org.apache.sling.jackrabbit.usermanager.impl;


import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.engine.impl.log.RequestLoggerService;
import org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo;
import org.apache.sling.jackrabbit.usermanager.ChangeUserPassword;
import org.apache.sling.jackrabbit.usermanager.CreateGroup;
import org.apache.sling.jackrabbit.usermanager.CreateUser;
import org.apache.sling.jackrabbit.usermanager.DeleteAuthorizables;
import org.apache.sling.jackrabbit.usermanager.DeleteGroup;
import org.apache.sling.jackrabbit.usermanager.DeleteUser;
import org.apache.sling.jackrabbit.usermanager.UpdateGroup;
import org.apache.sling.jackrabbit.usermanager.UpdateUser;
import org.apache.sling.jackrabbit.usermanager.impl.post.ChangeUserPasswordServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.CreateGroupServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.CreateUserServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.DeleteAuthorizableServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.UpdateGroupServlet;
import org.apache.sling.jackrabbit.usermanager.impl.post.UpdateUserServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
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
		//AuthorizablePrivilegesInfoImpl
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,AuthorizablePrivilegesInfo.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("user.admin.group.name","UserAdmin");
	    properties.put("group.admin.group.name","GroupAdmin");
		Component component = dm.createComponent()
				.setInterface(AuthorizablePrivilegesInfo.class.getName(), properties)
				.setImplementation(AuthorizablePrivilegesInfoImpl.class)
	            ;
		dm.add(component);

		//ChangeUserPasswordServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ChangeUserPasswordServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/user");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","changePassword");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss,'GMT'Z,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(), Servlet.class.getName(),ChangeUserPassword.class.getName()}, properties)
				.setImplementation(ChangeUserPasswordServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            ;
		dm.add(component);	
		
		//CreateGroupServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,CreateGroupServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/groups");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","create");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss,'GMT'Z,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName(),CreateGroup.class.getName()}, properties)
				.setImplementation(CreateGroupServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JcrResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);
		
		//CreateUserServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,CreateUserServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/users");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","create");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss,'GMT'Z,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName(),CreateUser.class.getName()}, properties)
				.setImplementation(CreateUserServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingRepository.class).setRequired(true))
	            ;
		dm.add(component);
		
		//DeleteAuthorizableServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,DeleteAuthorizableServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/user,sling/group,sling/userManager");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","delete");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName(),DeleteUser.class.getName(),DeleteGroup.class.getName(), DeleteAuthorizables.class.getName()}, properties)
				.setImplementation(DeleteAuthorizableServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            ;
		dm.add(component);
		
		//UpdateGroupServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,UpdateGroupServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/group");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","update");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss,'GMT'Z,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName(),UpdateGroup.class.getName()}, properties)
				.setImplementation(UpdateGroupServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JcrResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);
		
		//UpdateUserServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,UpdateUserServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/user");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","update");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss,'GMT'Z,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName(),UpdateUser.class.getName()}, properties)
				.setImplementation(UpdateUserServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JcrResourceResolverFactory.class).setRequired(true))
	            ;
		dm.add(component);
		
		//AuthorizableResourceProvider
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,AuthorizableResourceProvider.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.description","Resource provider implementation for UserManager resources");
		properties.put("service.vendor","The Apache Software Foundation");
		properties.put("provider.roots","/system/userManager/");
		component = dm.createComponent()
				.setInterface(ResourceProvider.class.getName(), properties)
				.setImplementation(AuthorizableResourceProvider.class)
	            ;
		dm.add(component);
	}

}