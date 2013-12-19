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
package org.apache.sling.extensions.featureflags;

import java.util.Collection;

import aQute.bnd.annotation.ProviderType;

/**
 * The client context can be used by client code to check whether
 * a specific feature is enable.
 * A client context can be created through the {@link Features} service.
 */
@ProviderType
public interface ClientContext {

    /**
     * Returns <code>true</code> if the feature is enabled.
     */
    boolean isEnabled(String featureName);

    /**
     * Returns a list of all enabled features
     * @return The list of features, the list might be empty.
     */
    Collection<String> getEnabledFeatures();
}
