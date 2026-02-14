package isa.jutjub.aspect;

import isa.jutjub.annotation.RateLimit;
import isa.jutjub.service.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String identifier = getIdentifier(rateLimit, request);
        
        boolean allowed = rateLimitingService.isAllowed(identifier, rateLimit.type(), rateLimit.weight());
        
        if (!allowed) {
            log.warn("Rate limit exceeded for identifier: {}, type: {}, method: {}", 
                    identifier, rateLimit.type(), joinPoint.getSignature().toShortString());
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }
        
        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No request context available");
        }
        return attributes.getRequest();
    }

    private String getIdentifier(RateLimit rateLimit, HttpServletRequest request) {
        if (!rateLimit.identifier().isEmpty()) {
            return rateLimit.identifier();
        }
        
        String userId = extractUserId(request);
        if (userId != null) {
            return "user:" + userId;
        }
        
        return "ip:" + rateLimitingService.getClientIdentifier(request);
    }

    private String extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return extractUserIdFromToken(authHeader.substring(7));
            } catch (Exception e) {
                log.debug("Failed to extract user ID from token: {}", e.getMessage());
            }
        }
        return null;
    }

    private String extractUserIdFromToken(String token) {
        return null;
    }
}
