package com.ridehailing.location_service.annontation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // Applies to controller methods
@Retention(RetentionPolicy.RUNTIME) // Available at runtime via reflection
@Documented
public @interface RequireRole {
    String value(); // The allowed role, e.g., "DRIVER"
}
