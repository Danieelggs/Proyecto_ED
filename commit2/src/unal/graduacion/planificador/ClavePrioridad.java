package unal.graduacion.planificador;

/**
 * Clave del AVL de asignaturas disponibles.
 *
 * <p>Ordena por altura y desempata por ID, de modo que el máximo del árbol es
 * la asignatura de mayor altura y, entre las de igual altura, la de ID menor.
 * Es el orden que muestra el ejemplo del README (101 antes que 106). El ID
 * hace que dos claves nunca sean iguales.</p>
 */
public final class ClavePrioridad implements Comparable<ClavePrioridad> {

    private final int altura;
    private final long id;

    /**
     * @param altura altura de la asignatura
     * @param id     código de la asignatura
     */
    public ClavePrioridad(int altura, long id) {
        this.altura = altura;
        this.id = id;
    }

    /** @return altura de la asignatura */
    public int altura() {
        return altura;
    }

    /** @return código de la asignatura */
    public long id() {
        return id;
    }

    /**
     * Mayor altura = mayor prioridad; con igual altura, ID menor = mayor prioridad. O(1).
     */
    @Override
    public int compareTo(ClavePrioridad otra) {
        if (altura != otra.altura) {
            return Integer.compare(altura, otra.altura);
        }
        return Long.compare(otra.id, id);
    }

    @Override
    public boolean equals(Object otro) {
        if (!(otro instanceof ClavePrioridad)) {
            return false;
        }
        ClavePrioridad clave = (ClavePrioridad) otro;
        return altura == clave.altura && id == clave.id;
    }

    @Override
    public int hashCode() {
        return 31 * altura + Long.hashCode(id);
    }
}
