package org.apache.sling.auth.sso.cas.api;

import javax.servlet.http.HttpServletRequest;

/**
 * Validates if the Token is suitable for this request.
 */
public interface TokenTrustValidator {
  boolean isTrusted(HttpServletRequest request);
}
