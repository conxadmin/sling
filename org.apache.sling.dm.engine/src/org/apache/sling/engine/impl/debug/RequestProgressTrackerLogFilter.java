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
package org.apache.sling.engine.impl.debug;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.engine.impl.request.SlingRequestProgressTracker;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that dumps the output of the RequestProgressTracker to the log after
 * processing the request.
 *
 */
public class RequestProgressTrackerLogFilter implements Filter, ManagedService {
    public static @interface Config {
        String[] extensions() default {};

        int minDurationMs() default 0;

        int maxDurationMs() default Integer.MAX_VALUE;

        boolean compactLogFormat() default false;
    }

    private final Logger log = LoggerFactory.getLogger(RequestProgressTrackerLogFilter.class);

    private int requestCounter;

    private Config configuration;

    private String[] extensions;

	private Dictionary<String, ?> properties;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);

        if (request instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            final RequestProgressTracker rpt = slingRequest.getRequestProgressTracker();
            rpt.done();

            if (log.isDebugEnabled() && allowDuration(rpt) && allowExtension(extractExtension(slingRequest))) {
                if (configuration.compactLogFormat()) {
                    logCompactFormat(rpt);
                } else {
                    logDefaultFormat(rpt);
                }
            }
        }
    }

    @Override
    public void destroy() {
    }

    private void logCompactFormat(RequestProgressTracker rpt) {
        final Iterator<String> messages = rpt.getMessages();
        final StringBuilder sb = new StringBuilder("\n");
        while (messages.hasNext()) {
            sb.append(messages.next());
        }
        sb.setLength(sb.length() - 1);
        log.debug(sb.toString());
    }

    private void logDefaultFormat(RequestProgressTracker rpt) {
        int requestId = 0;
        synchronized (getClass()) {
            requestId = ++requestCounter;
        }
        final Iterator<String> it = rpt.getMessages();
        while (it.hasNext()) {
            log.debug("REQUEST_{} - " + it.next(), requestId);
        }
    }

    private String extractExtension(SlingHttpServletRequest request) {
        final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String extension = requestPathInfo.getExtension();
        if (extension == null) {
            final String pathInfo = requestPathInfo.getResourcePath();
            final int extensionIndex = pathInfo.lastIndexOf('.') + 1;
            extension = pathInfo.substring(extensionIndex);
        }
        return extension;
    }

    private boolean allowExtension(final String extension) {
        return extensions == null
               || extensions.length == 0
               || Arrays.binarySearch(extensions, extension) > -1;
    }

    private boolean allowDuration(final RequestProgressTracker rpt) {
        if (rpt instanceof SlingRequestProgressTracker) {
            long duration = ((SlingRequestProgressTracker) rpt).getDuration();
            return configuration.minDurationMs() <= duration && duration <= configuration.maxDurationMs();
        } else {
            log.debug("Logged requests can only be filtered by duration if the SlingRequestProgressTracker is used.");
            return true;
        }

    }

    /**
     * Sorts the String array and removes any empty values, which are
     * assumed to be sorted to the beginning.
     *
     * @param strings An array of Strings
     * @return The sorted array with empty strings removed.
     */
    private String[] sortAndClean(String[] strings) {
        if (strings == null || strings.length == 0) {
            return strings;
        }

        Arrays.sort(strings);
        int skip = 0;
        for (int i = strings.length - 1; i > -1; i--) {
            // skip all empty strings that are sorted to the beginning of the array
            if (strings[i].length() == 0) {
                skip++;
            }
        }
        return skip == 0 ? strings : Arrays.copyOfRange(strings, skip, strings.length);
    }

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}
	
    private void activate() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        this.configuration = new Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] extensions() {
				return (String[]) RequestProgressTrackerLogFilter.this.properties.get("extensions");
			}

			@Override
			public int minDurationMs() {
				return (int) RequestProgressTrackerLogFilter.this.properties.get("minDurationMs");
			}

			@Override
			public int maxDurationMs() {
				return (int) RequestProgressTrackerLogFilter.this.properties.get("maxDurationMs");
			}

			@Override
			public boolean compactLogFormat() {
				return (boolean) RequestProgressTrackerLogFilter.this.properties.get("compactLogFormat");
			}
        	
        };
        // extensions needs to be sorted for Arrays.binarySearch() to work
        this.extensions = sortAndClean(this.configuration.extensions());
        log.debug("activated: extensions = {}, min = {}, max = {}, compact = {}",
                new Object[]{extensions, configuration.minDurationMs(), configuration.maxDurationMs(), configuration.compactLogFormat()});
    }

}