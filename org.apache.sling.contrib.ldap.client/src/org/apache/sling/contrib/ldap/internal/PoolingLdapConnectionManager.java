package org.apache.sling.contrib.ldap.internal;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.contrib.ldap.api.DomainNotFoundException;
import org.apache.sling.contrib.ldap.api.LdapConnectionManager;
import org.ldaptive.Connection;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.DnParser;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ReturnAttributes;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SearchScope;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SoftLimitConnectionPool;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Iterator;

/**
 */
public class PoolingLdapConnectionManager implements LdapConnectionManager, ManagedService {

	/** Class-specific logger */
	private static Logger log = LoggerFactory.getLogger(PoolingLdapConnectionManager.class);

	public final static String PID = PoolingLdapConnectionManager.class.getName();

	/** Whether to create secure connections. */
	public final static String LDAP_CLIENT_PROP_SECURECONNECTION = "secureConnection";
	public final static boolean DEFAULT_LDAP_CLIENT_PROP_SECURECONNECTION = false;

	/** Where to connect using TLS. */
	public final static String LDAP_CLIENT_PROP_TLS = "tls";
	public final static boolean DEFAULT_LDAP_CLIENT_PROP_TLS = false;

	/** The password to the keystore. */
	public final static String LDAP_CLIENT_PROP_KEYSTORELOCATION = "keystoreLocation";
	public final static String DEFAULT_LDAP_CLIENT_PROP_KEYSTORELOCATION = "";

	/** The password to the keystorePassword. */
	public final static String LDAP_CLIENT_PROP_KEYSTOREPASSWORD = "keystorePassword";
	public final static String DEFAULT_LDAP_CLIENT_PROP_KEYSTOREPASSWORD = "";

	/** The host to which to connect. */
	public final static String LDAP_CLIENT_PROP_HOST = "host";
	public final static String DEFAULT_LDAP_CLIENT_PROP_HOST = "";

	/** The port to which to connect. */
	public final static String LDAP_CLIENT_PROP_PORT = "port";
	public final static int DEFAULT_LDAP_CLIENT_PROP_PORT = 1389;

	/** The user/account to use for connections. */
	public final static String LDAP_CLIENT_PROP_LOGINUSER = "loginuser";
	public final static String DEFAULT_LDAP_CLIENT_PROP_LOGINUSER = "admin";

	/** The password to use for connections. */
	public final static String LDAP_CLIENT_PROP_LOGINPASSWORD = "loginpassword";
	public final static String DEFAULT_LDAP_CLIENT_PROP_LOGINPASSWORD = "admin";

	/** The password to use for connections. */
	public final static String LDAP_CLIENT_PROP_POOLMAX = "poolmaxconns";
	public final static int DEFAULT_LDAP_CLIENT_PROP_POOLMAX = 10;

	private PooledConnectionFactory connFactory;

	private Dictionary<String, ?> properties;

	private SearchExecutor searchExecutor;

	protected final String searchFilter = "(&(objectClass=person)(mail={user}))";

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties == null)
			throw new ConfigurationException("NULL conf props", "NULL conf props");

		this.properties = properties;

		init();
	}

	/**
	 * {@inheritDoc}
	 */
	public void init() {
		log.debug("init()");

		boolean isSecure = PropertiesUtil.toBoolean(this.properties.get(LDAP_CLIENT_PROP_SECURECONNECTION),
				DEFAULT_LDAP_CLIENT_PROP_SECURECONNECTION);
		String connectionString = isSecure ? "ldaps://" : "ldap://";
		connectionString += PropertiesUtil.toString(this.properties.get(LDAP_CLIENT_PROP_HOST),
				DEFAULT_LDAP_CLIENT_PROP_HOST);
		connectionString += ":"
				+ PropertiesUtil.toInteger(this.properties.get(LDAP_CLIENT_PROP_PORT), DEFAULT_LDAP_CLIENT_PROP_PORT);

		SoftLimitConnectionPool pool = new SoftLimitConnectionPool(new DefaultConnectionFactory(connectionString));
		pool.initialize();

		this.connFactory = new PooledConnectionFactory(pool);
		this.searchExecutor = new SearchExecutor();
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws LdapException {
		log.debug("getConnection()");

		Connection conn = newConnection();

		return conn;
	}

	protected Connection newConnection() throws LdapException {

		return connFactory.getConnection();
	}

	/**
	 * {@inheritDoc}
	 */
	public void returnConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			log.error("returnConnection(): failed on disconnect: ", e);
		}
	}

	/**
	 * Creates a new search operation configured with the properties on this
	 * factory.
	 *
	 * @param conn
	 *            to pass to the search operation
	 *
	 * @return search operation
	 */
	protected SearchOperation createSearchOperation(final Connection conn) {
		final SearchOperation op = new SearchOperation(conn);
		return op;
	}

	@Override
	public String lookupUserDomain(String user) throws LdapException {
		log.debug("mp lookupUserDomain user={}", user);

		String dn = null;
		if (user != null && !"".equals(user)) {
			// create the search filter
			final SearchFilter filter = createSearchFilter(user);

			if (filter.getFilter() != null) {
				final SearchResult result = performLdapSearch(filter, user, true, "");
				final Iterator<LdapEntry> answer = result.getEntries().iterator();

				// return first match, otherwise user doesn't exist
				if (answer != null && answer.hasNext()) {
					dn = resolveDn(answer.next());
					if (answer.hasNext()) {
						log.debug("lookupUserDomain multiple results found for user={} using filter={}", user, filter);
						throw new LdapException("lookupUserDomain Found more than (1) DN for: " + user);
					}
				} else {
					log.info("lookupUserDomain search for user={} failed using filter={}", user, filter);

					return null;
				}
			} else {
				log.error("lookupUserDomain DN search filter not found, no search performed");
				return null;
			}
		} else {
			log.warn("lookupUserDomain DN resolution cannot occur, user input was empty or null");
			return null;
		}
		log.debug("lookupUserDomain found user={} with dn={}", user, dn);

		String baseDn = DnParser.substring(dn, 2);
		log.debug("lookupUserDomain dn={} resolved to domain={}", dn, baseDn);

		return baseDn;
	}

	/**
	 * Returns the DN for the supplied ldap entry.
	 *
	 * @param entry
	 *            to retrieve the DN from
	 *
	 * @return dn
	 */
	@Override
	public String resolveDn(final LdapEntry entry) {
		return entry.getDn();
	}

	/**
	 * Returns a search filter using {@link #userFilter} and
	 * {@link #userFilterParameters}. The user parameter is injected as a named
	 * parameter of 'user'.
	 *
	 * @param user
	 *            identifier
	 *
	 * @return search filter
	 */
	@Override
	public SearchFilter createSearchFilter(final String user) {
		final SearchFilter filter = new SearchFilter();
		if (searchFilter != null) {
			log.debug("searching for DN using userFilter");
			filter.setFilter(searchFilter);
			// assign user as a named parameter
			filter.setParameter("user", user);
		} else {
			log.error("Invalid userFilter, cannot be null or empty.");
		}
		return filter;
	}

	/**
	 * Executes the ldap search operation with the supplied filter.
	 *
	 * @param filter
	 *            to execute
	 *
	 * @return ldap search result
	 *
	 * @throws LdapException
	 *             if an error occurs
	 */
	@Override
	public SearchResult performLdapSearch(final SearchFilter filter, final String user, final boolean rootSearch,
			final String baseDn) throws LdapException {
		SearchRequest request;
		try {
			request = createSearchRequest(filter, user, rootSearch, false /* subTreeSearch */, baseDn);
		} catch (DomainNotFoundException e) {
			log.warn("performLdapSearch domain not found for user={} in forest", user);
			return null;
		}

		Connection conn = null;
		try {
			conn = getConnection();

			final SearchOperation op = createSearchOperation(conn);
			return op.execute(request).getResult();
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	/**
	 * Returns a search request for searching for a single entry in an LDAP,
	 * returning no attributes.
	 *
	 * @param filter
	 *            to execute
	 *
	 * @return search request
	 * @throws DomainNotFoundException
	 */
	@Override
	public SearchRequest createSearchRequest(final SearchFilter filter, final String user, final boolean rootSearch,
			final boolean subTreeSearch, final String baseDn) throws DomainNotFoundException {
		final SearchRequest request = new SearchRequest();

		if (!rootSearch) {
			if (baseDn == null)
				throw new DomainNotFoundException();

			request.setBaseDn(baseDn);
			if (subTreeSearch) {
				request.setSearchScope(SearchScope.SUBTREE);
			} else {
				request.setSearchScope(SearchScope.ONELEVEL);
			}
		} else {
			request.setBaseDn("");
			request.setSearchScope(SearchScope.SUBTREE);
		}
		request.setSearchFilter(filter);
		request.setReturnAttributes(ReturnAttributes.NONE.value());
		request.setDerefAliases(null);
		return request;
	}
}
