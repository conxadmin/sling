package org.apache.sling.jcr.jackrabbit.accessmanager.internal;


import java.util.Properties;

import javax.servlet.Servlet;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.jcr.jackrabbit.accessmanager.DeleteAces;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAcl;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetEffectiveAcl;
import org.apache.sling.jcr.jackrabbit.accessmanager.ModifyAce;
import org.apache.sling.jcr.jackrabbit.accessmanager.post.DeleteAcesServlet;
import org.apache.sling.jcr.jackrabbit.accessmanager.post.GetAclServlet;
import org.apache.sling.jcr.jackrabbit.accessmanager.post.GetEffectiveAclServlet;
import org.apache.sling.jcr.jackrabbit.accessmanager.post.ModifyAceServlet;
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
		//DeleteAcesServlet
		Properties properties = new Properties();
		properties.put(Constants.SERVICE_PID, DeleteAcesServlet.class.getName());
		properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Content Loader Implementation");
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","deleteAce");
		Component component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),DeleteAces.class.getName()}, properties)
				.setImplementation(DeleteAcesServlet.class)
	            ;
		dm.add(component);

		//GetAclServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID, GetAclServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.methods","GET");
	    properties.put("sling.servlet.selectors",new String[]{"acl","tidy.acl"});
	    properties.put("sling.servlet.extensions","json");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),GetAcl.class.getName()}, properties)
				.setImplementation(GetAclServlet.class)
	            ;
		dm.add(component);
		
		//GetEffectiveAclServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID, GetEffectiveAclServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.methods","GET");
	    properties.put("sling.servlet.selectors",new String[]{"eacl","tidy.eacl"});
	    properties.put("sling.servlet.extensions","json");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),GetEffectiveAcl.class.getName()}, properties)
				.setImplementation(GetEffectiveAclServlet.class)
	            ;
		dm.add(component);
		
		//ModifyAceServlet
		properties = new Properties();
		properties.put(Constants.SERVICE_PID, ModifyAceServlet.class.getName());
		properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    properties.put("sling.servlet.resourceTypes","sling/servlet/default");
	    properties.put("sling.servlet.methods","POST");
	    properties.put("sling.servlet.selectors","modifyAce");
		component = dm.createComponent()
				.setInterface(new String[]{Servlet.class.getName(),ModifyAce.class.getName()}, properties)
				.setImplementation(ModifyAceServlet.class)
	            ;
		dm.add(component);
	}

}