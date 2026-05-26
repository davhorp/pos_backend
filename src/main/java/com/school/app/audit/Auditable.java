package com.school.app.audit;

import com.school.app.entity.SystemAuditLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// La anotación estará disponible en tiempo de ejecución (RUNTIME)
@Retention(RetentionPolicy.RUNTIME)
// Solo se puede aplicar a métodos
@Target(ElementType.METHOD)
public @interface Auditable {

    // Solicitamos la acción obligatoria (ej. LOGIN_SUCCESS, REPORT_GENERATED)
    SystemAuditLog.AuditAction action();

    // Opcional: El nombre de la entidad afectada (ej. "PRODUCT", "SALE")
    String entityName() default "";

}