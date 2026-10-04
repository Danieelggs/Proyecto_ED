package unal.graduacion.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista simplemente enlazada con referencia a la cola.
 *
 * Uso en el proyecto: listas de adyacencia de cada asignatura
 * (prerrequisitos y dependientes) y listas de asignaturas por semestre.
 *
 * Complejidades:
 *  - agregarFinal / agregarInicio: O(1)
 *  - obtener(i): O(i)
 *  - recorrido completo: O(n)
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    /** Agrega al final en O(1) gracias a la referencia a la cola. */
    public void agregarFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamano++;
    }

    /** Agrega al inicio en O(1). */
    public void agregarInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /** Devuelve el elemento en la posición i en O(i). */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamano + ")");
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    /** Indica si la lista contiene el dato (comparación con equals) en O(n). */
    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.dato == null ? dato == null : n.dato.equals(dato)) {
                return true;
            }
        }
        return false;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }
}
