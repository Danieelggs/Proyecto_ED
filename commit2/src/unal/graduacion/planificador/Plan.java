package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;

/** Resultado del planificador: los semestres y en cuál quedó cada asignatura (RF4, RF5). */
public final class Plan {

    private final ListaEnlazada<Semestre> semestres = new ListaEnlazada<>();
    private final int[] semestreDe;
    private final int k;
    private final String estructura;
    private String problema;

    Plan(int cantidadAsignaturas, int k, String estructura) {
        this.semestreDe = new int[cantidadAsignaturas];
        this.k = k;
        this.estructura = estructura;
    }

    void agregarSemestre(Semestre semestre) {
        semestres.agregarAlFinal(semestre);
        for (ListaEnlazada.Nodo<Asignatura> nodo = semestre.asignaturas().primero(); nodo != null; nodo = nodo.siguiente()) {
            semestreDe[nodo.dato().indice()] = semestre.numero();
        }
    }

    void marcarProblema(String problema) {
        this.problema = problema;
    }

    /** @return número de semestres del plan */
    public int cantidadSemestres() {
        return semestres.tamano();
    }

    /** @return los semestres, en orden */
    public ListaEnlazada<Semestre> semestres() {
        return semestres;
    }

    /**
     * @param indice índice interno de una asignatura
     * @return semestre donde quedó (desde 1), o 0 si no se pudo ubicar
     */
    public int semestreDe(int indice) {
        return semestreDe[indice];
    }

    /** @return k usado para planificar */
    public int k() {
        return k;
    }

    /** @return estructura de asignaturas disponibles que se usó ("AVL", "Lista"...) */
    public String estructura() {
        return estructura;
    }

    /** @return {@code true} si todas las asignaturas quedaron ubicadas */
    public boolean esCompleto() {
        return problema == null;
    }

    /** @return por qué no se pudo completar el plan, o {@code null} si está completo */
    public String problema() {
        return problema;
    }
}
