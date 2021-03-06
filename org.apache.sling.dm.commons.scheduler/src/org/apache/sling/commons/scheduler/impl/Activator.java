package org.apache.sling.commons.scheduler.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.discovery.TopologyEventListener;
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
		//QuartzScheduler
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,QuartzScheduler.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),QuartzScheduler.class.getName()}, properties)
				.setImplementation(QuartzScheduler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            .add(createServiceDependency()
	                	.setService(ThreadPoolManager.class)
	                	.setRequired(true))
	            ;
		 dm.add(component);
		 
		 //SchedulerServiceFactory
		 //TODO: implement prototype scope to support serviceFactory=true
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(Scheduler.class.getName(), properties)
				.setImplementation(SchedulerServiceFactory.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(QuartzScheduler.class).setRequired(true));
		dm.add(component);	 
		
		 //SettingsSupport
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(Object.class.getName(), properties)
				.setImplementation(SettingsSupport.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				;
		dm.add(component);	 

		 //TopologyHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(TopologyEventListener.class.getName(), properties)
				.setImplementation(TopologyHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				;
		dm.add(component);	
		
		//WebConsolePrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Scheduler Configuration Printer");
		properties.put("felix.webconsole.label","slingscheduler");
		properties.put("felix.webconsole.title","Sling Scheduler");
		properties.put("felix.webconsole.configprinter.modes","always");
		component = dm.createComponent()
				.setInterface(Object.class.getName(), properties)
				.setImplementation(WebConsolePrinter.class)
				.add(createServiceDependency().setService(QuartzScheduler.class).setRequired(true));
		dm.add(component);	
		
		//WhiteboardHandler
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(Object.class.getName(), properties)
				.setImplementation(WhiteboardHandler.class)
				.add(createServiceDependency().setService(QuartzScheduler.class).setRequired(true));
		dm.add(component);	
	}

}