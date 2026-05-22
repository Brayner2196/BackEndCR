package com.backendcr.residentialcomplex.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "La contraseña no cumple con la política de seguridad";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
