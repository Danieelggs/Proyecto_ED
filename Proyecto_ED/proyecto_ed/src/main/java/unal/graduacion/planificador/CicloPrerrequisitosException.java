package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;

/**
 * El plan tiene un ciclo de prerrequisitos (A antes de B y B antes de A),
 * por lo que es imposible de terminar (RF3).
 */
public class CicloPrerrequisitosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ListaEnlazada<Asignatura> bloqueadas;

    public CicloPrerrequisitosException(ListaEnlazada<Asignatura> bloqueadas) {
        super("El plan tiene un ciclo de prerrequisitos. Asignaturas en el ciclo o bloqueadas por él: " + bloqueadas);
        this.bloqueadas = bloqueadas;
    }

    public ListaEnlazada<Asignatura> getBloqueadas() {
        return bloqueadas;
    }
}
