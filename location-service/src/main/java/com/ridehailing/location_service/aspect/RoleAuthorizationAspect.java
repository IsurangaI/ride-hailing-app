package com.ridehailing.location_service.aspect;

import com.ridehailing.location_service.annontation.RequireRole;
import com.ridehailing.location_service.exception.DriverRequestValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RoleAuthorizationAspect {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new DriverRequestValidationException("Access Denied: Unable to verify role.");
        }

        HttpServletRequest request = attributes.getRequest();
        String userRole = request.getHeader(USER_ROLE_HEADER);
        String requiredRole = requireRole.value();

        if (userRole == null || !requiredRole.equals(userRole)) {
            throw new DriverRequestValidationException(
                    "Access Denied: Only " + requiredRole + " role can access this resource.");
        }
    }
}
