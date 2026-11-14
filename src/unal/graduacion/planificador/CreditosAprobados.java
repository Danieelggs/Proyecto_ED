package unal.graduacion.planificador;

import unal.graduacion.modelo.Asignatura;

/**
 * Créditos aprobados hasta cierto semestre, en total y por componente.
 * Sirve para revisar requisitos como "haber aprobado 60 créditos del
 * componente disciplinar" (Trabajo de grado). Todas las operaciones son O(1).
 */
public final class CreditosAprobados {

    private final long[] porComponente = new long[128];
    private long total;

    /**
     * Suma los créditos de una asignatura aprobada.
     *
     * @param asignatura asignatura aprobada
     */
    public void sumar(Asignatura asignatura) {
        total += asignatura.creditos();
        char componente = asignatura.componente();
        if (componente < porComponente.length) {
            porComponente[componente] += asignatura.creditos();
        }
    }

    /** @return créditos aprobados en total */
    public long total() {
        return total;
    }

    /**
     * @param componente letra del componente, o {@link Asignatura#TODOS_LOS_COMPONENTES}
     * @return créditos aprobados que cuentan para ese componente
     */
    public long de(char componente) {
        if (componente == Asignatura.TODOS_LOS_COMPONENTES) {
            return total;
        }
        return componente < porComponente.length ? porComponente[componente] : 0;
    }

    /**
     * @param asignatura asignatura que se quiere cursar
     * @return {@code true} si su requisito de créditos (si lo tiene) ya se cumple
     */
    public boolean cumpleRequisito(Asignatura asignatura) {
        return asignatura.creditosMinimos() <= 0 || de(asignatura.componenteRequisito()) >= asignatura.creditosMinimos();
    }
}
