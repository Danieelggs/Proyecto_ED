package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;

/** Un semestre del plan: sus asignaturas y sus créditos (RF5). */
public final class Semestre {

    private final int numero;
    private final ListaEnlazada<Asignatura> asignaturas = new ListaEnlazada<>();
    private int creditos;
    private long creditosAcumulados;

    Semestre(int numero) {
        this.numero = numero;
    }

    void agregar(Asignatura asignatura) {
        asignaturas.agregarAlFinal(asignatura);
        creditos += asignatura.creditos();
    }

    void fijarCreditosAcumulados(long creditosAcumulados) {
        this.creditosAcumulados = creditosAcumulados;
    }

    /** @return número del semestre, desde 1 */
    public int numero() {
        return numero;
    }

    /** @return asignaturas del semestre, en el orden en que se eligieron (mayor prioridad primero) */
    public ListaEnlazada<Asignatura> asignaturas() {
        return asignaturas;
    }

    /** @return créditos del semestre */
    public int creditos() {
        return creditos;
    }

    /** @return créditos aprobados al terminar este semestre */
    public long creditosAcumulados() {
        return creditosAcumulados;
    }
}
