package org.apache.sling.contrib.ldap.api;

import org.ldaptive.Connection;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;

/**
 * 
 */
public interface LdapConnectionManager {

  /**
   * Retrieve an <code>LDAPConnection</code> -- the connection may already be bound
   * depending on the configuration
   * 
   * @return a connected <code>LDAPConnection</code>
   * @throws LDAPException
   *           if the <code>LDAPConnection</code> allocation fails
   */
  Connection getConnection() throws LdapException;

  /**
   * Retrieve a bound <code>LDAPConnection</code> using the indicated credentials. If null
   * is passed for the dn, the default dn and password will be used mimicking
   * getConnection() with autobind = true.
   * 
   * @param dn
   *          The distinguished name for binding.
   * @param pass
   *          the password for binding
   * @return a connected <code>LDAPConnection</code>
   * @throws LDAPException
   *           if the <code>LDAPConnection</code> allocation fails
   */
 // Connection getBoundConnection(String dn, String pass) throws LdapException;

  /**
   * Return an <code>LDAPConnection</code>. This can allow for connections to be pooled
   * instead of just destroyed.
   * 
   * @param conn
   *          an <code>LDAPConnection</code> that you no longer need
   */
  void returnConnection(Connection conn);


  /**
   * Retrieve the currently assigned {@link LdapConnectionManagerConfig}.
   * 
   * @return the currently assigned {@link LdapConnectionManagerConfig}, if any
   */
  //LdapConnectionManagerConfig getConfig();
  
  String lookupUserDomain(String user) throws LdapException;

  SearchFilter createSearchFilter(String user);

  SearchResult performLdapSearch(SearchFilter filter, String user, boolean rootSearch, String baseDn)
		throws LdapException;

  SearchRequest createSearchRequest(SearchFilter filter, String user, boolean rootSearch, boolean subTreeSearch,
		String baseDn) throws DomainNotFoundException;

  String resolveDn(LdapEntry entry);
}
