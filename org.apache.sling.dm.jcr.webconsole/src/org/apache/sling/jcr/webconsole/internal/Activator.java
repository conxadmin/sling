package org.apache.sling.jcr.webconsole.internal;


import java.util.Properties;

import javax.jcr.Repository;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {

	}

	@Override
	public void init(BundleContext arg0, DependencyManager dm) throws Exception {
		//DescriptorsConfigurationPrinter
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID,DescriptorsConfigurationPrinter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"JCR Descriptors Configuration Printer");
		Component component = dm.createComponent()
				.setInterface(ConfigurationPrinter.class.getName(), properties)
				.setImplementation(DescriptorsConfigurationPrinter.class)
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
	            ;
		dm.add(component);
		
		//NamespaceConfigurationPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,NamespaceConfigurationPrinter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"JCR Namespace Configuration Printer");
		component = dm.createComponent()
				.setInterface(ConfigurationPrinter.class.getName(), properties)
				.setImplementation(NamespaceConfigurationPrinter.class)
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
	            ;
		dm.add(component);
		
		//NodeTypeConfigurationPrinter
		properties = new Properties();
		properties.put(Constants.SERVICE_PID,NodeTypeConfigurationPrinter.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		properties.put(Constants.SERVICE_DESCRIPTION,"JCR Nodetype Configuration Printer");
		properties.put("felix.webconsole.configprinter.web.unescaped",true);
		component = dm.createComponent()
				.setInterface(ConfigurationPrinter.class.getName(), properties)
				.setImplementation(NodeTypeConfigurationPrinter.class)
				.add(createServiceDependency().setService(SlingRepository.class)
						.setRequired(true))
	            ;
		dm.add(component);

	}

}