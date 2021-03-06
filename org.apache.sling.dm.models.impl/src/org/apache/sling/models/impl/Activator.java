package org.apache.sling.models.impl;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ResourcePathInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.SlingObjectInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ModelValidation;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
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
		//FirstImplementationPicker
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,FirstImplementationPicker.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		Component component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(),ImplementationPicker.class.getName()}, properties)
				.setImplementation(FirstImplementationPicker.class)
	            ;
		dm.add(component);


		//ModelAdapterFactory
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ModelAdapterFactory.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Model Adapter Factory");
		properties.put("max.recursion.depth",20);
		component = dm.createComponent()
				.setInterface(new String[]{ManagedService.class.getName(), ModelFactory.class.getName()}, properties)
				.setImplementation(ModelAdapterFactory.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(Injector.class).setRequired(true))
				.add(createServiceDependency().setService(InjectAnnotationProcessorFactory.class).setRequired(true))
				.add(createServiceDependency().setService(InjectAnnotationProcessorFactory2.class).setRequired(true))
				.add(createServiceDependency().setService(StaticInjectAnnotationProcessorFactory.class).setRequired(true))
				.add(createServiceDependency().setService(ImplementationPicker.class).setRequired(true))
				.add(createServiceDependency().setService(ModelValidation.class).setRequired(true))
	            ;
		dm.add(component);	

		//BindingsInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",1000);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName()}, properties)
				.setImplementation(BindingsInjector.class)
	            ;
		dm.add(component);		

		//ChildResourceInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",3000);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),InjectAnnotationProcessorFactory2.class.getName()}, properties)
				.setImplementation(ChildResourceInjector.class)
	            ;
		dm.add(component);	
		
		//OSGiServiceInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",5000);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName(),AcceptsNullName.class.getName()}, properties)
				.setImplementation(OSGiServiceInjector.class)
				.setCallbacks(null,"activate",null, null)//init, start, stop and destroy.
	            ;
		dm.add(component);
		
		//RequestAttributeInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",4000);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName()}, properties)
				.setImplementation(RequestAttributeInjector.class)
	            ;
		dm.add(component);	
		
		//ResourcePathInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",2500);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName(),AcceptsNullName.class.getName()}, properties)
				.setImplementation(ResourcePathInjector.class)
	            ;
		dm.add(component);	
		
		//SelfInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",Integer.MAX_VALUE);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName(),AcceptsNullName.class.getName()}, properties)
				.setImplementation(SelfInjector.class)
	            ;
		dm.add(component);	
		
		//SlingObjectInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",Integer.MAX_VALUE);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),StaticInjectAnnotationProcessorFactory.class.getName(),AcceptsNullName.class.getName()}, properties)
				.setImplementation(SlingObjectInjector.class)
	            ;
		dm.add(component);	
		
		//ValueMapInjector
		properties = new Properties();
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put("service.ranking",2000);
		component = dm.createComponent()
				.setInterface(new String[]{Injector.class.getName(),InjectAnnotationProcessorFactory.class.getName()}, properties)
				.setImplementation(ValueMapInjector.class)
	            ;
		dm.add(component);
	}

}