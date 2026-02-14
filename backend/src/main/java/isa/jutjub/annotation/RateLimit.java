package isa.jutjub.annotation;

import isa.jutjub.service.RateLimitingService.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    RateLimitType type();
    int weight() default 1;
    String identifier() default "";
}
