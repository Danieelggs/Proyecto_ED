package unal.graduacion.modelo;

import unal.graduacion.estructuras.ListaEnlazada;

/**
 * Asignatura del plan de estudios: un nodo del grafo de prerrequisitos.
 *
 * Cada asignatura guarda dos listas de adyacencia:
 *  - prerrequisitos: aristas entrantes (lo que debe aprobarse antes).
 *  - dependientes:   aristas salientes (lo que se habilita al aprobarla).
 * Tener ambas permite el orden topológico (dependientes) y validar la
 * entrada / mostrar información (prerrequisitos) sin recorrer todo el plan.
 */
public class Asignatura {

    private final int id;
    private final String nombre;
    private final ListaEnlazada<Asignatura> prerrequisitos = new ListaEnlazada<>();
    private final ListaEnlazada<Asignatura> dependientes = new ListaEnlazada<>();

    /** Posición 0..n-1 asignada al registrarse; indexa arreglos auxiliares del planificador. */
    private int indice = -1;

    public Asignatura(int id, String nombre) {
        if (id < 0) {
            throw new IllegalArgumentException("El ID debe ser no negativo: " + id);
        }
        this.id = id;
        this.nombre = nombre == null ? "" : nombre.trim();
    }

    /** Registra que {@code prerrequisito} debe aprobarse antes que esta asignatura. */
    public void agregarPrerrequisito(Asignatura prerrequisito) {
        if (prerrequisito == this) {
            throw new IllegalArgumentException("La asignatura " + id + " no puede ser prerrequisito de sí misma");
        }
        if (prerrequisitos.contiene(prerrequisito)) {
            return; // prerrequisito repetido en la entrada: se ignora
        }
        prerrequisitos.agregarFinal(prerrequisito);
        prerrequisito.dependientes.agregarFinal(this);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public ListaEnlazada<Asignatura> getPrerrequisitos() {
        return prerrequisitos;
    }

    public ListaEnlazada<Asignatura> getDependientes() {
        return dependientes;
    }

    public int getIndice() {
        return indice;
    }

    void setIndice(int indice) {
        this.indice = indice;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre;
    }
}
