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
package org.apache.sling.resourceresolver.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.runtime.RuntimeService;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.observation.ResourceChangeListenerWhiteboard;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker.ChangeListener;
import org.apache.sling.resourceresolver.impl.providers.RuntimeServiceImpl;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceResolverFactoryActivator/code> keeps track of required services for the
 * resource resolver factory.
 * One all required providers and provider factories are available a resource resolver factory
 * is registered.
 *
 */
public class ResourceResolverFactoryActivator implements ManagedService {
	
	public final static String PID = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl";

    private static final class FactoryRegistration {
        /** Registration .*/
        public volatile ServiceRegistration factoryRegistration;

        /** Runtime registration. */
        public volatile ServiceRegistration runtimeRegistration;

        public volatile CommonResourceResolverFactoryImpl commonFactory;
    }

    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced as per the generic
     * substitution pattern <code>/([^:]+):/_$1_/</code> when calling the <code>map</code> method of
     * the resource resolver. Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substitution pattern <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild which cannot cope
     * with URLs containing colons, even though they are perfectly valid characters in the path part
     * of URI references with a scheme.
     * <p>
     * The default value of this property if no configuration is provided is <code>true</code>.
     *
     */
    private static final String PROP_MANGLE_NAMESPACES = "resource.resolver.manglenamespaces";

    private static final String PROP_ALLOW_DIRECT = "resource.resolver.allowDirect";

    private static final String PROP_REQUIRED_PROVIDERS_LEGACY = "resource.resolver.required.providers";

    private static final String PROP_REQUIRED_PROVIDERS = "resource.resolver.required.providernames";

    /**
     * The resolver.virtual property has no default configuration. But the Sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     */
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    private static final String PROP_MAPPING = "resource.resolver.mapping";

    private static final String PROP_MAP_LOCATION = "resource.resolver.map.location";

    private static final String PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS = "resource.resolver.default.vanity.redirect.status";

    private static final boolean DEFAULT_ENABLE_VANITY_PATH = true;

    private static final String PROP_ENABLE_VANITY_PATH = "resource.resolver.enable.vanitypath";

    private static final long DEFAULT_MAX_CACHED_VANITY_PATHS = -1;

    private static final String PROP_MAX_CACHED_VANITY_PATHS = "resource.resolver.vanitypath.maxEntries";

    private static final boolean DEFAULT_MAX_CACHED_VANITY_PATHS_STARTUP = true;

    private static final String PROP_MAX_CACHED_VANITY_PATHS_STARTUP = "resource.resolver.vanitypath.maxEntries.startup";

    private static final int DEFAULT_VANITY_BLOOM_FILTER_MAX_BYTES = 1024000;

    private static final String PROP_VANITY_BLOOM_FILTER_MAX_BYTES = " resource.resolver.vanitypath.bloomfilter.maxBytes";

    private static final boolean DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION = true;

    private static final String PROP_ENABLE_OPTIMIZE_ALIAS_RESOLUTION = "resource.resolver.optimize.alias.resolution";

    private static final String PROP_ALLOWED_VANITY_PATH_PREFIX = "resource.resolver.vanitypath.whitelist";

    private static final String PROP_DENIED_VANITY_PATH_PREFIX = "resource.resolver.vanitypath.blacklist";

    private static final boolean DEFAULT_VANITY_PATH_PRECEDENCE = false;

    private static final String PROP_VANITY_PATH_PRECEDENCE = "resource.resolver.vanity.precedence";

    private static final boolean DEFAULT_PARANOID_PROVIDER_HANDLING = false;

    private static final String PROP_PARANOID_PROVIDER_HANDLING = "resource.resolver.providerhandling.paranoid";

    private static final boolean DEFAULT_LOG_RESOURCE_RESOLVER_CLOSING = false;

    private static final String PROP_LOG_RESOURCE_RESOLVER_CLOSING = "resource.resolver.log.closing";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    /** all mappings */
    private volatile Mapping[] mappings;

    /** The fake URLs */
    private volatile BidiMap virtualURLMap;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private volatile boolean allowDirect = false;

    /** the search path for ResourceResolver.getResource(String) */
    private volatile String[] searchPath;

    /** the root location of the /etc/map entries */
    private volatile String mapRoot;

    /** whether to mangle paths with namespaces or not */
    private volatile boolean mangleNamespacePrefixes;

    /** paranoid provider handling. */
    private volatile boolean paranoidProviderHandling;

    /** Event admin. */
    volatile EventAdmin eventAdmin;

    /** Service User Mapper */
    volatile private ServiceUserMapper serviceUserMapper;

    volatile ResourceAccessSecurityTracker resourceAccessSecurityTracker;

    volatile ResourceProviderTracker resourceProviderTracker;

    volatile ResourceChangeListenerWhiteboard changeListenerWhiteboard;

    /** ComponentContext */
    private volatile BundleContext componentContext;

    private volatile int defaultVanityPathRedirectStatus;

    /** vanityPath enabled? */
    private volatile boolean enableVanityPath = DEFAULT_ENABLE_VANITY_PATH;

    /** alias resource resolution optimization enabled? */
    private volatile boolean enableOptimizeAliasResolution = DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION;

    /** max number of cache vanity path entries */
    private volatile long maxCachedVanityPathEntries = DEFAULT_MAX_CACHED_VANITY_PATHS;

    /** limit max number of cache vanity path entries only at startup*/
    private volatile boolean maxCachedVanityPathEntriesStartup = DEFAULT_MAX_CACHED_VANITY_PATHS_STARTUP;

    /** Maximum number of vanity bloom filter bytes */
    private volatile int vanityBloomFilterMaxBytes = DEFAULT_VANITY_BLOOM_FILTER_MAX_BYTES;

    /** vanity paths will have precedence over existing /etc/map mapping? */
    private volatile boolean vanityPathPrecedence = DEFAULT_VANITY_PATH_PRECEDENCE;

    /** log the place where a resource resolver is closed */
    private volatile boolean logResourceResolverClosing = DEFAULT_LOG_RESOURCE_RESOLVER_CLOSING;

    /** Vanity path whitelist */
    private volatile String[] vanityPathWhiteList;

    /** Vanity path blacklist */
    private volatile String[] vanityPathBlackList;

    private final FactoryPreconditions preconds = new FactoryPreconditions();

    /** Factory registration. */
    private volatile FactoryRegistration factoryRegistration;

	private Dictionary<String, ?> properties;

    /**
     * Get the resource decorator tracker.
     */
    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.resourceDecoratorTracker;
    }

    public ResourceAccessSecurityTracker getResourceAccessSecurityTracker() {
        return this.resourceAccessSecurityTracker;
    }

    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public BidiMap getVirtualURLMap() {
        return virtualURLMap;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public Mapping[] getMappings() {
        return mappings;
    }

    public String[] getSearchPath() {
        return searchPath;
    }

    public boolean isMangleNamespacePrefixes() {
        return mangleNamespacePrefixes;

    }

    public String getMapRoot() {
        return mapRoot;
    }

    public int getDefaultVanityPathRedirectStatus() {
        return defaultVanityPathRedirectStatus;
    }

    public boolean isVanityPathEnabled() {
        return this.enableVanityPath;
    }

    public boolean isOptimizeAliasResolutionEnabled() {
        return this.enableOptimizeAliasResolution;
    }

    public String[] getVanityPathWhiteList() {
        return this.vanityPathWhiteList;
    }

    public String[] getVanityPathBlackList() {
        return this.vanityPathBlackList;
    }

    public boolean hasVanityPathPrecedence() {
        return this.vanityPathPrecedence;
    }

    public long getMaxCachedVanityPathEntries() {
        return this.maxCachedVanityPathEntries;
    }

    public boolean isMaxCachedVanityPathEntriesStartup() {
        return this.maxCachedVanityPathEntriesStartup;
    }

    public int getVanityBloomFilterMaxBytes() {
        return this.vanityBloomFilterMaxBytes;
    }

    public boolean shouldLogResourceResolverClosing() {
        return logResourceResolverClosing;
    }

    // ---------- DM Integration ---------------------------------------------

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}
    /**
     * Activates this component (called by SCR before)
     */
    protected void activate() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        final BidiMap virtuals = new TreeBidiMap();
        final String[] virtualList = PropertiesUtil.toStringArray(properties.get(PROP_VIRTUAL));
        for (int i = 0; virtualList != null && i < virtualList.length; i++) {
            final String[] parts = Mapping.split(virtualList[i]);
            virtuals.put(parts[0], parts[2]);
        }
        virtualURLMap = virtuals;

        final List<Mapping> maps = new ArrayList<Mapping>();
        final String[] mappingList = (String[]) this.properties.get(PROP_MAPPING);
        for (int i = 0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(mappingList[i]));
        }
        final Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        final Boolean directProp = (Boolean) this.properties.get(PROP_ALLOW_DIRECT);
        allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (allowDirect) {
            final Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = Mapping.DIRECT;
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }

        // from configuration if available
        searchPath = PropertiesUtil.toStringArray(properties.get(PROP_PATH));
        if (searchPath != null && searchPath.length > 0) {
            for (int i = 0; i < searchPath.length; i++) {
                // ensure leading slash
                if (!searchPath[i].startsWith("/")) {
                    searchPath[i] = "/" + searchPath[i];
                }
                // ensure trailing slash
                if (!searchPath[i].endsWith("/")) {
                    searchPath[i] += "/";
                }
            }
        }
        if (searchPath == null) {
            searchPath = new String[] { "/" };
        }
        // namespace mangling
        mangleNamespacePrefixes = PropertiesUtil.toBoolean(properties.get(PROP_MANGLE_NAMESPACES), false);

        // the root of the resolver mappings
        mapRoot = PropertiesUtil.toString(properties.get(PROP_MAP_LOCATION), MapEntries.DEFAULT_MAP_ROOT);

        defaultVanityPathRedirectStatus = PropertiesUtil.toInteger(properties.get(PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS),
                                                                   MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS);
        this.enableVanityPath = PropertiesUtil.toBoolean(properties.get(PROP_ENABLE_VANITY_PATH), DEFAULT_ENABLE_VANITY_PATH);
        // vanity path white list
        this.vanityPathWhiteList = null;
        String[] vanityPathPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_ALLOWED_VANITY_PATH_PREFIX));
        if ( vanityPathPrefixes != null ) {
            final List<String> prefixList = new ArrayList<String>();
            for(final String value : vanityPathPrefixes) {
                if ( value.trim().length() > 0 ) {
                    if ( value.trim().endsWith("/") ) {
                        prefixList.add(value.trim());
                    } else {
                        prefixList.add(value.trim() + "/");
                    }
                }
            }
            if ( prefixList.size() > 0 ) {
                this.vanityPathWhiteList = prefixList.toArray(new String[prefixList.size()]);
            }
        }
        // vanity path black list
        this.vanityPathBlackList = null;
        vanityPathPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_DENIED_VANITY_PATH_PREFIX));
        if ( vanityPathPrefixes != null ) {
            final List<String> prefixList = new ArrayList<String>();
            for(final String value : vanityPathPrefixes) {
                if ( value.trim().length() > 0 ) {
                    if ( value.trim().endsWith("/") ) {
                        prefixList.add(value.trim());
                    } else {
                        prefixList.add(value.trim() + "/");
                    }
                }
            }
            if ( prefixList.size() > 0 ) {
                this.vanityPathBlackList = prefixList.toArray(new String[prefixList.size()]);
            }
        }

        this.enableOptimizeAliasResolution = PropertiesUtil.toBoolean(properties.get(PROP_ENABLE_OPTIMIZE_ALIAS_RESOLUTION), DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION);
        this.maxCachedVanityPathEntries = PropertiesUtil.toLong(properties.get(PROP_MAX_CACHED_VANITY_PATHS), DEFAULT_MAX_CACHED_VANITY_PATHS);
        this.maxCachedVanityPathEntriesStartup = PropertiesUtil.toBoolean(properties.get(PROP_MAX_CACHED_VANITY_PATHS_STARTUP), DEFAULT_MAX_CACHED_VANITY_PATHS_STARTUP);
        this.vanityBloomFilterMaxBytes = PropertiesUtil.toInteger(properties.get(PROP_VANITY_BLOOM_FILTER_MAX_BYTES), DEFAULT_VANITY_BLOOM_FILTER_MAX_BYTES);

        this.vanityPathPrecedence = PropertiesUtil.toBoolean(properties.get(PROP_VANITY_PATH_PRECEDENCE), DEFAULT_VANITY_PATH_PRECEDENCE);
        this.logResourceResolverClosing = PropertiesUtil.toBoolean(properties.get(PROP_LOG_RESOURCE_RESOLVER_CLOSING),
            DEFAULT_LOG_RESOURCE_RESOLVER_CLOSING);
        this.paranoidProviderHandling = PropertiesUtil.toBoolean(properties.get(PROP_PARANOID_PROVIDER_HANDLING), DEFAULT_PARANOID_PROVIDER_HANDLING);

        final BundleContext bc = componentContext;

        // check for required property
        final String[] requiredResourceProvidersLegacy = PropertiesUtil.toStringArray(properties.get(PROP_REQUIRED_PROVIDERS_LEGACY));
        final String[] requiredResourceProviderNames = PropertiesUtil.toStringArray(properties.get(PROP_REQUIRED_PROVIDERS));

        if ( requiredResourceProvidersLegacy != null && requiredResourceProvidersLegacy.length > 0 ) {
            boolean hasRealValue = false;
            for(final String name : requiredResourceProvidersLegacy) {
                if ( name != null && !name.trim().isEmpty() ) {
                    hasRealValue = true;
                    break;
                }
            }
            if ( hasRealValue ) {
                logger.error("ResourceResolverFactory is using deprecated required providers configuration (" + PROP_REQUIRED_PROVIDERS_LEGACY +
                        "). Please change to use the property " + PROP_REQUIRED_PROVIDERS + " for values: " + Arrays.toString(requiredResourceProvidersLegacy));
            }
        }
        // for testing: if we run unit test, both trackers are set from the outside
        if ( this.resourceProviderTracker == null ) {
            this.resourceProviderTracker = new ResourceProviderTracker();
            this.changeListenerWhiteboard = new ResourceChangeListenerWhiteboard();
            this.preconds.activate(bc, requiredResourceProvidersLegacy, requiredResourceProviderNames, resourceProviderTracker);
            this.changeListenerWhiteboard.activate(this.componentContext,
                this.resourceProviderTracker, searchPath);
            this.resourceProviderTracker.activate(this.componentContext,
                    this.eventAdmin,
                    new ChangeListener() {

                        @Override
                        public void providerAdded() {
                            if ( factoryRegistration == null ) {
                                checkFactoryPreconditions(null, null);
                            }

                        }

                        @Override
                        public void providerRemoved(final String name, final String pid, final boolean stateful, final boolean isUsed) {
                            if ( factoryRegistration != null ) {
                                if ( isUsed && (stateful || paranoidProviderHandling) ) {
                                    unregisterFactory();
                                }
                                checkFactoryPreconditions(name, pid);
                            }
                        }
                    });
        } else {
            this.preconds.activate(bc, requiredResourceProvidersLegacy, requiredResourceProviderNames, resourceProviderTracker);
            this.checkFactoryPreconditions(null, null);
         }
    }

    /**
     * Modifies this component (called by SCR to update this component)
     */
    protected void modified() {
        this.deactivate();
        this.activate();
    }

    /**
     * Deactivates this component (called by SCR to take out of service)
     */
    protected void deactivate() {
        this.unregisterFactory();

        this.componentContext = null;

        this.changeListenerWhiteboard.deactivate();
        this.changeListenerWhiteboard = null;
        this.resourceProviderTracker.deactivate();
        this.resourceProviderTracker = null;

        this.preconds.deactivate();
        this.resourceDecoratorTracker.close();

        // this is just a sanity call to make sure that unregister
        // in the case that a registration happened again
        // while deactivation
        this.unregisterFactory();
    }

    /**
     * Unregister the factory (if registered)
     * This method might be called concurrently from deactivate and the
     * background thread, therefore we need to synchronize.
     */
    private void unregisterFactory() {
        FactoryRegistration local = null;
        synchronized ( this ) {
            if ( this.factoryRegistration != null ) {
                local = this.factoryRegistration;
                this.factoryRegistration = null;
            }
        }
        this.unregisterFactory(local);
    }

    /**
     * Unregister the provided factory
     */
    private void unregisterFactory(final FactoryRegistration local) {
        if ( local != null ) {
            if ( local.factoryRegistration != null ) {
                local.factoryRegistration.unregister();
            }
            if ( local.runtimeRegistration != null ) {
                local.runtimeRegistration.unregister();
            }
            if ( local.commonFactory != null ) {
                local.commonFactory.deactivate();
            }
        }
    }

    /**
     * Try to register the factory.
     */
    private void registerFactory(final BundleContext localContext) {
        final FactoryRegistration local = new FactoryRegistration();

        if ( localContext != null ) {
            // activate and register factory
            final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
            serviceProps.put(Constants.SERVICE_VENDOR, localContext.getProperty(Constants.SERVICE_VENDOR));
            serviceProps.put(Constants.SERVICE_DESCRIPTION,  localContext.getProperty(Constants.SERVICE_DESCRIPTION));

            local.commonFactory = new CommonResourceResolverFactoryImpl(this);
            local.commonFactory.activate(localContext);
            local.factoryRegistration = localContext.registerService(
                ResourceResolverFactory.class.getName(), new ServiceFactory() {

                    @Override
                    public Object getService(final Bundle bundle, final ServiceRegistration registration) {
                        if ( ResourceResolverFactoryActivator.this.componentContext == null ) {
                            return null;
                        }
                        final ResourceResolverFactoryImpl r = new ResourceResolverFactoryImpl(
                                local.commonFactory, bundle,
                            ResourceResolverFactoryActivator.this.serviceUserMapper);
                        return r;
                    }

                    @Override
                    public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service) {
                        // nothing to do
                    }
                }, serviceProps);

            local.runtimeRegistration = localContext.registerService(RuntimeService.class.getName(),
                    this.getRuntimeService(), null);

            this.factoryRegistration = local;
        }
    }

    /**
     * Get the runtime service
     * @return The runtime service
     */
    public RuntimeService getRuntimeService() {
        return new RuntimeServiceImpl(this.resourceProviderTracker);
    }

    /**
     * Check the preconditions and if it changed, either register factory or unregister
     */
    private void checkFactoryPreconditions(final String unavailableName, final String unavailableServicePid) {
        if ( this.componentContext != null ) {
            final boolean result = this.preconds.checkPreconditions(unavailableName, unavailableServicePid);
            if ( result && this.factoryRegistration == null ) {
                // check system bundle state - if stopping, don't register new factory
                final Bundle systemBundle = componentContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                if ( systemBundle != null && systemBundle.getState() != Bundle.STOPPING ) {
                    boolean create = true;
                    synchronized ( this ) {
                        if ( this.factoryRegistration == null ) {
                            this.factoryRegistration = new FactoryRegistration();
                        } else {
                            create = false;
                        }
                    }
                    if ( create ) {
                        this.registerFactory(this.componentContext);
                    }
                }
            } else if ( !result && this.factoryRegistration != null ) {
                this.unregisterFactory();
            }
        }
    }

    /**
     * Bind a resource decorator.
     */
    protected void bindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.bindResourceDecorator(decorator, props);
    }

    /**
     * Unbind a resource decorator.
     */
    protected void unbindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.unbindResourceDecorator(decorator, props);
    }

    /**
     * Get the resource provider tracker
     * @return The tracker
     */
    public ResourceProviderTracker getResourceProviderTracker() {
        return resourceProviderTracker;
    }
}
