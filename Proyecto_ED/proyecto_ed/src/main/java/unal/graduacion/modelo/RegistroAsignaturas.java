package unal.graduacion.modelo;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.ListaEnlazada;

/**
 * Registro de todas las asignaturas del plan (RF1 y RF2).
 *
 * Entrega 2: árbol AVL indexado por ID → buscar e insertar en O(log n).
 * Entrega 3: se reemplazará por una tabla hash (O(1) promedio) y se
 * compararán ambos con mediciones de tiempo.
 */
public class RegistroAsignaturas {

    private final ArbolAVL<Integer, Asignatura> porId = new ArbolAVL<>();
    private final ListaEnlazada<Asignatura> enOrdenDeRegistro = new ListaEnlazada<>();

    /** Registra una asignatura nueva. Rechaza IDs duplicados. */
    public void registrar(Asignatura asignatura) {
        if (porId.contiene(asignatura.getId())) {
            throw new IllegalArgumentException("ID duplicado: " + asignatura.getId());
        }
        asignatura.setIndice(porId.tamano());
        porId.insertar(asignatura.getId(), asignatura);
        enOrdenDeRegistro.agregarFinal(asignatura);
    }

    /** Acceso por ID (RF2). Devuelve null si no existe. */
    public Asignatura buscar(int id) {
        return porId.buscar(id);
    }

    /** Acceso por ID que lanza un error descriptivo si no existe. */
    public Asignatura obtener(int id) {
        Asignatura a = porId.buscar(id);
        if (a == null) {
            throw new IllegalArgumentException("No existe una asignatura con ID " + id);
        }
        return a;
    }

    /** Asignaturas en el orden en que se registraron, O(1). */
    public ListaEnlazada<Asignatura> todas() {
        return enOrdenDeRegistro;
    }

    public int tamano() {
        return porId.tamano();
    }

    /** Altura del AVL interno, para el análisis de la entrega 2. */
    public int alturaArbol() {
        return porId.altura();
    }
}
