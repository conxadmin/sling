package org.apache.sling.auth.sso.cas.impl;

import com.ctc.wstx.stax.WstxInputFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.sso.cas.api.ILDAPLoginUserManager;
import org.apache.sling.auth.sso.cas.api.SsoPrincipal;
import org.apache.sling.auth.trusted.token.api.memory.Cache;
import org.apache.sling.auth.trusted.token.api.memory.CacheManagerService;
import org.apache.sling.auth.trusted.token.api.memory.CacheScope;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;

/**
 * This class integrates SSO with the Sling authentication framework. The
 * integration is needed only due to limitations on servlet filter support in
 * the OSGi / Sling environment.
 */
public class CasAuthenticationHandler extends DefaultAuthenticationFeedbackHandler
		implements AuthenticationHandler, AuthenticationFeedbackHandler {

	private static final String TARGET_SERVICE = "targetService";

	public static final String AUTH_TYPE = "CAS";

	private static final Logger LOGGER = LoggerFactory.getLogger(CasAuthenticationHandler.class);

	static final String DEFAULT_ARTIFACT_NAME = "ticket";
	static final String DEFAULT_LOGIN_URL = "https://cas.myconxclouddev.com/cas/login";
	static final String DEFAULT_LOGOUT_URL = "https://cas.myconxclouddev.com/cas/logout";
	static final String DEFAULT_SERVER_URL = "https://cas.myconxclouddev.com/cas";
	static final boolean DEFAULT_RENEW = false;
	static final boolean DEFAULT_GATEWAY = false;
	static final boolean DEFAULT_PROXY = false;

	private static final int DEFAULT_MAX_COOKIE_SIZE = 4096;

	public static final String FIRST_NAME_PROP_DEFAULT = "firstName";

	public static final String LAST_NAME_PROP_DEFAULT = "lastName";

	public static final String EMAIL_PROP_DEFAULT = "email";

	/**
	 * Represents the constant for where the assertion will be located in
	 * memory.
	 */
	static final String AUTHN_INFO = "org.sakaiproject.nakamura.auth.cas.SsoAuthnInfo";

	static final String LOGIN_URL = "sakai.auth.cas.url.login";
	private String loginUrl;

	static final String LOGOUT_URL = "sakai.auth.cas.url.logout";
	private String logoutUrl;

	static final String SERVER_URL = "sakai.auth.cas.url.server";
	private String serverUrl;

	static final String RENEW = "sakai.auth.cas.prop.renew";
	private boolean renew;

	static final String GATEWAY = "sakai.auth.cas.prop.gateway";
	private boolean gateway;

	static final String PROXY = "sakai.auth.cas.prop.proxy";
	private boolean proxy;
	/**
	 * Define the set of authentication-related query parameters which should be
	 * removed from the "service" URL sent to the SSO server.
	 */
	Set<String> filteredQueryStrings = new HashSet<String>(
			Arrays.asList(REQUEST_LOGIN_PARAMETER, DEFAULT_ARTIFACT_NAME));

	private org.apache.http.client.HttpClient client = null;

	private Component component;

	private Dictionary<Object, Object> properties;

	private Cache<String> pgtIOUs;
	private Cache<String> pgts;

	// -- DM injected
	private volatile SlingRepository repository;

	private volatile ILDAPLoginUserManager ldapLoginUserManager;

	public CasAuthenticationHandler() {
	}

	/*
	 * CasAuthenticationHandler(SlingRepository repository) { this.repository =
	 * repository; }
	 */

	// ----------- OSGi integration ----------------------------
	protected void init(Component component) {
		this.component = component;
		this.properties = this.component.getServiceProperties();
		this.client = createHttpClient_AcceptsUntrustedCerts();
	}

	protected void activate() {
		modified();
	}

	protected void modified() {
		loginUrl = PropertiesUtil.toString(this.properties.get(LOGIN_URL), DEFAULT_LOGIN_URL);
		logoutUrl = PropertiesUtil.toString(this.properties.get(LOGOUT_URL), DEFAULT_LOGOUT_URL);
		serverUrl = PropertiesUtil.toString(this.properties.get(SERVER_URL), DEFAULT_SERVER_URL);

		renew = PropertiesUtil.toBoolean(this.properties.get(RENEW), DEFAULT_RENEW);
		gateway = PropertiesUtil.toBoolean(this.properties.get(GATEWAY), DEFAULT_GATEWAY);
		proxy = PropertiesUtil.toBoolean(this.properties.get(PROXY), DEFAULT_PROXY);

		/*
		 * pgtIOUs =
		 * cacheManagerService.getCache(CasAuthenticationHandler.class.getName()
		 * + "-iou-cache", CacheScope.CLUSTERREPLICATED); pgts =
		 * cacheManagerService.getCache(CasAuthenticationHandler.class.getName()
		 * + "-pgt-cache", CacheScope.CLUSTERREPLICATED);
		 */
	}

	// ----------- AuthenticationHandler interface ----------------------------

	@Override
	public void dropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String target = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
		if (StringUtils.isBlank(target)) {
			target = request.getParameter(Authenticator.LOGIN_RESOURCE);
		}

		if (target != null && target.length() > 0 && !("/".equals(target))) {
			LOGGER.info("SSO logout about to override requested redirect to {} and instead redirect to {}", target,
					logoutUrl);
		} else {
			LOGGER.debug("SSO logout will request redirect to {}", logoutUrl);
		}
		request.setAttribute(Authenticator.LOGIN_RESOURCE, logoutUrl);
	}

	@Override
	public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
		LOGGER.debug("extractCredentials called");

		AuthenticationInfo authnInfo = null;

		String artifact = extractArtifact(request);

		if (artifact != null) {
			try {
				// Check cookies
				final Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (final Cookie cookie : cookies) {
						final String cookieName = cookie.getName();
						String val = readCookieValue(cookie);
						LOGGER.info("cookie found: {} value {}", cookieName, val);
						/*
						 * if (cookieName.equals(xingCookie)) { hash =
						 * readCookieValue(cookie); logger.debug(
						 * "“Login with XING” cookie found: {}", hash); } else
						 * if (cookieName.equals(userCookie)) { user =
						 * readCookieValue(cookie); } else if
						 * (cookieName.equals(userIdCookie)) { userId =
						 * readCookieValue(cookie); }
						 */
					}
				}
				// make REST call to validate artifact
				String service = constructServiceParameter(request);
				String validateUrl = serverUrl + "/serviceValidate?service=" + service + "&ticket=" + artifact;
				HttpGet get = new HttpGet(validateUrl);
				HttpResponse rsp = this.client.execute(get);
				int returnCode = rsp.getStatusLine().getStatusCode();

				if (returnCode >= 200 && returnCode < 300) {
					// successful call; test for valid response
					String body = EntityUtils.toString(rsp.getEntity());
					String credentials = retrieveCredentials(body);
					if (credentials != null) {
						String password = RandomStringUtils.random(8);
						// found some credentials; proceed
						authnInfo = createAuthnInfo(credentials, password.toCharArray());

						request.setAttribute(AUTHN_INFO, authnInfo);
					} else {
						LOGGER.warn("Unable to extract credentials from validation server.");
						authnInfo = AuthenticationInfo.FAIL_AUTH;
					}
				} else {
					LOGGER.error("Failed response from validation server: [" + returnCode + "]");
					authnInfo = AuthenticationInfo.FAIL_AUTH;
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		return authnInfo;
	}

	public org.apache.http.client.HttpClient createHttpClient_AcceptsUntrustedCerts() {
		CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		return httpClient;
	}

	/**
	 * Called after extractCredentials has returne non-null but logging into the
	 * repository with the provided AuthenticationInfo failed.<br/>
	 *
	 * {@inheritDoc}
	 *
	 * @see org.apache.sling.auth.core.spi.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
		LOGGER.debug("requestCredentials called");
		
		//response.addHeader("Access-Control-Allow-Origin", "https://cas.myconxclouddev.com");

		final String service = constructServiceParameter(request);
		LOGGER.debug("Service URL = \"{}\"", service);
		final String urlToRedirectTo = constructLoginUrl(request, service);
		LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
		response.sendRedirect(urlToRedirectTo);
		return true;
	}

	private String constructLoginUrl(HttpServletRequest request, String service) {
		ArrayList<String> params = new ArrayList<String>();

		String renewParam = request.getParameter("renew");
		boolean renew = this.renew;
		if (renewParam != null) {
			renew = Boolean.parseBoolean(renewParam);
		}
		if (renew) {
			params.add("renew=true");
		}

		String gatewayParam = request.getParameter("gateway");
		boolean gateway = this.gateway;
		if (gatewayParam != null) {
			gateway = Boolean.parseBoolean(gatewayParam);
		}
		if (gateway) {
			params.add("gateway=true");
		}

		params.add("service=" + service);
		return loginUrl + "?" + StringUtils.join(params, '&');
	}

	// ----------- AuthenticationFeedbackHandler interface
	// ----------------------------

	/**
	 * {@inheritDoc}
	 *
	 * @see org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.apache.sling.auth.core.spi.AuthenticationInfo)
	 */
	@Override
	public void authenticationFailed(HttpServletRequest request, HttpServletResponse response,
			AuthenticationInfo authInfo) {
		// LOGGER.debug("authenticationFailed called");
		// final HttpSession session = request.getSession(false);
		// if (session != null) {
		// final SsoPrincipal principal = (SsoPrincipal) session
		// .getAttribute(CONST_SSO_ASSERTION);
		// if (principal != null) {
		// LOGGER.warn("SSO assertion is set", new Exception());
		// }
		// }
	}

	/**
	 * If a redirect is configured, this method will take care of the redirect.
	 * <p>
	 * If user auto-creation is configured, this method will check for an
	 * existing Authorizable that matches the principal. If not found, it
	 * creates a new Jackrabbit user with all properties blank except for the ID
	 * and a randomly generated password. WARNING: Currently this will not
	 * perform the extra work done by the Nakamura CreateUserServlet, and the
	 * resulting user will not be associated with a valid profile.
	 * <p>
	 * Note: do not try to inject the token here. The request has not had the
	 * authenticated user added to it so request.getUserPrincipal() and
	 * request.getRemoteUser() both return null.
	 * <p>
	 * TODO This really needs to be dropped to allow for user pull, person
	 * directory integrations, etc. See SLING-1563 for the related issue of user
	 * population via OpenID.
	 *
	 * @see org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.apache.sling.auth.core.spi.AuthenticationInfo)
	 */
	/*
	 * @Override public boolean authenticationSucceeded(HttpServletRequest
	 * request, HttpServletResponse response, AuthenticationInfo authInfo) {
	 * LOGGER.debug("authenticationSucceeded called");
	 * 
	 * try { Session session = repository.loginAdministrative(null);
	 * 
	 * UserManager um = AccessControlUtil.getUserManager(session); Authorizable
	 * auth = um.getAuthorizable(authInfo.getUser());
	 * 
	 * if (auth == null) { String password = RandomStringUtils.random(8); User
	 * user = um.createUser(authInfo.getUser(), password);
	 * 
	 * decorateUser(session, user);
	 * 
	 * // Check for the default post-authentication redirect. return
	 * DefaultAuthenticationFeedbackHandler.handleRedirect(request, response); }
	 * } catch (Exception e) { LOGGER.warn(e.getMessage(), e); }
	 * 
	 * return false; }
	 */

	// ----------- Internal ----------------------------
	private AuthenticationInfo createAuthnInfo(final String username, final char[] password) {
		final SimpleCredentials credentials = createCredentials(username, password);
		AuthenticationInfo authnInfo = new AuthenticationInfo(AUTH_TYPE, username);
		authnInfo.put(AUTHENTICATION_INFO_CREDENTIALS, credentials);
		return authnInfo;
	}

	private SimpleCredentials createCredentials(final String username, final char[] password) {
		final SsoPrincipal principal = new SsoPrincipal(username);
		SimpleCredentials credentials = new SimpleCredentials(principal.getName(), password);
		credentials.setAttribute(SsoPrincipal.class.getName(), principal);
		return credentials;
	}

	private AuthenticationInfo createAuthnInfo(final String username, CryptedSimpleCredentials creds) {
		AuthenticationInfo authnInfo = new AuthenticationInfo(AUTH_TYPE, username);
		authnInfo.put(AUTHENTICATION_INFO_CREDENTIALS, creds);
		return authnInfo;
	}

	/**
	 * @param request
	 * @return the URL to which the SSO server should redirect after successful
	 *         authentication. By default, this is the same URL from which
	 *         authentication was initiated (minus authentication-related query
	 *         strings like "ticket"). A request attribute or parameter can be
	 *         used to specify a different return path.
	 */
	protected String constructServiceParameter(HttpServletRequest request) throws UnsupportedEncodingException {
		//Check for target service param
		String queryString = request.getQueryString();
		if (queryString != null && queryString.contains(TARGET_SERVICE)) {
			String[] parameters = StringUtils.split(queryString, '&');
			for (String parameter : parameters) {
				String[] keyAndValue = StringUtils.split(parameter, "=", 2);
				String key = keyAndValue[0];
				if (TARGET_SERVICE.equals(key)) {
					//Base URL
					String scheme = request.getScheme();
					String host = request.getHeader("Host");        // includes server name and server port
					String contextPath = request.getContextPath();  // includes leading forward slash

					String base = scheme + "://" + host + contextPath;
					if (keyAndValue[1].contains(base))
						return keyAndValue[1];
					else
						return base+keyAndValue[1];
				}
			}
		}
		
		StringBuffer url = request.getRequestURL().append("?");
		String tryLogin = CasLoginServlet.TRY_LOGIN + "=2";
		if (queryString == null || queryString.indexOf(tryLogin) == -1) {
			url.append(tryLogin).append("&");
		}

		if (queryString != null) {
			String[] parameters = StringUtils.split(queryString, '&');
			for (String parameter : parameters) {
				String[] keyAndValue = StringUtils.split(parameter, "=", 2);
				String key = keyAndValue[0];
				if (!filteredQueryStrings.contains(key)) {
					url.append(parameter).append("&");
				}
			}
		}

		String finalUrl = url.toString();
		if (finalUrl.endsWith("&"))
			finalUrl = finalUrl.substring(0, finalUrl.length() - 1);

		return URLEncoder.encode(finalUrl, "UTF-8");
	}

	private String extractArtifact(HttpServletRequest request) {
		return request.getParameter(DEFAULT_ARTIFACT_NAME);
	}

	private String retrieveCredentials(String responseBody) {
		String username = null;
		String pgtIou = null;
		String failureCode = null;
		String failureMessage = null;

		try {
			XMLInputFactory xmlInputFactory = new WstxInputFactory();
			xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
			xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(new StringReader(responseBody));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				// process the event if we're starting an element
				if (event.isStartElement()) {
					StartElement startEl = event.asStartElement();
					QName startElName = startEl.getName();
					String startElLocalName = startElName.getLocalPart();

					/*
					 * Example of failure XML <cas:serviceResponse
					 * xmlns:cas='http://www.yale.edu/tp/cas'>
					 * <cas:authenticationFailure code='INVALID_REQUEST'>
					 * &#039;service&#039; and &#039;ticket&#039; parameters are
					 * both required </cas:authenticationFailure>
					 * </cas:serviceResponse>
					 */
					if ("authenticationFailure".equalsIgnoreCase(startElLocalName)) {
						// get code of the failure
						Attribute code = startEl.getAttributeByName(QName.valueOf("code"));
						failureCode = code.getValue();

						// get the message of the failure
						event = eventReader.nextEvent();
						assert event.isCharacters();
						Characters chars = event.asCharacters();
						failureMessage = chars.getData();
						break;
					}

					/*
					 * Example of success XML <cas:serviceResponse
					 * xmlns:cas='http://www.yale.edu/tp/cas'>
					 * <cas:authenticationSuccess> <cas:user>NetID</cas:user>
					 * </cas:authenticationSuccess> </cas:serviceResponse>
					 */
					if ("authenticationSuccess".equalsIgnoreCase(startElLocalName)) {
						// skip to the user tag start
						while (eventReader.hasNext()) {
							event = eventReader.nextTag();
							if (event.isEndElement()) {
								if (eventReader.hasNext()) {
									event = eventReader.nextTag();
								} else {
									break;
								}
							}
							assert event.isStartElement();
							startEl = event.asStartElement();
							startElName = startEl.getName();
							startElLocalName = startElName.getLocalPart();
							if (proxy && "proxyGrantingTicket".equals(startElLocalName)) {
								event = eventReader.nextEvent();
								assert event.isCharacters();
								Characters chars = event.asCharacters();
								pgtIou = chars.getData();
								LOGGER.debug("XML parser found pgt: {}", pgtIou);
							} else if ("user".equals(startElLocalName)) {
								// move on to the body of the user tag
								event = eventReader.nextEvent();
								assert event.isCharacters();
								Characters chars = event.asCharacters();
								username = chars.getData();
								LOGGER.debug("XML parser found user: {}", username);
							} else {
								LOGGER.error("Found unexpected element [{}] while inside 'authenticationSuccess'",
										startElName);
								break;
							}

							if (username != null && (!proxy || pgtIou != null)) {
								break;
							}
						}
					}
				}
			}
		} catch (XMLStreamException e) {
			LOGGER.error(e.getMessage(), e);
		}

		if (failureCode != null || failureMessage != null) {
			LOGGER.error("Error response from server [code=" + failureCode + ", message=" + failureMessage);
		}

		LOGGER.debug("Caching '{}' as the IOU for '{}'", pgtIou, username);
/*		String pgt = pgts.get(pgtIou);
		if (pgt != null) {
			savePgt(username, pgt, pgtIou);
		} else {
			LOGGER.debug("Caching '{}' as the IOU for '{}'", pgtIou, username);
			pgtIOUs.put(pgtIou, username);
		}*/
		return username;
	}

	protected void savePgt(String username, String pgt, String pgtIou) {
		Map<String, Object> pgtId = new HashMap<String, Object>();
		pgtId.put("ticket", pgt);
		try {
			final String password = RandomStringUtils.random(8);
			final SimpleCredentials creds = createCredentials(username, password.toCharArray());
			final User user = ldapLoginUserManager.createUser(creds);
			final String savePath = user.getPath() + "/cas";
			LOGGER.debug("Saving pgt '{}' to '{}'", pgtId, savePath);
			/*
			 * ContentManager contentManager = session.getContentManager(); if
			 * (contentManager.exists(LitePersonalUtils.getHomePath(username)))
			 * { String savePath = LitePersonalUtils.getPrivatePath(username) +
			 * "/cas";
			 * 
			 * Content storedTicket = new Content(savePath, pgtId);
			 * contentManager.update(storedTicket); // remove the pgtIOU from
			 * cache pgts.remove(pgtIou); pgtIOUs.remove(pgtIou); } else {
			 * LOGGER.debug("User {} has no content home, not saving pgt",
			 * username); }
			 */
		} catch (Exception e) {
			LOGGER.error("Couldn't save proxy granting ticket: ", e);
		}
	}

	protected String readCookieValue(final Cookie cookie) {
		if (cookie.getValue() != null) {
			if (cookie.getValue().length() > DEFAULT_MAX_COOKIE_SIZE) {
				LOGGER.warn("size of cookie value greater than configured max. cookie size of {}",
						DEFAULT_MAX_COOKIE_SIZE);
			} else {
				return cookie.getValue();
			}
		}
		return null;
	}

	protected void deleteCookies(final HttpServletRequest request, final HttpServletResponse response) {
		final Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (final Cookie cookie : cookies) {
				final String name = cookie.getName();
				LOGGER.debug("cookie found: '{}'", name);
				/*
				 * if (name.equals(xingCookie) || name.equals(userCookie) ||
				 * name.equals(userIdCookie)) { logger.debug(
				 * "deleting cookie '{}' with value '{}'", cookie.getName(),
				 * cookie.getValue()); cookie.setValue(null);
				 * cookie.setMaxAge(0); response.addCookie(cookie); }
				 */
			}
		}
	}
}
