package org.apache.sling.servlets.post.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.PostResponseWithErrorHandling;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.ChunkCleanUpTask;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//SlingPostServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR,"The Apache Software Foundation");
		properties.put("service.description","Sling Post Servlet");
		properties.put("service.vendor","The Apache Software Foundation");
		properties.put("sling.servlet.prefix", -1);
		properties.put("sling.servlet.paths","sling/servlet/default/POST");
		Component component = dm.createComponent()
				.setInterface(Servlet.class.getName(), properties)
				.setImplementation(SlingPostServlet.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(SlingPostServlet.class.getName()))
				.add(createServiceDependency().setService(SlingPostProcessor.class).setRequired(true))
				.add(createServiceDependency().setService(PostOperation.class).setRequired(true))
				.add(createServiceDependency().setService(NodeNameGenerator.class).setRequired(true))
				.add(createServiceDependency().setService(PostResponseCreator.class).setRequired(true))
				.add(createServiceDependency().setService(CndImporter.class).setRequired(true))
	            ;
		 dm.add(component);
		 
		//ChunkCleanUpTask
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("service.description","Default GET Servlet");
	    properties.put("scheduler.expression","31 41 0/12 * * ?");
	    properties.put("service.description","Periodic Chunk Cleanup Job");
	    properties.put("service.vendor","The Apache Software Foundation");
	    properties.put("scheduler.concurrent",false);
		component = dm.createComponent()
				.setInterface(Runnable.class.getName(), properties)
				.setImplementation(ChunkCleanUpTask.class)
				.add(createConfigurationDependency().setPid(ChunkCleanUpTask.class.getName()))
				.add(createServiceDependency().setService(SlingRepository.class).setRequired(true))
	            ;
		 dm.add(component);
		 
		 
		//PostOperationProxyProvider
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(PostOperationProxyProvider.class.getName(), properties)
				.setImplementation(PostOperationProxyProvider.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            ;
		 dm.add(component);	
		 
		//PostResponseWithErrorHandling
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(PostResponseWithErrorHandling.class.getName(), properties)
				.setImplementation(PostResponseWithErrorHandling.class)
	            ;
		 dm.add(component);	
	}
}