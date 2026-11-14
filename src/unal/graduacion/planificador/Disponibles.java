package unal.graduacion.planificador;

import unal.graduacion.modelo.Asignatura;

/**
 * Conjunto de asignaturas disponibles del que el planificador saca, cada
 * semestre, las de mayor prioridad (mayor altura y, al empatar, menor ID).
 *
 * <p>Separar esta interfaz permite comparar implementaciones con el mismo
 * planificador: {@link DisponiblesAVL} (entrega 2), {@link DisponiblesLista}
 * (línea base para la comparativa) y, en la entrega 3, un heap de máximos.</p>
 */
public interface Disponibles {

    /**
     * Agrega una asignatura que acaba de quedar disponible.
     *
     * @param asignatura asignatura disponible
     */
    void agregar(Asignatura asignatura);

    /**
     * Quita y devuelve la asignatura de mayor prioridad.
     *
     * @return la asignatura de mayor altura (ID menor al empatar)
     */
    Asignatura extraerMejor();

    /** @return cantidad de asignaturas disponibles */
    int tamano();

    /** @return {@code true} si no hay asignaturas disponibles */
    boolean estaVacia();

    /** @return nombre de la estructura, para los reportes de medición */
    String nombre();
}
