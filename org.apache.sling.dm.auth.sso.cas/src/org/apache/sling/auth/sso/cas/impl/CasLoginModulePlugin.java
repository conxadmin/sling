package org.apache.sling.auth.sso.cas.impl;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.sso.cas.api.ILDAPLoginUserManager;
import org.apache.sling.auth.sso.cas.api.SsoPrincipal;

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

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

/**
 * Plugin for interacting with the JCR authentication cycle.
 */
public class CasLoginModulePlugin implements LoginModulePlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(CasLoginModulePlugin.class);
	
	private volatile ILDAPLoginUserManager ldapLoginUserManager;

  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set principals) {
  }

  public boolean canHandle(Credentials credentials) {
    return (getPrincipal(credentials) != null);
  }

  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
  }

  public AuthenticationPlugin getAuthentication(Principal principal,
      Credentials credentials) throws RepositoryException {
    AuthenticationPlugin plugin = null;
    if (canHandle(credentials)) {
      plugin = new CasAuthenticationPlugin(this);
    }
    return plugin;
  }

  public Principal getPrincipal(Credentials credentials) {
      logger.debug("getPrincipal({})", credentials);
      try {
          User user = ldapLoginUserManager.getUser(credentials);
          if (user == null && ldapLoginUserManager.autoCreate()) {
              user = ldapLoginUserManager.createUser(credentials);
          }
          if (user != null) {
              return user.getPrincipal();
          }
      } catch (RepositoryException e) {
          logger.error(e.getMessage(), e);
      }
      return null;
  }

  public int impersonate(Principal principal, Credentials credentials)
      throws RepositoryException, FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }
}
