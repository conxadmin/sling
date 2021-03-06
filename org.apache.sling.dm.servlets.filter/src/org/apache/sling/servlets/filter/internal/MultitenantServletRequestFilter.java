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
package org.apache.sling.servlets.filter.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>I18NFilter</code> class is a request level filter, which provides
 * the resource bundle for the current request.
 */
public class MultitenantServletRequestFilter implements Filter {

    /** Logger */
    private final static Logger LOG = LoggerFactory.getLogger(MultitenantServletRequestFilter.class.getName());

    /** Count the number init() has been called. */
    private volatile int initCount;

	private Component component;

	private Dictionary<Object, Object> properties;

	private String pathContext;
    
    protected void initComponent(Component component) {
    	this.component = component;
    	this.properties = this.component.getServiceProperties();
    	
    	this.pathContext = PropertiesUtil.toString(this.properties.get("sling.tenant.pid"), null);
    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        synchronized(this) {
            initCount++;
        }
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req,
                         final ServletResponse response,
                         final FilterChain chain)
    throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String requestURI = request.getRequestURI();
        final String pathContextWPath = "/"+pathContext;
        if (pathContext != null && requestURI.startsWith(pathContextWPath)) {
	        final HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
	            @Override
	            public StringBuffer getRequestURL() {
	                final StringBuffer originalUrl = ((HttpServletRequest) getRequest()).getRequestURL();
	                final String newUrl = StringUtils.replace(originalUrl.toString(),pathContextWPath,"");
					return  new StringBuffer(newUrl);
	            }
	            @Override
	            public String getPathInfo() {
					final String originalPath = ((HttpServletRequest)((HttpServletRequestWrapper)getRequest()).getRequest()).getPathInfo();
					final String newUrl = StringUtils.replace(originalPath,pathContextWPath,"");
					return  newUrl;
	            }
	            @Override
	            public String getServletPath() {
					final String originalPath = ((HttpServletRequest)((HttpServletRequestWrapper)getRequest()).getRequest()).getServletPath();
					final String newUrl = StringUtils.replace(originalPath,pathContextWPath,"");
					return  newUrl;
	            }
	        };
	        chain.doFilter(wrapped, response);
        }
        else
        	chain.doFilter(req, response);
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        synchronized(this) {
            initCount--;
        }
    }

}
