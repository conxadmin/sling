/**
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
package org.apache.sling.auth.trusted.token.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.apache.sling.auth.trusted.token.api.TokenTrustValidator;
import org.apache.sling.auth.trusted.token.api.TrustedTokenService;
import org.apache.sling.auth.trusted.token.api.TrustedTokenTypes;
import org.apache.sling.auth.trusted.token.api.cluster.ClusterTrackingService;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.util.Signature;
import org.apache.sling.auth.trusted.token.api.servlet.HttpOnlyCookie;
import org.apache.sling.auth.trusted.token.internal.TokenStore.SecureCookie;
import org.apache.sling.auth.trusted.token.internal.TokenStore.SecureCookieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
public final class TrustedTokenServiceImpl implements TrustedTokenService {
  private static final Logger LOG = LoggerFactory.getLogger(TrustedTokenServiceImpl.class);

  /** Property to invalidate if the session should be used. */
  public static final String USE_SESSION = "sakai.auth.trusted.token.usesession";

  /** Property to indicate if only cookies should be secure */
  public static final String SECURE_COOKIE = "sakai.auth.trusted.token.securecookie";

  /** Property to indicate the TTL on cookies */
  public static final String TTL = "sakai.auth.trusted.token.ttl";

  /** Property to indicate the name of the cookie. */
  public static final String COOKIE_NAME = "sakai.auth.trusted.token.name";

  /** Property to point to keystore file */
  public static final String TOKEN_FILE_NAME = "sakai.auth.trusted.token.storefile";

  /** Property to contain the shared secret used by all trusted servers */
  public static final String SERVER_TOKEN_SHARED_SECRET = "sakai.auth.trusted.server.secret";

  /** True if server tokens are enabled. */
  public static final String SERVER_TOKEN_ENABLED = "sakai.auth.trusted.server.enabled";

  /** A list of all the known safe hosts to trust as servers */
  public static final String SERVER_TOKEN_SAFE_HOSTS_ADDR = "sakai.auth.trusted.server.safe-hostsaddress";
                                                                                                                                                                                                                                                                                                                  
  private static final String DEFAULT_WRAPPERS = "org.sakaiproject.nakamura.formauth.FormAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.saml.SamlAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.rest.RestAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.cas.CasAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.http.usercontent.UserContentAuthenticationTokenServiceWrapper";
  public static final String SERVER_TOKEN_SAFE_WRAPPERS = "sakai.auth.trusted.wrapper.class.names";

  public static final String TRUSTED_HEADER_NAME = "sakai.auth.trusted.header";

  public static final String TRUSTED_PARAMETER_NAME = "sakai.auth.trusted.request-parameter";

  /** A list of all the known safe hosts to trust for authentication purposes, ie front end proxies */
  public static final String TRUSTED_PROXY_SERVER_ADDR = "sakai.auth.trusted.server.safe-authentication-addresses";

  public static final String DEBUG_COOKIES = "sakai.auth.trusted.token.debugcookies";

  /**
   * the name of the header to be trusted, if null or "" then don't trust headers.
   */
  private String trustedHeaderName;

  /**
   * the name of the parameter to be trusted, if null or "" then don't trust request parameters.
   */
  private String trustedParameterName;

  /**
   * set of trusted IP address to use as proxies.
   */
  private Set<String> trustedProxyServerAddrSet = new HashSet<String>(5);


  /**
   * If True, sessions will be used, if false cookies.
   */
  private boolean usingSession = false;

  /**
   * Should the cookies go over ssl.
   */
  private boolean secureCookie = false;

  /**
   * The name of the authN token.
   */
  private String trustedAuthCookieName;


  /**
   * An optional cookie server can be used in a cluster to centralize the management of
   * authN tokens. This is for situations where session storage and replication is not
   * desired, and session affinity can't be tolerated. Without this clients must come back
   * to the same host where they were authenticated as the cookie encode decode has
   * entropy associated with the instance of the server they are operating on.
   */
  private ClusterCookieServer clusterCookieServer;

  private TokenStore tokenStore;

  private Long ttl;


  protected ClusterTrackingService clusterTrackingService;

  protected CacheManagerService cacheManager;

  protected EventAdmin eventAdmin;

  /**
   * If this is true the implementation is in test mode to enable external components to
   * test, without compromising the protection of the class.
   */
  private boolean testing = false;

  /**
   * Contains the calls made during testing.
   */
  private ArrayList<Object[]> calls;

  private String sharedSecret;

  private boolean trustedTokenEnabled;

  private Set<String> safeHostAddrSet = new HashSet<String>(16); // 16 way cluster is about as big as we will get.

  private String[] safeWrappers;

  private boolean debugCookies;

  private Map<String, TokenTrustValidator> registeredTypes = new ConcurrentHashMap<String, TokenTrustValidator>();

private Component component;

private Dictionary<Object, Object> properties;

  /**
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws UnsupportedEncodingException
   * @throws IllegalStateException
   *
   */
  public TrustedTokenServiceImpl() throws NoSuchAlgorithmException, InvalidKeyException,
      IllegalStateException, UnsupportedEncodingException {
      tokenStore = new TokenStore();
    
  }
  
  protected void init(Component component) {
	  this.component = component;
	  this.properties = this.component.getServiceProperties();
  }


  @SuppressWarnings("rawtypes")
  protected void activate() {
    usingSession = PropertiesUtil.toBoolean(this.properties.get(USE_SESSION), false);
    secureCookie = PropertiesUtil.toBoolean(this.properties.get(SECURE_COOKIE), false);
    ttl = PropertiesUtil.toLong(this.properties.get(TTL), 1200000);
    trustedAuthCookieName = PropertiesUtil.toString(this.properties.get(COOKIE_NAME), "");
    sharedSecret = PropertiesUtil.toString(this.properties.get(SERVER_TOKEN_SHARED_SECRET), "default-setting-change-before-use");
    trustedTokenEnabled = PropertiesUtil.toBoolean(this.properties.get(SERVER_TOKEN_ENABLED), true);
    debugCookies = PropertiesUtil.toBoolean(this.properties.get(DEBUG_COOKIES), false);
    tokenStore.setDebugCookies(debugCookies);
    String safeHostsAddr = PropertiesUtil.toString(this.properties.get(SERVER_TOKEN_SAFE_HOSTS_ADDR), "");
    safeHostAddrSet.clear();
    if ( safeHostsAddr != null) {
      for ( String address : StringUtils.split(safeHostsAddr,';')) {
        safeHostAddrSet.add(address);
      }
    }
    String trustedProxyServerAddr = PropertiesUtil.toString(this.properties.get(TRUSTED_PROXY_SERVER_ADDR), "");
    trustedProxyServerAddrSet.clear();
    if ( trustedProxyServerAddr != null) {
      for ( String address : StringUtils.split(trustedProxyServerAddr,';')) {
        trustedProxyServerAddrSet.add(address);
      }
    }
    String wrappers = PropertiesUtil.toString(this.properties.get(SERVER_TOKEN_SAFE_WRAPPERS), "");
    if ( wrappers == null || wrappers.length() == 0 ) {
      wrappers = DEFAULT_WRAPPERS;
    }
    safeWrappers = StringUtils.split(wrappers, ";");

    String tokenFile = PropertiesUtil.toString(this.properties.get(TOKEN_FILE_NAME), "");
    String serverId = clusterTrackingService.getCurrentServerId();
    tokenStore.doInit(cacheManager, tokenFile, serverId, ttl);

    trustedHeaderName = PropertiesUtil.toString(this.properties.get(TRUSTED_HEADER_NAME), "");
    trustedParameterName = PropertiesUtil.toString(this.properties.get(TRUSTED_PARAMETER_NAME), "");
  }

  public void activateForTesting() {
    testing = true;
    calls = new ArrayList<Object[]>();
    safeWrappers = StringUtils.split(DEFAULT_WRAPPERS,";");
  }

  /**
   * @return the calls used in testing.
   */
  public ArrayList<Object[]> getCalls() {
    return calls;
  }

  /**
   * Extract credentials from the request.
   *
   * @param req
   * @return credentials associated with the request.
   */
  public Credentials getCredentials(HttpServletRequest req, HttpServletResponse response) {
    if (testing) {
      calls.add(new Object[] { "getCredentials", req, response });
      return new SimpleCredentials("testing", "testing".toCharArray());
    }
    Credentials cred = null;
    String userId = null;
    String sakaiTrustedHeader = req.getHeader("x-sakai-token");
    if (trustedTokenEnabled && sakaiTrustedHeader != null
        && sakaiTrustedHeader.trim().length() > 0) {
      String host = req.getRemoteAddr();
      if (!safeHostAddrSet.contains(host)) {
        LOG.warn("Ignoring Trusted Token request from {} ", host);
      } else {
        // we have a HMAC based token, we should see if it is valid against the key we
        // have
        // and if so create some credentials.
        String[] parts = sakaiTrustedHeader.split(";");
        if (parts.length == 3) {
          try {
            String hash = parts[0];
            String user = parts[1];
            String timestamp = parts[2];
            String hmac = Signature.calculateRFC2104HMAC(
                user + ";" + timestamp, sharedSecret);
            if (hmac.equals(hash)) {
              // the user is Ok, we will trust it.
              userId = user;
              cred = createCredentials(userId, TrustedTokenTypes.TRUSTED_TOKEN);
            } else {
              LOG.debug("HMAC Match Failed {} != {} ", hmac, hash );
            }
          } catch (SignatureException e) {
            LOG.warn("Failed to validate server token : {} {} ", sakaiTrustedHeader, e
                .getMessage());
          }
        } else {
          LOG.warn("Illegal number of elements in trusted server token:{} {}  ",
              sakaiTrustedHeader, parts.length);
        }
      }
    }
    if (userId == null) {
      if (usingSession) {
        HttpSession session = req.getSession(false);
        if (session != null) {
          Credentials testCredentials = (Credentials) session
              .getAttribute(SA_AUTHENTICATION_CREDENTIALS);
          if (testCredentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) testCredentials;
            Object o = sc.getAttribute(CA_AUTHENTICATION_USER);
            if (o instanceof TrustedUser) {
              TrustedUser tu = (TrustedUser) o;
              if (tu.getUser() != null) {
                userId = tu.getUser();
                cred = testCredentials;
              }
            }
          }
        } else {
          cred = null;
        }
      } else {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
          for (Cookie c : cookies) {
            if (trustedAuthCookieName.equals(c.getName())) {
              if (secureCookie && !c.getSecure()) {
                continue;
              }
              String cookieValue = c.getValue();
              String[] decodedToken = decodeCookie(c.getValue());
              if (decodedToken != null) {
                userId = decodedToken[0];
                String tokenType = decodedToken[1];
                TokenTrustValidator ttv = registeredTypes.get(tokenType);
                if ( ttv == null || ttv.isTrusted(req) ) {
                  LOG.debug("Token is valid and decoded to {} ",userId);
                  cred = createCredentials(userId, tokenType);
                  refreshToken(response, c.getValue(), userId, tokenType);
                  break;
                } else {
                  LOG.debug("Cookie cant be trusted for this request {} ", cookieValue);
                }
              } else {
                LOG.debug("Invalid Cookie {} ", cookieValue);
                clearCookie(response);
              }
            }
          }
        }
      }
    }
    if (userId != null) {
      LOG.debug("Trusted Authentication for {} with credentials {}  ", userId, cred);
    }

    return cred;
  }

  /**
   * Remove credentials so that subsequent request don't contain credentials.
   *
   * @param request
   * @param response
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
    if ( testing ) {
      calls.add(new Object[]{"dropCredentials",request,response});
      return;
    }
    if (usingSession) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, null);
      }
    } else {
      clearCookie(response);
    }
  }

  /**
   * Inject a token into the request/response, this assumes htat the getUserPrincipal() of the request
   * or the request.getRemoteUser() contain valid user ID's from which to generate the request.
   *
   *
   * @param req
   * @param resp
   * @param readOnlyToken if true, the session or cookie will only allow read only operations in the server.
   */
  public String injectToken(HttpServletRequest request, HttpServletResponse response, String tokenType, UserValidator userValidator) {
    if ( testing ) {
      calls.add(new Object[]{"injectToken",request,response});
      return "testing";
    }
    String userId = null;
    String remoteAddress = request.getRemoteAddr();
    if (trustedProxyServerAddrSet.contains(remoteAddress)) {
      if (trustedHeaderName.length() > 0) {
        userId = StringUtils.trimToNull(request.getHeader(trustedHeaderName));
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: Header [{}] indicated user was [{}] ",
              trustedHeaderName, userId);
        }
      }
      if (userId == null && trustedParameterName.length() > 0) {
        userId = StringUtils.trimToNull(request.getParameter(trustedParameterName));
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: Parameter [{}] indicated user was [{}] ",
              trustedParameterName, userId);
        }
      }
    }
    if (userId == null) {
      Principal p = request.getUserPrincipal();
      if (p != null) {
        userId = StringUtils.trimToNull(p.getName());
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: User Principal indicated user was [{}] ",
              userId);
        }
      }
    }
    if (userId == null) {
      userId = StringUtils.trimToNull(request.getRemoteUser());
      if ( userId != null ) {
        LOG.debug("Injecting Trusted Token from request: Remote User indicated user was [{}] ", userId);
      }
    }

    if ( userValidator != null) {
      userId = userValidator.validate(userId);
    }
    if (userId != null) {
      if (usingSession) {
        HttpSession session = request.getSession(true);
        if (session != null) {
          LOG.debug("Injecting Credentials into Session for " + userId);
          session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, createCredentials(userId, tokenType));
        }
      } else {
        addCookie(response, userId, tokenType);
      }
      Dictionary<String, Object> eventDictionary = new Hashtable<String, Object>();
      eventDictionary.put(TrustedTokenService.EVENT_USER_ID, userId);

      // send an async event to indicate that the user has been trusted, things that want to create users can hook into this.
      eventAdmin.sendEvent(new Event(TrustedTokenService.TRUST_USER_TOPIC,eventDictionary));
      return userId;
    } else {
      LOG.warn("Unable to inject token; unable to determine user from request.");
    }
    return null;
  }

  /**
   * @param userId
   * @param response
   */
  void addCookie(HttpServletResponse response, String userId, String tokenType) {
    Cookie c = new HttpOnlyCookie(trustedAuthCookieName, encodeCookie(userId, tokenType));
    c.setMaxAge(-1);
    c.setPath("/");
    c.setSecure(secureCookie);
    response.addCookie(c);
    // rfc 2109 section 4.5. stop http 1.1 caches caching the response
    response.addHeader("Cache-Control", "no-cache=\"set-cookie\" ");
    // and stop http 1.0 caches caching the response
    response.addDateHeader("Expires", 0);
  }

  /**
   * @param response
   */
  void clearCookie(HttpServletResponse response) {
    Cookie c = new HttpOnlyCookie(trustedAuthCookieName, "");
    c.setMaxAge(0);
    c.setPath("/");
    c.setSecure(secureCookie);
    response.addCookie(c);
  }

  /**
   * Refresh the token, assumes that the cookie is valid.
   *
   * @param req
   * @param value
   * @param userId
   * @param tokenType 
   */
  void refreshToken(HttpServletResponse response, String value, String userId, String tokenType) {
    String[] parts = StringUtils.split(value, "@");
    if (parts != null && parts.length == 5) {
      long cookieTime = Long.parseLong(parts[1].substring(1));
      if (System.currentTimeMillis() + (ttl / 2) > cookieTime) {
        if ( debugCookies) {
          LOG.info("Refreshing Token for {} cookieTime {} ttl {} CurrentTime {} ",new Object[]{userId, cookieTime, ttl, System.currentTimeMillis()});
        }
        addCookie(response, userId, tokenType);
      }
    }

  }

  /**
   * Encode the user ID in a secure cookie.
   *
   * @param userId
   * @return
   */
  String encodeCookie(String userId, String tokenType) {
    if (userId == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.encodeCookie(userId);
    } else {
      long expires = System.currentTimeMillis() + ttl;
      SecureCookie secretKeyHolder = tokenStore.getActiveToken();

      try {
        return secretKeyHolder.encode(expires, userId, tokenType);
      } catch (NoSuchAlgorithmException e) {
        LOG.error(e.getMessage(), e);
      } catch (InvalidKeyException e) {
        LOG.error(e.getMessage(), e);
      } catch (IllegalStateException e) {
        LOG.error(e.getMessage(), e);
      } catch (UnsupportedEncodingException e) {
        LOG.error(e.getMessage(), e);
      } catch (SecureCookieException e) {
        if ( e.isError() ) {
          LOG.error(e.getMessage(), e);
        } else {
          LOG.info(e.getMessage(), e);
        }
      }
      return null;
    }
  }

  /**
   * Decode the user ID.
   *
   * @param value
   * @return
   */
  String[] decodeCookie(String value) {
    if (value == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.decodeCookie(value);
    } else {
      try {
        SecureCookie secureCookie = tokenStore.getSecureCookie();
        return secureCookie.decode(value);
      } catch (SecureCookieException e) {
        if ( e.isError() ) {
          LOG.error(e.getMessage());
        } else {
          LOG.info(e.getMessage());
        }
      }
    }
    return null;

  }


  /**
   * Create credentials from a validated userId.
   *
   * @param req
   *          The request to sniff for a user.
   * @return
   */
  private Credentials createCredentials(String userId, String tokenType) {
    SimpleCredentials sc = new SimpleCredentials(userId, new char[0]);
    TrustedUser user = new TrustedUser(userId);
    sc.setAttribute(CA_AUTHENTICATION_USER, user);
    sc.setAttribute(CA_AUTHENTICATION_ATTRIBUTES, tokenType);
    return sc;
  }

  /**
   * "Trusted" inner class for passing the user on to the authentication handler.<br/>
   * <br/>
   * By being a static, inner class with a private constructor, it is harder for an
   * external source to inject into the authentication chain.
   */
  static final class TrustedUser {
    private final String user;

    /**
     * Constructor.
     *
     * @param user
     *          The user to represent.
     */
    private TrustedUser(String user) {
      this.user = user;
    }

    /**
     * Get the user that is being represented.
     *
     * @return The represented user.
     */
    String getUser() {
      return user;
    }
  }

  /**
   * @return
   */
  public String[] getAuthorizedWrappers() {
    return safeWrappers;
  }


  public void registerType(String type,
      TokenTrustValidator tokenTrustValidator) {
    registeredTypes.put(type, tokenTrustValidator);
  }


  public void deregisterType(String type,
      TokenTrustValidator tokenTrustValidator) {
    TokenTrustValidator ttv = registeredTypes.get(type);
    if ( tokenTrustValidator.equals(ttv) ) {
      registeredTypes.remove(type);
    }
  }

}
