package unal.graduacion.estructuras;

/**
 * Lista simplemente enlazada con referencias al primer y al último nodo.
 *
 * <p>En el sistema guarda los prerrequisitos y los dependientes de cada
 * asignatura, las asignaturas de cada semestre, las optativas de una casilla
 * y los mensajes del cargador. En todos esos casos el tamaño es variable y la
 * lista solo se recorre en orden, así que agregar en O(1) es lo que importa.</p>
 *
 * <p>Para recorrerla sin usar {@code java.util} se exponen los nodos:</p>
 * <pre>
 * for (ListaEnlazada.Nodo&lt;T&gt; n = lista.primero(); n != null; n = n.siguiente()) {
 *     T dato = n.dato();
 * }
 * </pre>
 *
 * @param <T> tipo de los elementos
 */
public class ListaEnlazada<T> {

    /**
     * Nodo de la lista.
     *
     * @param <T> tipo del dato
     */
    public static final class Nodo<T> {
        private final T dato;
        private Nodo<T> siguiente;

        private Nodo(T dato) {
            this.dato = dato;
        }

        /** @return el elemento guardado en el nodo. O(1). */
        public T dato() {
            return dato;
        }

        /** @return el nodo siguiente, o {@code null} si este es el último. O(1). */
        public Nodo<T> siguiente() {
            return siguiente;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> ultimo;
    private int tamano;

    /**
     * Agrega un elemento al final. O(1).
     *
     * @param dato elemento a agregar
     */
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            ultimo.siguiente = nuevo;
        }
        ultimo = nuevo;
        tamano++;
    }

    /**
     * Agrega un elemento al inicio. O(1).
     *
     * @param dato elemento a agregar
     */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (ultimo == null) {
            ultimo = nuevo;
        }
        tamano++;
    }

    /**
     * Quita y devuelve el primer elemento. O(1).
     *
     * @return el primer elemento
     * @throws IllegalStateException si la lista está vacía
     */
    public T eliminarPrimero() {
        if (cabeza == null) {
            throw new IllegalStateException("La lista está vacía");
        }
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        if (cabeza == null) {
            ultimo = null;
        }
        tamano--;
        return dato;
    }

    /**
     * Devuelve el primer elemento sin quitarlo. O(1).
     *
     * @return el primer elemento
     * @throws IllegalStateException si la lista está vacía
     */
    public T obtenerPrimero() {
        if (cabeza == null) {
            throw new IllegalStateException("La lista está vacía");
        }
        return cabeza.dato;
    }

    /**
     * Devuelve el último elemento sin quitarlo. O(1).
     *
     * @return el último elemento
     * @throws IllegalStateException si la lista está vacía
     */
    public T obtenerUltimo() {
        if (ultimo == null) {
            throw new IllegalStateException("La lista está vacía");
        }
        return ultimo.dato;
    }

    /**
     * Devuelve el elemento en una posición. O(i).
     *
     * @param indice posición, de 0 a {@code tamano() - 1}
     * @return el elemento en esa posición
     * @throws IndexOutOfBoundsException si la posición no existe
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Posición " + indice + " fuera de la lista de tamaño " + tamano);
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    /**
     * Indica si la lista contiene un elemento (comparando con {@code equals}). O(n).
     *
     * @param dato elemento buscado
     * @return {@code true} si aparece al menos una vez
     */
    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (iguales(n.dato, dato)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Elimina la primera aparición de un elemento. O(n).
     *
     * @param dato elemento a eliminar
     * @return {@code true} si estaba y se eliminó
     */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        for (Nodo<T> n = cabeza; n != null; anterior = n, n = n.siguiente) {
            if (iguales(n.dato, dato)) {
                if (anterior == null) {
                    cabeza = n.siguiente;
                } else {
                    anterior.siguiente = n.siguiente;
                }
                if (n == ultimo) {
                    ultimo = anterior;
                }
                tamano--;
                return true;
            }
        }
        return false;
    }

    /** Vacía la lista. O(1). */
    public void limpiar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }

    /** @return el primer nodo, o {@code null} si la lista está vacía. O(1). */
    public Nodo<T> primero() {
        return cabeza;
    }

    /** @return cantidad de elementos. O(1). */
    public int tamano() {
        return tamano;
    }

    /** @return {@code true} si no tiene elementos. O(1). */
    public boolean estaVacia() {
        return tamano == 0;
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
