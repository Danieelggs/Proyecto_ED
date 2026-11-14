package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Comprueba que un plan cumple todas las reglas. Lo usan las pruebas y las
 * mediciones (fuera del tiempo medido) para asegurar que lo que se mide es
 * un plan correcto. O(n + e).
 */
public final class VerificadorPlan {

    private VerificadorPlan() {
    }

    /**
     * Revisa que cada asignatura esté exactamente una vez, que ningún semestre
     * pase de k ni quede vacío, que cada prerrequisito esté en un semestre
     * anterior y que se cumplan los requisitos de créditos.
     *
     * @param registro plan de estudios
     * @param plan     plan por semestres
     * @return {@code null} si es válido; si no, la primera regla que falla
     */
    public static String verificar(RegistroAsignaturas registro, Plan plan) {
        if (!plan.esCompleto()) {
            return "El plan está incompleto: " + plan.problema();
        }
        int n = registro.cantidad();
        int semestres = plan.cantidadSemestres();
        int[] cuenta = new int[semestres + 1];

        int total = 0;
        int numeroEsperado = 1;
        for (ListaEnlazada.Nodo<Semestre> nodo = plan.semestres().primero(); nodo != null; nodo = nodo.siguiente()) {
            Semestre semestre = nodo.dato();
            if (semestre.numero() != numeroEsperado) {
                return "Los semestres no están numerados en orden (se esperaba " + numeroEsperado + ").";
            }
            int tamano = semestre.asignaturas().tamano();
            if (tamano == 0) {
                return "El semestre " + semestre.numero() + " está vacío.";
            }
            if (tamano > plan.k()) {
                return "El semestre " + semestre.numero() + " tiene " + tamano + " asignaturas y k = " + plan.k() + ".";
            }
            cuenta[semestre.numero()] = tamano;
            total += tamano;
            numeroEsperado++;
        }
        if (total != n) {
            return "El plan ubica " + total + " asignaturas y el registro tiene " + n + ".";
        }

        int[] vistas = new int[semestres + 1];
        for (int i = 0; i < n; i++) {
            Asignatura asignatura = registro.obtener(i);
            int s = plan.semestreDe(i);
            if (s < 1 || s > semestres) {
                return asignatura + " no quedó ubicada.";
            }
            vistas[s]++;
            for (ListaEnlazada.Nodo<Asignatura> nodo = asignatura.prerrequisitos().primero(); nodo != null; nodo = nodo.siguiente()) {
                Asignatura prerrequisito = nodo.dato();
                if (plan.semestreDe(prerrequisito.indice()) >= s) {
                    return asignatura + " (semestre " + s + ") no tiene antes a su prerrequisito " + prerrequisito
                            + " (semestre " + plan.semestreDe(prerrequisito.indice()) + ").";
                }
            }
        }
        for (int s = 1; s <= semestres; s++) {
            if (vistas[s] != cuenta[s]) {
                return "Hay asignaturas repetidas en el semestre " + s + ".";
            }
        }

        // Requisitos de créditos: se revisan contra lo aprobado antes de cada semestre.
        CreditosAprobados aprobados = new CreditosAprobados();
        for (ListaEnlazada.Nodo<Semestre> nodo = plan.semestres().primero(); nodo != null; nodo = nodo.siguiente()) {
            Semestre semestre = nodo.dato();
            for (ListaEnlazada.Nodo<Asignatura> a = semestre.asignaturas().primero(); a != null; a = a.siguiente()) {
                if (!aprobados.cumpleRequisito(a.dato())) {
                    return a.dato() + " exige " + a.dato().requisitoCreditosTexto() + " y antes del semestre "
                            + semestre.numero() + " solo hay " + aprobados.de(a.dato().componenteRequisito()) + ".";
                }
            }
            for (ListaEnlazada.Nodo<Asignatura> a = semestre.asignaturas().primero(); a != null; a = a.siguiente()) {
                aprobados.sumar(a.dato());
            }
        }
        return null;
    }
}
