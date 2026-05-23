package com.backendcr.residentialcomplex.entity.enums;

public enum EstadoPlan {
    /** Residente solicitó el plan, espera aprobación del admin */
    PENDIENTE,
    /** Admin aprobó — cuotas generadas, plan en curso */
    ACTIVO,
    /** Admin rechazó la solicitud */
    RECHAZADO,
    /** Todas las cuotas fueron pagadas */
    COMPLETADO,
    /** Admin o residente canceló el plan manualmente */
    CANCELADO
}
