package unal.graduacion.estructuras;

/**
 * Árbol binario de búsqueda sin balanceo.
 *
 * <p>No lo usa el planificador: existe para la comparativa del informe. Con
 * IDs que llegan ordenados (como los códigos de un plan de estudios), cada
 * inserción alarga una sola rama y el árbol se vuelve una lista: O(n) por
 * operación en vez de O(log n). Inserción, búsqueda y altura son iterativas
 * para que el caso degenerado no desborde la pila de Java.</p>
 *
 * @param <K> tipo de la clave, comparable
 * @param <V> tipo del valor asociado
 */
public class ArbolBST<K extends Comparable<K>, V> {

    private static final class Nodo<K, V> {
        private final K clave;
        private final V valor;
        private Nodo<K, V> izquierdo;
        private Nodo<K, V> derecho;

        private Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;

    /**
     * Inserta una pareja si la clave no existe. O(h): O(log n) si el árbol
     * queda equilibrado, O(n) si degenera.
     *
     * @param clave clave nueva (no nula)
     * @param valor valor asociado
     * @return {@code true} si se insertó; {@code false} si la clave ya estaba
     */
    public boolean insertar(K clave, V valor) {
        if (clave == null) {
            throw new IllegalArgumentException("La clave no puede ser nula");
        }
        if (raiz == null) {
            raiz = new Nodo<>(clave, valor);
            tamano++;
            return true;
        }
        Nodo<K, V> actual = raiz;
        while (true) {
            int c = clave.compareTo(actual.clave);
            if (c == 0) {
                return false;
            }
            if (c < 0) {
                if (actual.izquierdo == null) {
                    actual.izquierdo = new Nodo<>(clave, valor);
                    tamano++;
                    return true;
                }
                actual = actual.izquierdo;
            } else {
                if (actual.derecho == null) {
                    actual.derecho = new Nodo<>(clave, valor);
                    tamano++;
                    return true;
                }
                actual = actual.derecho;
            }
        }
    }

    /**
     * Busca el valor de una clave. O(h).
     *
     * @param clave clave buscada
     * @return el valor, o {@code null} si no está
     */
    public V buscar(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int c = clave.compareTo(actual.clave);
            if (c == 0) {
                return actual.valor;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    /**
     * Calcula la altura recorriendo el árbol por niveles con una cola. O(n).
     *
     * @return altura (0 si está vacío)
     */
    public int altura() {
        if (raiz == null) {
            return 0;
        }
        Cola<Nodo<K, V>> nivel = new Cola<>();
        nivel.encolar(raiz);
        int altura = 0;
        while (!nivel.estaVacia()) {
            altura++;
            int enEsteNivel = nivel.tamano();
            for (int i = 0; i < enEsteNivel; i++) {
                Nodo<K, V> nodo = nivel.desencolar();
                if (nodo.izquierdo != null) {
                    nivel.encolar(nodo.izquierdo);
                }
                if (nodo.derecho != null) {
                    nivel.encolar(nodo.derecho);
                }
            }
        }
        return altura;
    }

    /** @return cantidad de claves. O(1). */
    public int tamano() {
        return tamano;
    }
}
