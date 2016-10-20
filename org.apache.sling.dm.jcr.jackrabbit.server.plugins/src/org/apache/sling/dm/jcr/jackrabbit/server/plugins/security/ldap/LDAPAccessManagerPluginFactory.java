package org.apache.sling.dm.jcr.jackrabbit.server.plugins.security.ldap;

import java.security.Principal;
import java.util.Set;

import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPluginFactory;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.WorkspaceAccessManagerPlugin;

public class LDAPAccessManagerPluginFactory implements AccessManagerPluginFactory {
	public AccessManagerPlugin getAccessManager() {
		return new LDAPAccessManagerPlugin();
	}

	public class LDAPAccessManagerPlugin implements AccessManagerPlugin {
		private Subject subject;
		private Session originalSession;
		private boolean isAdminSession;

		public void init(Subject subject, Session originalSession) {
			this.subject = subject;
			this.originalSession = originalSession;
			Set<Principal> subjectPrincipals = this.subject.getPrincipals();
			this.isAdminSession = false;
			for (Principal subjectPrincipal : subjectPrincipals) {
				if (subjectPrincipal instanceof org.apache.jackrabbit.core.security.principal.AdminPrincipal) {
					this.isAdminSession = true;
					break;
				}
			}
		}

		public boolean isGranted(String path, int bits) {
			if (this.isAdminSession) {
				return true;
			}
			
			return false;
			// Implement custom logic here
		}

		public boolean canRead(String path) {
			return isGranted(path,
					org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPlugin.READ);
		}

		@Override
		public void close() throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public WorkspaceAccessManagerPlugin getWorkspaceAccessManager() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}