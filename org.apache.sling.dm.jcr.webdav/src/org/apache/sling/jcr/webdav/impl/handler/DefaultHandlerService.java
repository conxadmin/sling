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
package org.apache.sling.jcr.webdav.impl.handler;

import org.apache.jackrabbit.server.io.DefaultHandler;
import org.apache.jackrabbit.server.io.DeleteContext;
import org.apache.jackrabbit.server.io.DeleteHandler;
import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.server.io.PropertyExportContext;
import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.server.io.PropertyImportContext;
import org.apache.jackrabbit.server.io.CopyMoveHandler;
import org.apache.jackrabbit.server.io.CopyMoveContext;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.webdav.impl.servlets.SlingWebDavServlet;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Wraps {@link org.apache.jackrabbit.server.io.DefaultHandler} in order to run
 * it as a service.
 */
public class DefaultHandlerService implements IOHandler, PropertyHandler, CopyMoveHandler,
        DeleteHandler, ManagedService {

    private DefaultHandler delegatee;
	private Dictionary<String, ?> properties;

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}
	
    @SuppressWarnings("unused")
    private void activate() {
    	if (this.properties == null)
    		this.properties = new Hashtable<>();
    	
        final String collectionType = OsgiUtil.toString(
            properties.get(SlingWebDavServlet.TYPE_COLLECTIONS),
            SlingWebDavServlet.TYPE_COLLECTIONS_DEFAULT);
        final String nonCollectionType = OsgiUtil.toString(
            properties.get(SlingWebDavServlet.TYPE_NONCOLLECTIONS),
            SlingWebDavServlet.TYPE_NONCOLLECTIONS_DEFAULT);
        final String contentType = OsgiUtil.toString(
            properties.get(SlingWebDavServlet.TYPE_CONTENT),
            SlingWebDavServlet.TYPE_CONTENT_DEFAULT);

        this.delegatee = new DefaultHandler(null, collectionType,
            nonCollectionType, contentType);
    }

    @SuppressWarnings("unused")
    private void deactivate() {
        this.delegatee = null;
    }

    @Override
	public IOManager getIOManager() {
        return delegatee.getIOManager();
    }

    @Override
	public void setIOManager(IOManager ioManager) {
        delegatee.setIOManager(ioManager);
    }

    @Override
	public String getName() {
        return delegatee.getName();
    }

    @Override
	public boolean canImport(ImportContext context, boolean isCollection) {
        return delegatee.canImport(context, isCollection);
    }

    @Override
	public boolean canImport(ImportContext context, DavResource resource) {
        return delegatee.canImport(context, resource);
    }

    @Override
	public boolean importContent(ImportContext context, boolean isCollection)
            throws IOException {
        return delegatee.importContent(context, isCollection);
    }

    @Override
	public boolean importContent(ImportContext context, DavResource resource)
            throws IOException {
        return delegatee.importContent(context, resource);
    }

    @Override
	public boolean canExport(ExportContext context, boolean isCollection) {
        return delegatee.canExport(context, isCollection);
    }

    @Override
	public boolean canExport(ExportContext context, DavResource resource) {
        return delegatee.canExport(context, resource);
    }

    @Override
	public boolean exportContent(ExportContext context, boolean isCollection)
            throws IOException {
        return delegatee.exportContent(context, isCollection);
    }

    @Override
	public boolean exportContent(ExportContext context, DavResource resource)
            throws IOException {
        return delegatee.exportContent(context, resource);
    }

    @Override
	public boolean canExport(PropertyExportContext context, boolean isCollection) {
        return delegatee.canExport(context, isCollection);
    }

    @Override
	public boolean exportProperties(PropertyExportContext exportContext,
            boolean isCollection) throws RepositoryException {
        return delegatee.exportProperties(exportContext, isCollection);
    }

    @Override
	public boolean canImport(PropertyImportContext context, boolean isCollection) {
        return delegatee.canImport(context, isCollection);
    }

    @Override
	public Map<? extends PropEntry, ?> importProperties(
            PropertyImportContext importContext, boolean isCollection)
            throws RepositoryException {
        return delegatee.importProperties(importContext, isCollection);
    }

    @Override
	public boolean canCopy(CopyMoveContext context, DavResource source, DavResource destination) {
        return delegatee.canCopy(context, source, destination);
    }

    @Override
	public boolean copy(CopyMoveContext context, DavResource source, DavResource destination) throws DavException {
        return delegatee.copy(context, source, destination);
    }

    @Override
	public boolean canMove(CopyMoveContext context, DavResource source, DavResource destination) {
        return delegatee.canMove(context, source, destination);
    }

    @Override
	public boolean move(CopyMoveContext context, DavResource source, DavResource destination) throws DavException {
      return delegatee.move(context, source, destination);
    }

    @Override
	public boolean delete(DeleteContext deleteContext, DavResource davResource)
            throws DavException {
        return delegatee.delete(deleteContext, davResource);
    }

    @Override
	public boolean canDelete(DeleteContext deleteContext,
            DavResource davResource) {
        return delegatee.canDelete(deleteContext, davResource);
    }

}
