package org.apache.sling.contrib.ldap.api;

public class UserNotFoundException extends Exception {
	public UserNotFoundException(Exception e) {
		super(e);
	}
}
