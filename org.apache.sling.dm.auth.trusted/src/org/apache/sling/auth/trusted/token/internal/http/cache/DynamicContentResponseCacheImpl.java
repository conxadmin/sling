/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.sling.auth.trusted.token.internal.http.cache;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.apache.felix.dm.Component;
//import org.perf4j.aop.Profiled;
import org.apache.sling.auth.trusted.token.api.http.cache.DynamicContentResponseCache;
import org.apache.sling.auth.trusted.token.api.memory.Cache;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.api.memory.CacheScope;
import org.apache.sling.auth.trusted.token.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DynamicContentResponseCacheImpl implements DynamicContentResponseCache {

  static final String DISABLE_CACHE_FOR_UI_DEV = "disable.cache.for.dev.mode";

  static final String BYPASS_CACHE_FOR_LOCALHOST = "bypass.cache.for.localhost";

  protected volatile CacheManagerService cacheManagerService;

  private Cache<String> cache;

  private boolean disableForDevMode;

  private boolean bypassForLocalhost;

private Component component;

private Dictionary<Object, Object> properties;

  protected void init(Component component) {
	  this.component = component;
	  this.properties = this.component.getServiceProperties();
  }


  @SuppressWarnings("UnusedParameters")
  protected void activate() throws ServletException {
    cache = cacheManagerService.getCache(DynamicContentResponseCache.class.getName() + "-cache",
        CacheScope.INSTANCE);

    disableForDevMode = PropertiesUtil.toBoolean(properties.get(DISABLE_CACHE_FOR_UI_DEV), false);
    bypassForLocalhost = PropertiesUtil.toBoolean(properties.get(BYPASS_CACHE_FOR_LOCALHOST), true);
  }

  @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
  public void deactivate() {
    cache.clear();
  }

  @Override
  public void recordResponse(String cacheCategory, HttpServletRequest request, HttpServletResponse response) {
    if (isDisabled(request)) {
      return;
    }
    String key = buildCacheKey(cacheCategory, request.getRemoteUser());
    String etag = cache.get(key);
    if (etag == null) {
      etag = buildETag(request);
      saveEntry(cacheCategory, key, etag);
    }
    setHeaders(response, etag);
  }

  @Override
  public void invalidate(String cacheCategory, String userID) {
    if (disableForDevMode) {
      return;
    }
    String key = buildCacheKey(cacheCategory, userID);
    if (cache.containsKey(key)) {
      invalidateEntry(cacheCategory, key);
    }
    String wildcardKey = buildCacheKey("*", userID);
    if (cache.containsKey(wildcardKey)) {
      invalidateEntry("*", wildcardKey);
    }
  }

  @Override
  public boolean send304WhenClientHasFreshETag(String cacheCategory, HttpServletRequest request, HttpServletResponse response) {
    if (isDisabled(request)) {
      return false;
    }
    // examine client request for If-None-Match http header. compare that against the etag.
    String clientEtag = request.getHeader("If-None-Match");
    String serverEtag = cache.get(buildCacheKey(cacheCategory, request.getRemoteUser()));
    if (clientEtag != null && clientEtag.equals(serverEtag)) {
      hitEntry(cacheCategory, response);
      setHeaders(response, serverEtag);
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    if (cache != null) {
      cache.clear();
    }
  }

  private String buildETag(HttpServletRequest request) {
    String rawTag = request.getRemoteUser() + ':' + request.getPathInfo() + ':' + request.getQueryString()
        + ':' + System.nanoTime();
    try {
      return StringUtils.sha1Hash(rawTag);
    } catch (UnsupportedEncodingException e) {
      return rawTag;
    } catch (NoSuchAlgorithmException e) {
      return rawTag;
    }
  }

  String buildCacheKey(String cacheCategory, String userID) {
    return userID + ':' + cacheCategory;
  }
  
  //@Profiled(tag="http:DynamicContentResponseCache:save:{$0}", el=true)
  private void saveEntry(String cacheCategory, String key, String value) {
    cache.put(key, value);
  }
  
  //@Profiled(tag="http:DynamicResponseCache:invalidation:{$0}", el=true)
  private void invalidateEntry(String cacheCategory, String key) {
    cache.remove(key);
  }
  
  //@Profiled(tag="http:DynamicResponseCache:hit:{$0}")
  private void hitEntry(String cacheCategory, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
  }

  private boolean isDisabled(HttpServletRequest request) {
    return disableForDevMode || (bypassForLocalhost && ("localhost".equals(request.getServerName())
        || "127.0.0.1".equals(request.getServerName())));
  }

  private void setHeaders(HttpServletResponse response, String etag) {
    response.setHeader("ETag", etag);
    response.setHeader("Cache-Control", "private, must-revalidate, max-age=0");
  }
}
