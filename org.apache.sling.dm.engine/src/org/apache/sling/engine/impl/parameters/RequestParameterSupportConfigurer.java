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
package org.apache.sling.engine.impl.parameters;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestParameterSupportConfigurer implements ManagedService {

    static final String PID = "org.apache.sling.engine.parameters";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(PID);

    public static final String PROP_FIX_ENCODING = "sling.default.parameter.encoding";

    public static final String PROP_MAX_PARAMS = "sling.default.max.parameters";

    public static final String PROP_FILE_LOCATION = "file.location";

    public static final String PROP_FILE_SIZE_THRESHOLD = "file.threshold";

    public static final String PROP_FILE_SIZE_MAX = "file.max";

    public static final String PROP_MAX_REQUEST_SIZE = "request.max";
    
    public static final String PROP_CHECK_ADDITIONAL_PARAMETERS = "sling.default.parameter.checkForAdditionalContainerParameters";

    private SlingSettingsService settignsService;
    
    private volatile BundleContext context;

	private Dictionary<String, ?> properties;
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    private void configure() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = (Dictionary<String, Object>) this.properties;

        final String fixEncoding = PropertiesUtil.toString(props.get(PROP_FIX_ENCODING), Util.ENCODING_DIRECT);
        final int maxParams = PropertiesUtil.toInteger(props.get(PROP_MAX_PARAMS), ParameterMap.DEFAULT_MAX_PARAMS);
        final long maxRequestSize = PropertiesUtil.toLong(props.get(PROP_MAX_REQUEST_SIZE), -1);
        final String fileLocation = getFileLocation(PropertiesUtil.toString(props.get(PROP_FILE_LOCATION), null));
        final long maxFileSize = PropertiesUtil.toLong(props.get(PROP_FILE_SIZE_MAX), -1);
        final int fileSizeThreshold = PropertiesUtil.toInteger(props.get(PROP_FILE_SIZE_THRESHOLD), -1);
        final boolean checkAddParameters = PropertiesUtil.toBoolean(props.get(PROP_CHECK_ADDITIONAL_PARAMETERS), false);

        if (log.isInfoEnabled()) {
            log.info("Default Character Encoding: {}", fixEncoding);
            log.info("Parameter Number Limit: {}", (maxParams < 0) ? "unlimited" : maxParams);
            log.info("Maximum Request Size: {}", (maxParams < 0) ? "unlimited" : maxRequestSize);
            log.info("Temporary File Location: {}", fileLocation);
            log.info("Maximum File Size: {}", maxFileSize);
            log.info("Tempory File Creation Threshold: {}", fileSizeThreshold);
            log.info("Check for additional container parameters: {}", checkAddParameters);
        }

        Util.setDefaultFixEncoding(fixEncoding);
        ParameterMap.setMaxParameters(maxParams);
        ParameterSupport.configure(maxRequestSize, fileLocation, maxFileSize,
                fileSizeThreshold, checkAddParameters);
    }

    private String getFileLocation(String fileLocation) {
        if (fileLocation != null) {
            File file = new File(fileLocation);
            if (!file.isAbsolute()) {
                file = new File(this.settignsService.getSlingHomePath(), fileLocation);
                fileLocation = file.getAbsolutePath();
            }
            if (file.exists()) {
                if (!file.isDirectory()) {
                    log.error(
                        "Configured temporary file location {} exists but is not a directory; using java.io.tmpdir instead",
                        fileLocation);
                    fileLocation = null;
                }
            } else {
                if (!file.mkdirs()) {
                    log.error("Cannot create temporary file directory {}; using java.io.tmpdir instead", fileLocation);
                    fileLocation = null;
                }
            }
        }
        return fileLocation;
    }
}
