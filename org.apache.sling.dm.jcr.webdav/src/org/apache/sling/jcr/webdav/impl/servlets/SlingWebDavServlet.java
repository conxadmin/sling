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
package org.apache.sling.jcr.webdav.impl.servlets;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.io.CopyMoveHandler;
import org.apache.jackrabbit.server.io.DeleteHandler;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.webdav.impl.handler.SlingCopyMoveManager;
import org.apache.sling.jcr.webdav.impl.handler.SlingDeleteManager;
import org.apache.sling.jcr.webdav.impl.handler.SlingIOManager;
import org.apache.sling.jcr.webdav.impl.handler.SlingPropertyManager;
import org.apache.sling.jcr.webdav.impl.helper.SlingLocatorFactory;
import org.apache.sling.jcr.webdav.impl.helper.SlingResourceConfig;
import org.apache.sling.jcr.webdav.impl.helper.SlingSessionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * The <code>SlingWebDavServlet</code> implements the WebDAV protocol as a
 * default servlet for Sling handling all WebDAV methods.
 *
 */
public class SlingWebDavServlet extends SimpleWebdavServlet implements ManagedService {

    public static final String DEFAULT_CONTEXT = "/dav";

    public static final String PROP_CONTEXT = "dav.root";

    public static final boolean DEFAULT_CREATE_ABSOLUTE_URI = true;

    public static final String PROP_CREATE_ABSOLUTE_URI = "dav.create-absolute-uri";

    public static final String DEFAULT_REALM = "Sling WebDAV";

    public static final String PROP_REALM = "dav.realm";

    public static final String COLLECTION_TYPES = "collection.types";

    public static final String TYPE_NONCOLLECTIONS_DEFAULT = "nt:file";

    public static final String TYPE_CONTENT_DEFAULT = "nt:resource";

    public static final String[] COLLECTION_TYPES_DEFAULT = new String[] {
        TYPE_NONCOLLECTIONS_DEFAULT, TYPE_CONTENT_DEFAULT };

    public static final String FILTER_PREFIXES = "filter.prefixes";

    public static final String[] FILTER_PREFIXES_DEFAULT = new String[] {
        "rep", "jcr" };

    public static final String[] EMPTY_DEFAULT = new String[0];

    public static final String FILTER_TYPES = "filter.types";

    public static final String FILTER_URIS = "filter.uris";

    public static final String TYPE_COLLECTIONS_DEFAULT = "sling:Folder";

    public static final String TYPE_COLLECTIONS = "type.collections";

    public static final String TYPE_NONCOLLECTIONS = "type.noncollections";

    public static final String TYPE_CONTENT = "type.content";

    static final String IOHANDLER_REF_NAME = "IOHandler";

    static final String PROPERTYHANDLER_REF_NAME = "PropertyHandler";

    static final String COPYMOVEHANDLER_REF_NAME = "CopyMoveHandler";

    static final String DELETEHANDLER_REF_NAME = "DeleteHandler";

    private volatile SlingRepository repository;

    private volatile HttpService httpService;

    private volatile MimeTypeService mimeTypeService;

    private final SlingIOManager ioManager = new SlingIOManager(
        IOHANDLER_REF_NAME);

    private final SlingPropertyManager propertyManager = new SlingPropertyManager(
        PROPERTYHANDLER_REF_NAME);

    private final SlingCopyMoveManager copyMoveManager = new SlingCopyMoveManager(
        COPYMOVEHANDLER_REF_NAME);

    private final SlingDeleteManager deleteManager = new SlingDeleteManager(
            DELETEHANDLER_REF_NAME);

    private SlingResourceConfig resourceConfig;

    private DavLocatorFactory locatorFactory;

    private SessionProvider sessionProvider;

    private boolean simpleWebDavServletRegistered;

	private Dictionary<String, ?> properties;
	
	private volatile BundleContext context;

    // ---------- SimpleWebdavServlet overwrites -------------------------------

    @Override
    public void init() throws ServletException {
        super.init();

        setResourceConfig(resourceConfig);
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {

            // configured default workspace name
            SlingRepository slingRepo = (SlingRepository) getRepository();
            String workspace = slingRepo.getDefaultWorkspace();

            // no configuration, try to login and acquire the default name
            if (workspace == null || workspace.length() == 0) {
                Session tmp = null;
                try {
                    tmp = slingRepo.login();
                    workspace = tmp.getWorkspace().getName();
                } catch (Throwable t) {
                    // TODO: log !!
                    workspace = "default"; // fall back name
                } finally {
                    if (tmp != null) {
                        tmp.logout();
                    }
                }
            }

            locatorFactory = new SlingLocatorFactory(workspace);
        }
        return locatorFactory;
    }

    @Override
    public synchronized SessionProvider getSessionProvider() {
        if (sessionProvider == null) {
            sessionProvider = new SlingSessionProvider();
        }
        return sessionProvider;
    }

    // ---------- DM integration
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    protected void activate()
            throws NamespaceException, ServletException {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        this.ioManager.setComponentContext(context);
        this.propertyManager.setComponentContext(context);
        this.copyMoveManager.setComponentContext(context);
        this.deleteManager.setComponentContext(context);

        resourceConfig = new SlingResourceConfig(mimeTypeService,
            this.properties,
            ioManager,
            propertyManager,
            copyMoveManager,
            deleteManager);

        // Register servlet, and set the contextPath field to signal successful
        // registration
        Servlet simpleServlet = new SlingSimpleWebDavServlet(resourceConfig,
            getRepository());
        httpService.registerServlet(resourceConfig.getServletContextPath(),
            simpleServlet, resourceConfig.getServletInitParams(), null);
        simpleWebDavServletRegistered = true;
    }

    protected void deactivate(ComponentContext context) {

        if (simpleWebDavServletRegistered) {
            httpService.unregister(resourceConfig.getServletContextPath());
            simpleWebDavServletRegistered = false;
        }

        this.resourceConfig = null;
        this.ioManager.setComponentContext(null);
        this.propertyManager.setComponentContext(null);
        this.copyMoveManager.setComponentContext(null);
        this.deleteManager.setComponentContext(null);
    }

    public void bindIOHandler(final ServiceReference ioHandlerReference) {
        this.ioManager.bindIOHandler(ioHandlerReference);
    }

    public void unbindIOHandler(final ServiceReference ioHandlerReference) {
        this.ioManager.unbindIOHandler(ioHandlerReference);
    }

    public void bindPropertyHandler(final ServiceReference propertyHandlerReference) {
        this.propertyManager.bindPropertyHandler(propertyHandlerReference);
    }

    public void unbindPropertyHandler(final ServiceReference propertyHandlerReference) {
        this.propertyManager.unbindPropertyHandler(propertyHandlerReference);
    }

    public void bindCopyMoveHandler(final ServiceReference copyMoveHandlerReference) {
        this.copyMoveManager.bindCopyMoveHandler(copyMoveHandlerReference);
    }

    public void unbindCopyMoveHandler(final ServiceReference copyMoveHandlerReference) {
        this.copyMoveManager.unbindCopyMoveHandler(copyMoveHandlerReference);
    }

    public void bindDeleteHandler(final ServiceReference deleteHandlerReference) {
        this.deleteManager.bindDeleteHandler(deleteHandlerReference);
    }

    public void unbindDeleteHandler(final ServiceReference deleteHandlerReference) {
        this.deleteManager.unbindDeleteHandler(deleteHandlerReference);
    }

    /** Overridden as the base class uses sendError that we don't want (SLING-2443) */
    @Override
    protected void sendUnauthorized(WebdavRequest request, WebdavResponse response, DavException error) throws IOException {
        response.setHeader("WWW-Authenticate", getAuthenticateHeaderValue());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        if (error != null) {
            response.getWriter().write(error.getStatusPhrase());
            response.getWriter().write("\n");
        }
        response.getWriter().flush();
    }
}
