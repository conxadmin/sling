package org.apache.sling.auth.sso.cas.api;

import javax.jcr.Credentials;

import org.apache.jackrabbit.api.security.user.User;

public interface ILDAPLoginUserManager {

    String getHash(User user);
    
    boolean autoCreate();

    boolean autoUpdate();

    User getUser(Credentials credentials);

    User createUser(Credentials credentials);

    User updateUser(Credentials credentials);
}
