package org.apache.sling.event.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.MainQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.TopologyHandler;
import org.apache.sling.event.impl.jobs.console.InventoryPlugin;
import org.apache.sling.event.impl.jobs.console.WebConsolePlugin;
import org.apache.sling.event.impl.jobs.jmx.AllJobStatisticsMBean;
import org.apache.sling.event.impl.jobs.jmx.QueuesMBeanImpl;
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
import org.osgi.service.cm.ManagedService;
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
		properties.put(Constants.SERVICE_PID,EnvironmentComponent.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),EnvironmentComponent.class.getName()}, properties)
				.setImplementation(EnvironmentComponent.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EventingThreadPool.class)
						 .setCallbacks("bindThreadPool", "unbindThreadPool").setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            ;
		dm.add(component);


		//EventingThreadPool
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,EventingThreadPool.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Apache Sling Job Thread Pool");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),EventingThreadPool.class.getName()}, properties)
				.setImplementation(EventingThreadPool.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(ThreadPoolManager.class).setRequired(true))
	            ;
		dm.add(component);	
		
		//InternalQueueConfiguration
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,InternalQueueConfiguration.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),InternalQueueConfiguration.class.getName()}, properties)
				.setImplementation(InternalQueueConfiguration.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            ;
		dm.add(component);
		

		//JobManagerConfiguration
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,JobManagerConfiguration.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "This is the central service of the job handling.");

		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),JobManagerConfiguration.class.getName()}, properties)
				.setImplementation(JobManagerConfiguration.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EnvironmentComponent.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(QueueConfigurationManager.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
	            ;
		dm.add(component);		

		//MainQueueConfiguration
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,MainQueueConfiguration.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "The configuration of the default job queue.");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),MainQueueConfiguration.class.getName()}, properties)
				.setImplementation(JobManagerConfiguration.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EnvironmentComponent.class).setRequired(true))
				.add(createServiceDependency().setService(ResourceResolverFactory.class).setRequired(true))
				.add(createServiceDependency().setService(QueueConfigurationManager.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
	            ;
		dm.add(component);		
		
		//QueueConfigurationManager
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,QueueConfigurationManager.class.getName());
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
		properties.put(Constants.SERVICE_PID,TopologyHandler.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(TopologyEventListener.class.getName(), properties)
				.setImplementation(TopologyHandler.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
	            ;
		dm.add(component);
		
		//InventoryPlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,InventoryPlugin.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(InventoryPrinter.NAME, "slingjobs");
		properties.put(InventoryPrinter.TITLE, "Sling Jobs");
		properties.put(InventoryPrinter.FORMAT, new String[]{"TEXT", "JSON"});
		properties.put(InventoryPrinter.WEBCONSOLE, false);
		component = dm.createComponent()
				.setInterface(InventoryPrinter.class.getName(), properties)
				.setImplementation(InventoryPlugin.class)
				.add(createServiceDependency().setService(JobManager.class).setRequired(true))
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(JobConsumerManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//WebConsolePlugin
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,WebConsolePlugin.class.getName());
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
		
		//AllJobStatisticsMBean
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,AllJobStatisticsMBean.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("jmx.objectname","org.apache.sling:type=queues,name=AllQueues");
	    
		component = dm.createComponent()
				.setInterface(StatisticsMBean.class.getName(), properties)
				.setImplementation(AllJobStatisticsMBean.class)
				.add(createServiceDependency().setService(JobManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//QueuesMBeanImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,QueuesMBeanImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("jmx.objectname","org.apache.sling:type=queues,name=QueueNames");
		component = dm.createComponent()
				.setInterface(QueuesMBean.class.getName(), properties)
				.setImplementation(QueuesMBeanImpl.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
	            ;
		dm.add(component);
		
		//JobConsumerManager
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,JobConsumerManager.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("org.apache.sling.installer.configuration.persist",false);
		properties.put("job.consumermanager.whitelist","*");
	    properties.put("job.consumermanager.blacklist","");
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),JobConsumerManager.class.getName()}, properties)
				.setImplementation(JobConsumerManager.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobConsumer.class)
						 .setCallbacks("bindJobConsumer", "unbindJobConsumer")//(java.lang.String add, java.lang.String change, java.lang.String remove)
						 .setRequired(false))
				.add(createServiceDependency().setService(JobExecutor.class)
						 .setCallbacks("bindJobExecutor", "unbindJobExecutor")//(java.lang.String add, java.lang.String change, java.lang.String remove)
						 .setRequired(false))
	            ;
		dm.add(component);
		
		//JobManagerImpl
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,JobManagerImpl.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("scheduler.period",60);
		properties.put("scheduler.concurrent",false);
	    properties.put("event.topics",new String[]{"org/apache/sling/api/resource/Resource/ADDED","org/apache/sling/api/resource/Resource/CHANGED","org/apache/sling/api/resource/Resource/REMOVED","org/osgi/framework/BundleEvent/STARTED","org/osgi/framework/BundleEvent/UPDATED"});
		component = dm.createComponent()
				.setInterface(new String[]{JobManager.class.getName(),EventHandler.class.getName(),Runnable.class.getName()}, properties)
				.setImplementation(JobManagerImpl.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
				.add(createServiceDependency().setService(Scheduler.class).setRequired(true))
				.add(createServiceDependency().setService(JobConsumerManager.class).setRequired(true))
				.add(createServiceDependency().setService(QueuesMBean.class).setRequired(true))
				.add(createServiceDependency().setService(ThreadPoolManager.class).setRequired(true))
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(StatisticsManager.class).setRequired(true))
				.add(createServiceDependency().setService(QueueManager.class).setRequired(true))
	            ;
		dm.add(component);
		
		//NewJobSender
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,NewJobSender.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(NewJobSender.class.getName(), properties)
				.setImplementation(NewJobSender.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(JobManagerConfiguration.class).setRequired(true))
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
	            ;
		dm.add(component);
		
		//QueueManager
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,QueueManager.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Scheduler.PROPERTY_SCHEDULER_PERIOD,60L);
		properties.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT,false);
		properties.put(EventConstants.EVENT_TOPIC,NotificationConstants.TOPIC_JOB_ADDED);
		component = dm.createComponent()
				.setInterface(new String[]{Runnable.class.getName(), QueueManager.class.getName(), EventHandler.class.getName()}, properties)
				.setImplementation(QueueManager.class)
				.setCallbacks("init","activate","deactivate", null)//init, start, stop and destroy.
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
		properties.put(Constants.SERVICE_PID,StatisticsManager.class.getName());
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
		properties.put(Constants.SERVICE_PID,HistoryCleanUpTask.class.getName());
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