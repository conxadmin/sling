package org.apache.sling.servlets.post.impl;


import java.util.Properties;

import javax.servlet.Servlet;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.PostResponseWithErrorHandling;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.ChunkCleanUpTask;
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
		//SlingPostServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,SlingPostServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR,"The Apache Software Foundation");
		properties.put("service.description","Sling Post Servlet");
		properties.put("service.vendor","The Apache Software Foundation");
		properties.put("sling.servlet.prefix", -1);
		properties.put("sling.servlet.paths","sling/servlet/default/POST");
	    properties.put("servlet.post.dateFormats","EEE MMM dd yyyy HH:mm:ss 'GMT'Z,ISO8601,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd,dd.MM.yyyy HH:mm:ss,dd.MM.yyyy");
	    properties.put("servlet.post.nodeNameHints","title,jcr:title,name,description,jcr:description,abstract,text,jcr:text");
	    properties.put("servlet.post.nodeNameMaxLength",20);
	    properties.put("servlet.post.checkinNewVersionableNodes",true);
	    properties.put("servlet.post.autoCheckout",false);
	    properties.put("servlet.post.autoCheckin",true);
	    properties.put("servlet.post.ignorePattern","j_.*");
	    
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),Servlet.class.getName()}, properties)
				.setImplementation(SlingPostServlet.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingPostProcessor.class)
						.setCallbacks("bindPostProcessor", "unbindPostProcessor")
						.setRequired(false))
				.add(createServiceDependency().setService(PostOperation.class)
						.setCallbacks("bindPostOperation", "unbindPostOperation")
						.setRequired(false))
				.add(createServiceDependency().setService(NodeNameGenerator.class)
						.setCallbacks("bindNodeNameGenerator", "unbindNodeNameGenerator")
						.setRequired(false))
				.add(createServiceDependency().setService(PostResponseCreator.class)
						.setCallbacks("bindPostResponseCreator", "unbindPostResponseCreator")
						.setRequired(false))
				.add(createServiceDependency().setService(ContentImporter.class)
						.setCallbacks("bindContentImporter", "unbindContentImporter")
						.setRequired(false))
	            ;
		 dm.add(component);

		 //ChunkCleanUpTask
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ChunkCleanUpTask.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("service.description","Default GET Servlet");
	    properties.put("scheduler.expression","31 41 0/12 * * ?");
	    properties.put("service.description","Periodic Chunk Cleanup Job");
	    properties.put("service.vendor","The Apache Software Foundation");
	    properties.put("scheduler.concurrent",false);
		component = dm.createComponent()
				.setInterface(new String[]{/*ManagedService.class.getName(),*/Runnable.class.getName()}, properties)
				.setImplementation(ChunkCleanUpTask.class)
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