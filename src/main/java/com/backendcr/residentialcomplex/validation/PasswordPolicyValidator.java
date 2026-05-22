package com.backendcr.residentialcomplex.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida la política de contraseñas:
 * - Entre 8 y 20 caracteres
 * - Al menos 1 letra mayúscula
 * - Al menos 1 carácter especial
 * - Sin espacios en blanco
 * - No contraseña común (lista negra)
 * - No 3+ caracteres repetidos consecutivos (ej. "aaa")
 * - Al menos 3 de los 4 tipos: minúscula, mayúscula, número, especial
 */
public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE  = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE  = Pattern.compile("[a-z]");
    private static final Pattern DIGIT      = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL    = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern SPACES     = Pattern.compile("\\s");
    private static final Pattern REPEATED   = Pattern.compile("(.)\\1{2,}");

    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "12345678", "123456789", "1234567890", "password", "password1",
        "qwerty123", "qwertyuiop", "iloveyou", "admin1234", "welcome1",
        "monkey123", "dragon123", "master123", "abc12345", "letmein1",
        "sunshine", "princess", "football", "shadow123", "superman",
        "contraseña", "colombia1", "bogota123", "medellin1", "cali1234"
    );

    @Override
    public boolean isValid(String password, ConstraintValidatorContext ctx) {
        if (password == null || password.isBlank()) return true; // @NotBlank lo maneja aparte

        ctx.disableDefaultConstraintViolation();
        boolean valida = true;

        if (password.length() < 8 || password.length() > 20) {
            ctx.buildConstraintViolationWithTemplate("Debe tener entre 8 y 20 caracteres")
               .addConstraintViolation();
            valida = false;
        }
        if (!UPPERCASE.matcher(password).find()) {
            ctx.buildConstraintViolationWithTemplate("Debe contener al menos una letra mayúscula")
               .addConstraintViolation();
            valida = false;
        }
        if (!SPECIAL.matcher(password).find()) {
            ctx.buildConstraintViolationWithTemplate("Debe contener al menos un carácter especial")
               .addConstraintViolation();
            valida = false;
        }
        if (SPACES.matcher(password).find()) {
            ctx.buildConstraintViolationWithTemplate("No puede contener espacios en blanco")
               .addConstraintViolation();
            valida = false;
        }
        if (REPEATED.matcher(password).find()) {
            ctx.buildConstraintViolationWithTemplate("No puede tener 3 o más caracteres repetidos consecutivos")
               .addConstraintViolation();
            valida = false;
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            ctx.buildConstraintViolationWithTemplate("La contraseña es demasiado común")
               .addConstraintViolation();
            valida = false;
        }

        int tipos = 0;
        if (LOWERCASE.matcher(password).find()) tipos++;
        if (UPPERCASE.matcher(password).find()) tipos++;
        if (DIGIT.matcher(password).find())     tipos++;
        if (SPECIAL.matcher(password).find())   tipos++;

        if (tipos < 3) {
            ctx.buildConstraintViolationWithTemplate(
                "Debe combinar al menos 3 tipos: minúscula, mayúscula, número, carácter especial")
               .addConstraintViolation();
            valida = false;
        }

        return valida;
    }
}
