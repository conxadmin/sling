/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jackrabbit.usermanager.impl.post;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Servlet;

import org.apache.felix.dm.Component;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.ChangeUserPassword;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Changes the password associated with a user. Maps on to nodes of resourceType <code>sling/user</code> like
 * <code>/rep:system/rep:userManager/rep:users/ae/fd/3e/ieb</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.changePassword.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>oldPwd</dt>
 * <dd>The current password for the user (required for non-administrators)</dd>
 * <dt>newPwd</dt>
 * <dd>The new password for the user (required)</dd>
 * <dt>newPwdConfirm</dt>
 * <dd>The confirm new password for the user (required)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success sent with no body</dd>
 * <dt>404</dt>
 * <dd>If the user was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure, including password validation errors. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -FoldPwd=oldpassword -FnewPwd=newpassword -FnewPwdConfirm=newpassword http://localhost:8080/system/userManager/user/ieb.changePassword.html
 * </code>
 *
 * <h4>Notes</h4>
 */
public class ChangeUserPasswordServlet extends AbstractUserPostServlet implements ChangeUserPassword, ManagedService {
    private static final long serialVersionUID = 1923614318474654502L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The default 'User administrator' group name
     *
     * @see #PAR_USER_ADMIN_GROUP_NAME
     */
    private static final String DEFAULT_USER_ADMIN_GROUP_NAME = "UserAdmin";
    
    private volatile BundleContext componentContext;

    /**
     * The name of the configuration parameter providing the
     * name of the group whose members are allowed to reset the password
     * of a user without the 'oldPwd' value.
     */
    private static final String PAR_USER_ADMIN_GROUP_NAME = "user.admin.group.name";

    private String userAdminGroupName = DEFAULT_USER_ADMIN_GROUP_NAME;

    // ---------- DM integration ---------------------------------------------
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		super.properties = properties;
	}
    /**
     * Activates this component.
     *
     * @param componentContext The OSGi <code>ComponentContext</code> of this
     *            component.
     */
    @Override
    protected void activate(Component component) {
    	if (super.properties == null)
    		this.properties = new Hashtable<>();
	    
    	super.activate(component);
        Dictionary<?, ?> props = this.properties;

        this.userAdminGroupName = OsgiUtil.toString(props.get(PAR_USER_ADMIN_GROUP_NAME),
                DEFAULT_USER_ADMIN_GROUP_NAME);
        log.info("User Admin Group Name {}", this.userAdminGroupName);
    }

    @Override
    protected void deactivate() {
        super.deactivate();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
    		AbstractPostResponse response, List<Modification> changes)
            throws RepositoryException {

        Resource resource = request.getResource();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        changePassword(session,
                resource.getName(),
                request.getParameter("oldPwd"),
                request.getParameter("newPwd"),
                request.getParameter("newPwdConfirm"),
                changes);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.ChangeUserPassword#changePassword(javax.jcr.Session, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List)
     */
    public User changePassword(Session jcrSession,
                                String name,
                                String oldPassword,
                                String newPassword,
                                String newPasswordConfirm,
                                List<Modification> changes)
                throws RepositoryException {

        if ("anonymous".equals(name)) {
            throw new RepositoryException(
                "Can not change the password of the anonymous user.");
        }

        User user;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof User) {
            user = (User)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "User to update could not be determined");
        }

        //SLING-2069: if the current user is an administrator, then a missing oldPwd is ok,
        // otherwise the oldPwd must be supplied.
        boolean administrator = false;

        // check that the submitted parameter values have valid values.
        if (oldPassword == null || oldPassword.length() == 0) {
            try {
                UserManager um = AccessControlUtil.getUserManager(jcrSession);
                User currentUser = (User) um.getAuthorizable(jcrSession.getUserID());
                administrator = currentUser.isAdmin();

                if (!administrator) {
                    //check if the user is a member of the 'User administrator' group
                    Authorizable userAdmin = um.getAuthorizable(this.userAdminGroupName);
                    if (userAdmin instanceof Group) {
                        boolean isMember = ((Group)userAdmin).isMember(currentUser);
                        if (isMember) {
                            administrator = true;
                        }
                    }

                }
            } catch ( Exception ex ) {
                log.warn("Failed to determine if the user is an admin, assuming not. Cause: "+ex.getMessage());
                administrator = false;
            }
            if (!administrator) {
                throw new RepositoryException("Old Password was not submitted");
            }
        }
        if (newPassword == null || newPassword.length() == 0) {
            throw new RepositoryException("New Password was not submitted");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new RepositoryException(
                "New Password does not match the confirmation password");
        }

        try {
            if (oldPassword != null && oldPassword.length() > 0) {
                // verify old password
                user.changePassword(newPassword, oldPassword);
            } else {
                user.changePassword(newPassword);
            }

            final String passwordPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + user.getID() + "/rep:password";

            changes.add(Modification.onModified(passwordPath));
        } catch (RepositoryException re) {
            throw new RepositoryException("Failed to change user password.", re);
        }

        return user;
    }
}
