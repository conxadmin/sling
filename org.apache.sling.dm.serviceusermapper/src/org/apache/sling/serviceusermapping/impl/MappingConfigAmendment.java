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
package org.apache.sling.serviceusermapping.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingConfigAmendment implements Comparable<MappingConfigAmendment>, ManagedService {
    private static final String PROP_SERVICE2USER_MAPPING = "user.mapping";

    private static final String[] PROP_SERVICE2USER_MAPPING_DEFAULT = {};

    /** default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Mapping[] serviceUserMappings;

    private int serviceRanking;

	private Dictionary<String, ?> properties;
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    void configure() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        final String[] props = PropertiesUtil.toStringArray(this.properties.get(PROP_SERVICE2USER_MAPPING),
            PROP_SERVICE2USER_MAPPING_DEFAULT);

        final ArrayList<Mapping> mappings = new ArrayList<Mapping>(props.length);
        for (final String prop : props) {
            if (prop != null && prop.trim().length() > 0 ) {
                try {
                    final Mapping mapping = new Mapping(prop.trim());
                    mappings.add(mapping);
                } catch (final IllegalArgumentException iae) {
                    logger.info("configure: Ignoring '{}': {}", prop, iae.getMessage());
                }
            }
        }

        this.serviceUserMappings = mappings.toArray(new Mapping[mappings.size()]);
        this.serviceRanking = PropertiesUtil.toInteger(this.properties.get(Constants.SERVICE_RANKING), 0);
    }

    public Mapping[] getServiceUserMappings() {
        return this.serviceUserMappings;
    }

    public int compareTo(final MappingConfigAmendment o) {
        // Sort by rank in descending order.
        if ( this.serviceRanking > o.serviceRanking ) {
            return -1; // lower rank
        } else if (this.serviceRanking < o.serviceRanking) {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by hash code
        return this.hashCode() < o.hashCode() ? -1 : 1;
    }
}
