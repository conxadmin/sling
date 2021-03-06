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
package org.apache.sling.jcr.resource.internal;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link org.apache.sling.serviceusermapping.ServiceUserValidator}
 * interface that verifies that all registered service users are represented by
 * {@link org.apache.jackrabbit.api.security.user.User#isSystemUser() system users}
 * in the underlying JCR repository.
 *
 * @see org.apache.jackrabbit.api.security.user.User#isSystemUser()
 */
public class JcrSystemUserValidator implements ServiceUserValidator, ManagedService {

    /**
     * logger instance
     */
    private final Logger log = LoggerFactory.getLogger(JcrSystemUserValidator.class);

    private volatile SlingRepository repository;
    
    public static final boolean PROP_ALLOW_ONLY_SYSTEM_USERS_DEFAULT = true;
    
    public static final String PROP_ALLOW_ONLY_SYSTEM_USERS = "allow.only.system.user";

    private final Method isSystemUserMethod;

    private final Set<String> validIds = new CopyOnWriteArraySet<String>();
    
    private boolean allowOnlySystemUsers;

	private Dictionary<String, ?> properties;

    public JcrSystemUserValidator() {
        Method m = null;
        try {
            m = User.class.getMethod("isSystemUser");
        } catch (Exception e) {
            log.debug("Exception while accessing isSystemUser method", e);
        }
        isSystemUserMethod = m;
    }
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    public void activate() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
    	allowOnlySystemUsers = PropertiesUtil.toBoolean(this.properties.get(PROP_ALLOW_ONLY_SYSTEM_USERS),PROP_ALLOW_ONLY_SYSTEM_USERS_DEFAULT);
    }

    public boolean isValid(final String serviceUserId, final String serviceName, final String subServiceName) {
        if (serviceUserId == null) {
            log.debug("The provided service user id is null");
            return false;
        }
        if (!allowOnlySystemUsers) {
            log.debug("There is no enforcement of JCR system users, therefore service user id '{}' is valid", serviceUserId);
            return true;
        }
        if (validIds.contains(serviceUserId)) {
            log.debug("The provided service user id '{}' has been already validated and is a known JCR system user id", serviceUserId);
            return true;
        } else {
            Session administrativeSession = null;
            try {
                try {
                    /*
                     * TODO: Instead of using the deprecated loginAdministrative
                     * method, this bundle could be configured with an appropriate
                     * user for service authentication and do:
                     *     tmpSession = repository.loginService(null, workspace);
                     * For now, we keep loginAdministrative
                     */
                    administrativeSession = repository.loginAdministrative(null);
                    if (administrativeSession instanceof JackrabbitSession) {
                        final UserManager userManager = ((JackrabbitSession) administrativeSession).getUserManager();
                        final Authorizable authorizable = userManager.getAuthorizable(serviceUserId);
                        if (authorizable != null && !authorizable.isGroup() && (isSystemUser((User)authorizable))) {
                            validIds.add(serviceUserId);
                            log.debug("The provided service user id {} is a known JCR system user id", serviceUserId);
                            return true;
                        }
                    }
                } catch (final RepositoryException e) {
                    log.warn("Could not get user information", e);
                }
            } finally {
                if (administrativeSession != null) {
                    administrativeSession.logout();
                }
            }
            log.warn("The provided service user id '{}' is not a known JCR system user id and therefore not allowed in the Sling Service User Mapper.", serviceUserId);
            return false;
        }
    }


    private boolean isSystemUser(final User user){
        if (isSystemUserMethod != null) {
            try {
                return (Boolean) isSystemUserMethod.invoke(user);
            } catch (Exception e) {
                log.debug("Exception while invoking isSystemUser method", e);
                return true;
            }
         } else {
             return true;
         }
    }
}