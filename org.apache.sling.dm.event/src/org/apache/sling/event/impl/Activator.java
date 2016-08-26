package org.apache.sling.event.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.impl.WebConsolePlugin;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.scheduler.impl.TopologyHandler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.MainQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.console.InventoryPlugin;
import org.apache.sling.event.impl.jobs.jmx.AllJobStatisticsMBean;
import org.apache.sling.event.impl.jobs.notifications.NewJobSender;
import org.apache.sling.event.impl.jobs.queues.QueueManager;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.impl.jobs.tasks.HistoryCleanUpTask;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.apache.sling.event.jobs.jmx.QueuesMBean;
import org.apache.sling.event.jobs.jmx.StatisticsMBean;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//EnvironmentComponent
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(EnvironmentComponent.class.getName(), properties)
				.setImplementation(EnvironmentComponent.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EventingThreadPool.class)
						 .setCallbacks("bindThreadPool", "unbindThreadPool").setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            ;
		dm.add(component);


		//EventingThreadPool
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Apache Sling Job Thread Pool");
		component = dm.createComponent()
				.setInterface(EventingThreadPool.class.getName(), properties)
				.setImplementation(EventingThreadPool.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(EventingThreadPool.class.getName()))
				.add(createServiceDependency().setService(ThreadPoolManager.class).setRequired(true))
	            ;
		dm.add(component);	

		//JobManagerConfiguration
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "This is the central service of the job handling.");

		component = dm.createComponent()
				.setInterface(JobManagerConfiguration.class.getName(), properties)
				.setImplementation(JobManagerConfiguration.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(JobManagerConfiguration.class.getName()))
				.add(createServiceDependency().setService(EnvironmentComponent.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(QueueConfigurationManager.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
	            ;
		dm.add(component);		

		//MainQueueConfiguration
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "The configuration of the default job queue.");

		component = dm.createComponent()
				.setInterface(MainQueueConfiguration.class.getName(), properties)
				.setImplementation(JobManagerConfiguration.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createConfigurationDependency().setPid(JobManagerConfiguration.class.getName()))
				.add(createServiceDependency().setService(EnvironmentComponent.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(QueueConfigurationManager.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
	            ;
		dm.add(component);		
		
		//QueueConfigurationManager
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(QueueConfigurationManager.class.getName(), properties)
				.setImplementation(QueueConfigurationManager.class)
				.add(createServiceDependency().setService(InternalQueueConfiguration.class)
						 .setCallbacks("bindConfig", "updateConfig", "unbindConfig")//(java.lang.String add, java.lang.String change, java.lang.String remove)
						 .setRequired(true))
				.add(createServiceDependency().setService(MainQueueConfiguration.class).setRequired(true))
	            ;
		dm.add(component);
		
		
		//TopologyEventListener
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(TopologyEventListener.class.getName(), properties)
				.setImplementation(TopologyHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
	            ;
		dm.add(component);
		
		//InventoryPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(InventoryPrinter.NAME, "slingjobs");
		properties.put(InventoryPrinter.TITLE, "Sling Jobs");
		properties.put(InventoryPrinter.FORMAT, new String[]{"TEXT", "JSON"});
		properties.put(InventoryPrinter.WEBCONSOLE, false);
		component = dm.createComponent()
				.setInterface(InventoryPrinter.class.getName(), properties)
				.setImplementation(InventoryPlugin.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobManager.class).setRequired(true))
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(JobConsumerManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//InventoryPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

		properties.put("felix.webconsole.label","slingevent");
		properties.put("felix.webconsole.title","Jobs");
		properties.put("felix.webconsole.category","Sling");
		properties.put(JobConsumer.PROPERTY_TOPICS, new String[]{"sling/webconsole/test"});
	    
		component = dm.createComponent()
				.setInterface(new String[]{javax.servlet.Servlet.class.getName(), JobConsumer.class.getName()}, properties)
				.setImplementation(WebConsolePlugin.class)
				.add(createServiceDependency().setService(JobManager.class).setRequired(true))
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(JobConsumerManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//StatisticsMBean
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("jmx.objectname","org.apache.sling:type=queues,name=AllQueues");
	    
		component = dm.createComponent()
				.setInterface(StatisticsMBean.class.getName(), properties)
				.setImplementation(AllJobStatisticsMBean.class)
				.add(createServiceDependency().setService(JobManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//QueuesMBean
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(EventHandler.class.getName(), properties)
				.setImplementation(NewJobSender.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
	            ;
		dm.add(component);
		
		//QueueManager
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Scheduler.PROPERTY_SCHEDULER_PERIOD,60L);
		properties.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT,false);
		properties.put(EventConstants.EVENT_TOPIC,NotificationConstants.TOPIC_JOB_ADDED);
		component = dm.createComponent()
				.setInterface(new String[]{Runnable.class.getName(), QueueManager.class.getName(), EventHandler.class.getName()}, properties)
				.setImplementation(QueueManager.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
				.add(createServiceDependency().setService(JobConsumerManager.class).setRequired(true))
				.add(createServiceDependency().setService(QueuesMBean.class).setRequired(true))
				.add(createServiceDependency().setService(ThreadPoolManager.class).setRequired(true))
				.add(createServiceDependency().setService(EventingThreadPool.class)
						.setCallbacks("setEventingThreadPool","unsetEventingThreadPool")
						.setRequired(true))
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(StatisticsManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//StatisticsManager
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(StatisticsManager.class.getName(), properties)
				.setImplementation(StatisticsManager.class)
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
	            ;
		dm.add(component);
		
		//HistoryCleanUpTask
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(JobExecutor.PROPERTY_TOPICS,"org/apache/sling/event/impl/jobs/tasks/HistoryCleanUpTask");
		component = dm.createComponent()
				.setInterface(JobExecutor.class.getName(), properties)
				.setImplementation(HistoryCleanUpTask.class)
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
	            ;
		dm.add(component);
	}

}