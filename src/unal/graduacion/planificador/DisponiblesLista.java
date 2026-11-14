package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;

/**
 * Línea base para la comparativa: las asignaturas disponibles en una lista
 * sin orden. Agregar es O(1), pero sacar la mejor exige recorrer toda la lista:
 * O(n) por extracción y O(n²) en total. No la usa el sistema; sirve para
 * mostrar con tiempos reales por qué se eligió el AVL.
 */
public final class DisponiblesLista implements Disponibles {

    private final ListaEnlazada<Asignatura> lista = new ListaEnlazada<>();
    private final int[] altura;

    /**
     * @param altura altura de cada asignatura, por índice interno
     */
    public DisponiblesLista(int[] altura) {
        this.altura = altura;
    }

    /** O(1). */
    @Override
    public void agregar(Asignatura asignatura) {
        lista.agregarAlFinal(asignatura);
    }

    /** O(n): recorre la lista buscando la mayor altura (ID menor al empatar). */
    @Override
    public Asignatura extraerMejor() {
        Asignatura mejor = null;
        for (ListaEnlazada.Nodo<Asignatura> nodo = lista.primero(); nodo != null; nodo = nodo.siguiente()) {
            Asignatura candidata = nodo.dato();
            if (mejor == null || esMejor(candidata, mejor)) {
                mejor = candidata;
            }
        }
        if (mejor == null) {
            throw new IllegalStateException("No hay asignaturas disponibles");
        }
        lista.eliminar(mejor);
        return mejor;
    }

    private boolean esMejor(Asignatura a, Asignatura b) {
        int alturaA = altura[a.indice()];
        int alturaB = altura[b.indice()];
        if (alturaA != alturaB) {
            return alturaA > alturaB;
        }
        return a.id() < b.id();
    }

    @Override
    public int tamano() {
        return lista.tamano();
    }

    @Override
    public boolean estaVacia() {
        return lista.estaVacia();
    }

    @Override
    public String nombre() {
        return "Lista";
    }
}
