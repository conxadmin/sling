<%@page import="javax.jcr.Session"%>
<%@page session="false" contentType="text/html; charset=utf-8"
	trimDirectiveWhitespaces="true"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<sling:defineObjects />
<c:set var="staticRoot" value="/${slingRequest.resourceResolver.tenantID}/apps/browser/static" scope="request" />

<!DOCTYPE html>
<html>
<script type="text/javascript">
    var tenantID = '${slingRequest.resourceResolver.tenantID}';
</script>
<c:choose>
	<c:when test="${slingRequest.resourceResolver.userID != 'anonymous'}">
		<head>
			<sling:include replaceSelectors="components.head" />
		</head>
		<body>
			<sling:include replaceSelectors="components.bodyContent" />
			<sling:include replaceSelectors="components.modals" />
			<sling:include replaceSelectors="components.tail" />
		</body>
	</c:when>
	<c:otherwise>
		<%-- Not login use CDN for styling --%>
		<head>
			<title>Sling Browser SSON Login required</title>
			<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css">
		</head>
		<body>
		<sling:include replaceSelectors="components.login" />
			<script src="//code.jquery.com/jquery-2.1.1.min.js"></script>
			<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
			<script>
			//$.post('/${slingRequest.resourceResolver.tenantID}/system/sling/cas/login','{"sakaiauth:login":"2"}');
			var browserLocation='/${slingRequest.resourceResolver.tenantID}/browser.html';
			$(location).attr('href', '/${slingRequest.resourceResolver.tenantID}/system/sling/cas/login?targetService='+browserLocation);
// 			$("#loginModal").on('shown.bs.modal', function() {
// 				$('#login-form #j_username').focus();
// 			});
// 			$('#login-form').on('submit', function(event) {
// 				event.preventDefault();
// 				$.post('${slingRequest.resourceResolver.tenantID}/j_security_check', $(this).serialize(), function(data) {
// 					$('#login-form .alert').addClass('hide');
// 					window.location.reload(true);
// 				}).fail(function() {
// 					$('#login-form .alert').removeClass('hide').fadeOut(500).fadeIn(1000);
					
// 				})
// 			});
// 			$('#loginModal').modal('show');
			</script>
		</body>
	</c:otherwise>
</c:choose>
</html>





