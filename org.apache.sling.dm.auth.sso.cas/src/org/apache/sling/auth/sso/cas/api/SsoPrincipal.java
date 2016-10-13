package org.apache.sling.auth.sso.cas.api;

import java.security.Principal;

public class SsoPrincipal implements Principal {
	private String principalName;

	public SsoPrincipal(String principalName) {
		this.principalName = principalName;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.security.Principal#getName()
	 */
	@Override
	public String getName() {
		return principalName;
	}
}