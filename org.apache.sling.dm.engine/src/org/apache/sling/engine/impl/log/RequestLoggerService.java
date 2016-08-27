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
package org.apache.sling.engine.impl.log;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.RequestLog;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>RequestLoggerService</code> is a factory component which gets
 * configuration to register loggers for the {@link RequestLogger}.
 */
public class RequestLoggerService implements ManagedService {

    public static final String PARAM_FORMAT = "request.log.service.format";

    public static final String PARAM_OUTPUT = "request.log.service.output";

    public static final String PARAM_OUTPUT_TYPE = "request.log.service.outputtype";

    public static final String PARAM_ON_ENTRY = "request.log.service.onentry";

    private static final int OUTPUT_TYPE_LOGGER = 0;

    private static final int OUTPUT_TYPE_FILE = 1;

    private static final int OUTPUT_TYPE_CLASS = 2;

    private boolean onEntry;

    private CustomLogFormat logFormat;

    private RequestLog log;

	private Dictionary<String, ?> properties;
	
	private volatile BundleContext bundleContext;

    /**
     * Public default constructor for SCR integration
     */
    public RequestLoggerService() {
    }
    

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    RequestLoggerService(BundleContext bundleContext, Map<String, Object> configuration) {
    	this.properties = new Hashtable<>();
    	((Hashtable)this.properties).putAll(configuration);
    	
    	this.bundleContext = bundleContext;
    	
        this.setup();
    }

    void setup() {
    	if (this.properties != null) {
	        // whether to log on request entry or request exit
	        this.onEntry = PropertiesUtil.toBoolean(this.properties.get(PARAM_ON_ENTRY), false);
	
	        // shared or private CustomLogFormat
	        final String format = PropertiesUtil.toString(this.properties.get(PARAM_FORMAT), null);
	        if (format != null) {
	            this.logFormat = new CustomLogFormat(format);
	        }
	
	        // where to log to
	        final String output = PropertiesUtil.toString(this.properties.get(PARAM_OUTPUT), null);
	        if (output != null) {
	            final int outputType = PropertiesUtil.toInteger(this.properties.get(PARAM_OUTPUT_TYPE), OUTPUT_TYPE_LOGGER);
	            this.log = this.getLog(bundleContext, output, outputType);
	        }
    	}
    	else
    		System.out.println("Properties is NULL");
    }

    void shutdown() {
        if (this.log != null) {
            this.log.close();
            this.log = null;
        }

        this.logFormat = null;
    }

    void log(RequestLoggerRequest request, RequestLoggerResponse response) {
        if (this.log != null && this.logFormat != null) {
            this.log.write(this.logFormat.format(request, response));
        }
    }

    boolean isOnEntry() {
        return this.onEntry;
    }

    private RequestLog getLog(BundleContext bundleContext, String output, int outputType) {
        switch (outputType) {
            case OUTPUT_TYPE_FILE:
                // file logging
                try {
                    // ensure the path is absolute
                    File file = new File(output);
                    if (!file.isAbsolute()) {
                        final String home = bundleContext.getProperty("sling.home");
                        if (home != null) {
                            file = new File(home, output);
                        }
                        file = file.getAbsoluteFile();
                    }

                    return new FileRequestLog(file);
                } catch (IOException ioe) {
                    // TODO: log
                }
                break;

            case OUTPUT_TYPE_CLASS:
                // only try to use service if we have a bundle context
                if (bundleContext != null) {
                    return new RequestLogServiceFacade(bundleContext, output);
                }
                break;

            case OUTPUT_TYPE_LOGGER:
            default:
                return new LoggerRequestLog(output);
        }

        // fallback in case of issue or so...
        return null;
    }
}
