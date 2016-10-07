package org.apache.sling.contrib.ldap.api;

public class DomainNotFoundException extends Exception {
	public DomainNotFoundException(Exception e) {
		super(e);
	}

	public DomainNotFoundException() {
		super();
	}
}
