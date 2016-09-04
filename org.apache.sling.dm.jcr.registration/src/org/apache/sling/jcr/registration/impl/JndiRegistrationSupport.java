/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.registration.impl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.sling.jcr.registration.AbstractRegistrationSupport;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;


/**
 * The <code>JndiRegistrationSupport</code> extends the
 * {@link AbstractRegistrationSupport} class to register repositories with a
 * JNDI context whose provider URL and initial factory class name may be
 * configured.
 * <p>
 * Note: Currently, only these two properties are declared to be configurable,
 * in the future a mechanism should be devised to support declaration of more
 * properties.
 */
public class JndiRegistrationSupport extends AbstractRegistrationSupport implements ManagedService {

    private Context jndiContext;
	private Dictionary<String, ?> properties;

    // ---------- SCR intergration ---------------------------------------------

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    @Override
    protected boolean doActivate() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
    	@SuppressWarnings("unchecked")
        Dictionary<String, Object> props = this.getComponentContext().getProperties();
        Properties env = new Properties();
        for (Enumeration<String> pe = props.keys(); pe.hasMoreElements();) {
            String key = pe.nextElement();
            if (key.startsWith("java.naming.")) {
                env.setProperty(key, (String) props.get(key));
            }
        }

        try {
            // create the JNDI context for registration
            this.jndiContext = this.createInitialContext(env);

            this.log(LogService.LOG_INFO, "Using JNDI context "
                + this.jndiContext.getEnvironment() + " to register repositories",
                null);

            return true;
        } catch (NamingException ne) {
            this.log(
                LogService.LOG_ERROR,
                "Problem setting up JNDI initial context, repositories will not be registered. Reason: "
                    + ne.getMessage(), null);
        }
        // fallback to false
        return false;
    }

    @Override
    protected void doDeactivate() {
        if (this.jndiContext != null) {
            try {
                this.jndiContext.close();
            } catch (NamingException ne) {
                this.log(LogService.LOG_INFO, "Problem closing JNDI context", ne);
            }

            this.jndiContext = null;
        }
    }

    private Context createInitialContext(final Properties env) throws NamingException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Context>() {
                public Context run() throws NamingException {
                    Thread currentThread = Thread.currentThread();
                    ClassLoader old = currentThread.getContextClassLoader();
                    currentThread.setContextClassLoader(JndiRegistrationSupport.this.getClass().getClassLoader());
                    try {
                        return new InitialContext(env);
                    } finally {
                        currentThread.setContextClassLoader(old);
                    }
                }
            });
        } catch (PrivilegedActionException pae) {
            // we now that this method only throws a NamingException
            throw (NamingException) pae.getCause();
        }
    }

    @Override
    protected Object bindRepository(String name, Repository repository) {

        if (this.jndiContext != null) {
            try {
                this.jndiContext.bind(name, repository);
                this.log(LogService.LOG_INFO, "Repository bound to JNDI as " + name,
                    null);
                return repository;
            } catch (NamingException ne) {
                this.log(LogService.LOG_ERROR, "Failed to register repository " + name, ne);
            }
        }

        // fall back to unregistered in case of failures or no context
        return null;
    }

    @Override
    protected void unbindRepository(String name, Object data) {
        if (this.jndiContext != null) {
            try {
                this.jndiContext.unbind(name);
                this.log(LogService.LOG_INFO, "Repository " + name
                    + " unbound from JNDI", null);
            } catch (NamingException ne) {
                this.log(LogService.LOG_ERROR, "Problem unregistering repository "
                    + name, ne);
            }
        }
    }
}
