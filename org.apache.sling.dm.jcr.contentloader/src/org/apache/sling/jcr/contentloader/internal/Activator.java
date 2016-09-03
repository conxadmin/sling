package org.apache.sling.jcr.contentloader.internal;


import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.impl.log.RequestLoggerService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.OrderedJsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.XmlReader;
import org.apache.sling.jcr.contentloader.internal.readers.ZipReader;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//ContentLoaderService
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,ContentLoaderService.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Content Loader Implementation");
		Component component = dm.createComponent()
				.setInterface(ContentLoaderService.class.getName(), properties)
				.setImplementation(ContentLoaderService.class)
				.setCallbacks(null,"activate","deactivate", null)//init, start, stop and destroy.
				.add(createServiceDependency().setService(SlingRepository.class).setRequired(true))
				.add(createServiceDependency().setService(MimeTypeService.class).setRequired(true))
				.add(createServiceDependency().setService(ContentReaderWhiteboard.class).setRequired(true))
				.add(createServiceDependency().setService(SlingSettingsService.class).setRequired(true))
	            ;
		dm.add(component);
		
		//ContentReaderWhiteboard
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ContentReaderWhiteboard.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		component = dm.createComponent()
				.setInterface(ContentReaderWhiteboard.class.getName(), properties)
				.setImplementation(ContentReaderWhiteboard.class)
				.add(createServiceDependency().setService(ContentReader.class)
						.setCallbacks("bindContentReader", "unbindContentReader")
						.setRequired(false))
	            ;
		dm.add(component);
		
		//DefaultContentImporter
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,DefaultContentImporter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"Apache Sling JCR Content Import Service");
		component = dm.createComponent()
				.setInterface(ContentImporter.class.getName(), properties)
				.setImplementation(DefaultContentImporter.class)
				.add(createServiceDependency().setService(ContentReaderWhiteboard.class).setRequired(true))
				.add(createServiceDependency().setService(MimeTypeService.class).setRequired(true))
	            ;
		dm.add(component);
		
		//JsonReader
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,JsonReader.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(ContentReader.PROPERTY_EXTENSIONS,"json");
		properties.put(ContentReader.PROPERTY_TYPES, "application/json");
		component = dm.createComponent()
				.setInterface(ContentReader.class.getName(), properties)
				.setImplementation(JsonReader.class)
	            ;
		dm.add(component);
		
		//OrderedJsonReader
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,OrderedJsonReader.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(ContentReader.PROPERTY_EXTENSIONS,"ordered-json");
		properties.put(ContentReader.PROPERTY_TYPES, "application/json");
		component = dm.createComponent()
				.setInterface(ContentReader.class.getName(), properties)
				.setImplementation(OrderedJsonReader.class)
				.add(createServiceDependency().setService(MimeTypeService.class).setRequired(true))
	            ;
		dm.add(component);
		
		//XmlReader
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,XmlReader.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(ContentReader.PROPERTY_EXTENSIONS,"xml");
		properties.put(ContentReader.PROPERTY_TYPES, new String[]{"application/xml", "text/xml"});
		component = dm.createComponent()
				.setInterface(ContentReader.class.getName(), properties)
				.setImplementation(XmlReader.class)
	            ;
		dm.add(component);
		
		//ZipReader
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,ZipReader.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(ContentReader.PROPERTY_EXTENSIONS,new String[]{"zip", "jar"});
		properties.put(ContentReader.PROPERTY_TYPES, new String[]{"application/zip", "application/java-archive"});
		component = dm.createComponent()
				.setInterface(ContentReader.class.getName(), properties)
				.setImplementation(ZipReader.class)
	            ;
		dm.add(component);
	}

}