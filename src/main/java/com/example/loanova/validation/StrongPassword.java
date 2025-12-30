package com.example.loanova.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default
            "Password minimal 8 karakter dan harus mengandung huruf besar, huruf kecil, angka, dan simbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
