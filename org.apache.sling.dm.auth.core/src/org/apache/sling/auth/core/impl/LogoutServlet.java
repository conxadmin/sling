/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.auth.core.impl;

import java.util.Dictionary;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogoutServlet</code> lets the Authenticator
 * do the logout.
 */
public class LogoutServlet extends SlingAllMethodsServlet implements ManagedService {

    /** serialization UID */
    private static final long serialVersionUID = -1L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private volatile Authenticator authenticator;

    /**
     * The servlet is registered on this path.
     */
    public static final String SERVLET_PATH = "/system/sling/logout";
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		// TODO Auto-generated method stub
		
	}

    @Override
    protected void service(SlingHttpServletRequest request,
            SlingHttpServletResponse response) {

        final Authenticator authenticator = this.authenticator;
        if (authenticator != null) {
            try {
                AuthUtil.setLoginResourceAttribute(request, null);
                authenticator.logout(request, response);
                return;
            } catch (IllegalStateException ise) {
                log.error("service: Response already committed, cannot logout");
                return;
            }
        }

        log.error("service: Authenticator service missing, cannot logout");

        // well, we don't really have something to say here, do we ?
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

}
