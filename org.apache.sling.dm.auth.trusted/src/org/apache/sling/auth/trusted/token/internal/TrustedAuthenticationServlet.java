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

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.apache.felix.dm.Component;
import org.apache.sling.auth.trusted.token.api.TrustedTokenService;
import org.apache.sling.auth.trusted.token.api.TrustedTokenTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Dictionary;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet for storing authentication credentials from requests using an external trusted
 * mechanism such as CAS.
 * </p>
 * <p>
 * This servlet does not perform the authentication itself but looks for information in
 * the request from the authentication authority. This information is then stored in the
 * session for use by the authentication handler on subsequent calls.
 * </p>
 * <p>
 * This servlet is mounted outside sling. In essence we Trust the external authentication and 
 * simply store the trusted user in a a trusted token in the form of a cookie.
 * </p>
 */
public final class TrustedAuthenticationServlet extends HttpServlet implements HttpContext {
  /**
   * 
   */
  private static final long serialVersionUID = 4265672306115024805L;

  
  private static final String PARAM_DESTINATION = "url";

  static final String DESCRIPTION_PROPERTY = "service.description";

  static final String VENDOR_PROPERTY = "service.vendor";

  static final String REGISTRATION_PATH = "sakai.auth.trusted.path.registration";

  /**
   * Property for the default destination to go to if no destination is specified.
   */
  static final String DEFAULT_DESTINATION = "sakai.auth.trusted.destination.default";

  private static final String DEFAULT_NO_USER_REDIRECT_FORMAT = "/system/trustedauth-nouser?u={0}";

  private static final String NO_USER_REDIRECT_LOCATION_FORMAT = "sakai.auth.trusted.nouserlocationformat";

  private static final Logger LOGGER = LoggerFactory.getLogger(TrustedAuthenticationServlet.class);





  protected volatile HttpService httpService;

  protected volatile TrustedTokenService trustedTokenService;
  
  protected volatile SlingRepository repository;

  /** The registration path for this servlet. */
  private String registrationPath;

  /** The default destination to go to if none is specified. */
  private String defaultDestination;


  private String noUserRedirectLocationFormat;


  private Component component;


  private Dictionary<Object, Object> properties;


  protected void init(Component component) {
	  this.component = component;
	  this.properties = this.component.getServiceProperties();
  }
  
  @SuppressWarnings("rawtypes")
  protected void activate() {
    noUserRedirectLocationFormat = PropertiesUtil.toString(properties.get(NO_USER_REDIRECT_LOCATION_FORMAT), DEFAULT_NO_USER_REDIRECT_FORMAT);
    registrationPath = PropertiesUtil.toString(properties.get(REGISTRATION_PATH), "/system/trustedauth");
    defaultDestination = PropertiesUtil.toString(properties.get(DEFAULT_DESTINATION), "/dev");
    try {
      httpService.registerServlet(registrationPath, this, null, null);
      LOGGER.info("Registered {} at {} ",this,registrationPath);
    } catch (ServletException e) {
      LOGGER.error(e.getMessage(),e);
    } catch (NamespaceException e) {
      LOGGER.error(e.getMessage(),e);
    }
  }

  protected void deactivate() {
    httpService.unregister(registrationPath);
    LOGGER.info("Unregistered {} from {} ",this,registrationPath);
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="BC_VACUOUS_INSTANCEOF",justification="Could be injected from annother bundle")
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    
    if (trustedTokenService instanceof TrustedTokenServiceImpl) {
      final AuthenticatedAction authAction = new AuthenticatedAction();
      ((TrustedTokenServiceImpl) trustedTokenService).injectToken(req, resp, TrustedTokenTypes.AUTHENTICATED_TRUST, new UserValidator(){

        public String validate(String userId) {
          if ( userId != null ) {        
            // we found a user, check if it really exists.
            Session session = null;
/*            try {
              session = repository.loginAdministrative(null);
              AuthorizableManager am = session.getAuthorizableManager();
              Authorizable a = am.findAuthorizable(userId);
              if ( a == null ) {
                LOGGER.info("Authenticated User {} does not exist", userId);
                authAction.setAction(AuthenticatedAction.REDIRECT);
                return null;
              }
            } catch (Exception e) {
              LOGGER.warn("Failed to check user ",e);
            } finally {
              if ( session != null ) {
                try {
                  session.logout();
                } catch (ClientPoolException e) {
                  LOGGER.warn("Failed to close admin session ",e);
                }
              }
            }*/
          }
          return userId;
        }
      });
      String destination = req.getParameter(PARAM_DESTINATION);
      if (destination == null) {
        destination = defaultDestination;
      }
      if ( authAction.isRedirect() ) {
        String redirectLocation = MessageFormat.format(noUserRedirectLocationFormat, URLEncoder.encode(destination, "UTF-8"));
        resp.sendRedirect(redirectLocation);
      } else {
        if (destination == null) {
          destination = defaultDestination;
        }
        // ensure that the redirect is safe and not susceptible to
        resp.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
      }
    } else {
      LOGGER.debug("Trusted Token Service is not the correct implementation and so cant inject tokens. ");
    }
  }

  public String getMimeType(String mimetype) {
    return null;
  }

  public URL getResource(String name) {
    return getClass().getResource(name);
  }

  /**
   * (non-Javadoc) This servlet handles its own security since it is going to trust the
   * external remote user. If we don't do this the Sling handleSecurity takes over and causes problems.
   * 
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    return true;
  }

 
}
