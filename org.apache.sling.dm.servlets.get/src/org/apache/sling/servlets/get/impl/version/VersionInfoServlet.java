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

package org.apache.sling.servlets.get.impl.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONRenderer;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>VersionInfoServlet</code> renders list of versions available for
 * the current resource.
 *
 * At the moment only JCR nodes are supported.
 */
public class VersionInfoServlet extends SlingSafeMethodsServlet implements ManagedService {

    private static final long serialVersionUID = 1656887064561951302L;

    /** Selector that means "pretty-print the output */
    public static final String TIDY = "tidy";

    /**
     * Selector that causes hierarchy to be rendered as arrays instead of child objects - useful to preserve
     * the order of those child objects
     */
    public static final String HARRAY = "harray";

    /** How much to indent in tidy mode */
    public static final int INDENT_SPACES = 2;

    private final JSONRenderer renderer = new JSONRenderer();

	private Dictionary<String, ?> properties;

    public void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException,
            IOException {
        resp.setContentType(req.getResponseContentType());
        resp.setCharacterEncoding("UTF-8");
        final boolean tidy = hasSelector(req, TIDY);
        final boolean harray = hasSelector(req, HARRAY);

        final JSONRenderer.Options opt = renderer.options().withIndent(tidy ? INDENT_SPACES : 0)
                .withArraysForChildren(harray);
        try {
            resp.getWriter().write(renderer.prettyPrint(getJsonObject(req.getResource()), opt));
        } catch (RepositoryException e) {
            throw new ServletException(e);
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    private JSONObject getJsonObject(Resource resource) throws RepositoryException, JSONException {
        final JSONObject result = new JSONObject();
        final Node node = resource.adaptTo(Node.class);
        if (node == null || !node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            return result;
        }

        final VersionHistory history = node.getVersionHistory();
        final Version baseVersion = node.getBaseVersion();
        for (final VersionIterator it = history.getAllVersions(); it.hasNext();) {
            final Version v = it.nextVersion();
            final JSONObject obj = new JSONObject();
            obj.put("created", createdDate(v));
            obj.put("successors", getNames(v.getSuccessors()));
            obj.put("predecessors", getNames(v.getPredecessors()));
            obj.put("labels", Arrays.asList(history.getVersionLabels(v)));
            obj.put("baseVersion", baseVersion.isSame(v));
            result.put(v.getName(), obj);
        }

        final JSONObject wrapper = new JSONObject();
        wrapper.put("versions", result);
        return wrapper;
    }

    private static Collection<String> getNames(Version[] versions) throws RepositoryException {
        final List<String> result = new ArrayList<String>();
        for (Version s : versions) {
            result.add(s.getName());
        }
        return result;
    }

    /** True if our request has the given selector */
    private boolean hasSelector(SlingHttpServletRequest req, String selectorToCheck) {
        for (String selector : req.getRequestPathInfo().getSelectors()) {
            if (selectorToCheck.equals(selector)) {
                return true;
            }
        }
        return false;
    }

    private static String createdDate(Node node) throws RepositoryException {
        return JsonItemWriter.format(node.getProperty(JcrConstants.JCR_CREATED).getDate());
    }

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

}
