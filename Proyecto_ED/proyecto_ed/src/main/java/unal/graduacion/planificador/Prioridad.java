package unal.graduacion.planificador;

/**
 * Clave de prioridad de una asignatura disponible.
 *
 * Mayor altura = mayor prioridad (abre la cadena de prerrequisitos más larga).
 * Empates: gana el ID menor, para que el resultado sea determinista.
 * Así, el máximo del AVL es siempre la siguiente asignatura a cursar.
 */
public final class Prioridad implements Comparable<Prioridad> {

    private final int altura;
    private final int id;

    public Prioridad(int altura, int id) {
        this.altura = altura;
        this.id = id;
    }

    @Override
    public int compareTo(Prioridad otra) {
        if (altura != otra.altura) {
            return Integer.compare(altura, otra.altura);
        }
        return Integer.compare(otra.id, id); // ID menor = clave mayor
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Prioridad p && p.altura == altura && p.id == id;
    }

    @Override
    public int hashCode() {
        return 31 * altura + id;
    }

    @Override
    public String toString() {
        return "(altura=" + altura + ", id=" + id + ")";
    }
}
