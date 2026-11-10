package unal.graduacion.estructuras;

/**
 * Árbol AVL: árbol binario de búsqueda que se mantiene balanceado con
 * rotaciones, así que su altura nunca pasa de 1,44·log2(n + 2).
 *
 * <p>El sistema usa dos:</p>
 * <ul>
 *   <li>Registro de asignaturas por ID (clave {@code Long}): los IDs son
 *       dispersos y suelen venir ordenados, y un BST simple degeneraría en una
 *       lista.</li>
 *   <li>Asignaturas disponibles por prioridad (clave {@code ClavePrioridad}):
 *       cada semestre se extraen las k de mayor altura sin reordenar nada.</li>
 * </ul>
 *
 * <p>Las claves no se repiten: insertar una clave existente no cambia el árbol.</p>
 *
 * @param <K> tipo de la clave, comparable
 * @param <V> tipo del valor asociado
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private static final class Nodo<K, V> {
        private final K clave;
        private V valor;
        private Nodo<K, V> izquierdo;
        private Nodo<K, V> derecho;
        private int altura = 1;

        private Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;
    private long rotaciones;

    /** Resultado auxiliar de la última inserción o eliminación recursiva. */
    private boolean cambio;
    /** Valor del nodo quitado en la última eliminación recursiva. */
    private V valorQuitado;

    // ------------------------------------------------------------------ consultas

    /** @return cantidad de claves guardadas. O(1). */
    public int tamano() {
        return tamano;
    }

    /** @return {@code true} si no tiene claves. O(1). */
    public boolean estaVacio() {
        return raiz == null;
    }

    /** @return altura del árbol (0 si está vacío, 1 con un solo nodo). O(1). */
    public int altura() {
        return altura(raiz);
    }

    /** @return rotaciones hechas desde que se creó el árbol (para el análisis). O(1). */
    public long rotaciones() {
        return rotaciones;
    }

    /**
     * Busca el valor asociado a una clave. O(log n), sin recursión.
     *
     * @param clave clave buscada
     * @return el valor, o {@code null} si la clave no está
     */
    public V buscar(K clave) {
        Nodo<K, V> nodo = nodoDe(clave);
        return nodo == null ? null : nodo.valor;
    }

    /**
     * Indica si una clave está en el árbol. O(log n).
     *
     * @param clave clave buscada
     * @return {@code true} si está
     */
    public boolean contiene(K clave) {
        return nodoDe(clave) != null;
    }

    /**
     * Devuelve la clave mayor. O(log n).
     *
     * @return la clave máxima
     * @throws IllegalStateException si el árbol está vacío
     */
    public K claveMaxima() {
        return nodoMaximo().clave;
    }

    /**
     * Devuelve el valor asociado a la clave mayor, sin quitarlo. O(log n).
     *
     * @return el valor de la clave máxima
     * @throws IllegalStateException si el árbol está vacío
     */
    public V valorMaximo() {
        return nodoMaximo().valor;
    }

    /**
     * Devuelve la clave menor. O(log n).
     *
     * @return la clave mínima
     * @throws IllegalStateException si el árbol está vacío
     */
    public K claveMinima() {
        if (raiz == null) {
            throw new IllegalStateException("El árbol está vacío");
        }
        return minimo(raiz).clave;
    }

    // ------------------------------------------------------------ modificaciones

    /**
     * Inserta una pareja clave–valor si la clave no existe. O(log n).
     *
     * @param clave clave nueva (no nula)
     * @param valor valor asociado
     * @return {@code true} si se insertó; {@code false} si la clave ya estaba
     */
    public boolean insertar(K clave, V valor) {
        if (clave == null) {
            throw new IllegalArgumentException("La clave no puede ser nula");
        }
        cambio = false;
        raiz = insertar(raiz, clave, valor);
        if (cambio) {
            tamano++;
        }
        return cambio;
    }

    /**
     * Elimina una clave. O(log n).
     *
     * @param clave clave a eliminar
     * @return el valor que tenía, o {@code null} si la clave no estaba
     */
    public V eliminar(K clave) {
        if (clave == null) {
            return null;
        }
        cambio = false;
        valorQuitado = null;
        raiz = eliminar(raiz, clave);
        if (cambio) {
            tamano--;
        }
        V resultado = valorQuitado;
        valorQuitado = null;
        return resultado;
    }

    /**
     * Quita la clave mayor y devuelve su valor. O(log n).
     *
     * @return el valor de la clave máxima
     * @throws IllegalStateException si el árbol está vacío
     */
    public V extraerMaximo() {
        if (raiz == null) {
            throw new IllegalStateException("El árbol está vacío");
        }
        raiz = eliminarMaximo(raiz);
        tamano--;
        V resultado = valorQuitado;
        valorQuitado = null;
        return resultado;
    }

    /**
     * Quita la clave menor y devuelve su valor. O(log n).
     *
     * @return el valor de la clave mínima
     * @throws IllegalStateException si el árbol está vacío
     */
    public V extraerMinimo() {
        if (raiz == null) {
            throw new IllegalStateException("El árbol está vacío");
        }
        Nodo<K, V> minimo = minimo(raiz);
        valorQuitado = minimo.valor;
        raiz = eliminarMinimo(raiz);
        tamano--;
        V resultado = valorQuitado;
        valorQuitado = null;
        return resultado;
    }

    // ------------------------------------------------------------------ recorridos

    /**
     * Agrega las claves en orden ascendente al final de una lista. O(n),
     * recorrido en orden con una pila propia (sin recursión).
     *
     * @param destino lista donde se agregan las claves
     */
    public void clavesEnOrden(ListaEnlazada<K> destino) {
        Pila<Nodo<K, V>> pila = new Pila<>(Math.max(16, altura() + 1));
        Nodo<K, V> actual = raiz;
        while (actual != null || !pila.estaVacia()) {
            while (actual != null) {
                pila.apilar(actual);
                actual = actual.izquierdo;
            }
            actual = pila.desapilar();
            destino.agregarAlFinal(actual.clave);
            actual = actual.derecho;
        }
    }

    /**
     * Agrega los valores en el orden ascendente de sus claves. O(n).
     *
     * @param destino lista donde se agregan los valores
     */
    public void valoresEnOrden(ListaEnlazada<V> destino) {
        Pila<Nodo<K, V>> pila = new Pila<>(Math.max(16, altura() + 1));
        Nodo<K, V> actual = raiz;
        while (actual != null || !pila.estaVacia()) {
            while (actual != null) {
                pila.apilar(actual);
                actual = actual.izquierdo;
            }
            actual = pila.desapilar();
            destino.agregarAlFinal(actual.valor);
            actual = actual.derecho;
        }
    }

    /**
     * Comprueba las tres propiedades del AVL: orden de las claves, alturas
     * guardadas correctas y factor de balance entre −1 y 1. O(n). Se usa en
     * las pruebas.
     *
     * @return {@code true} si el árbol es un AVL válido
     */
    public boolean esValido() {
        ListaEnlazada<K> claves = new ListaEnlazada<>();
        clavesEnOrden(claves);
        K anterior = null;
        for (ListaEnlazada.Nodo<K> n = claves.primero(); n != null; n = n.siguiente()) {
            if (anterior != null && anterior.compareTo(n.dato()) >= 0) {
                return false;
            }
            anterior = n.dato();
        }
        return claves.tamano() == tamano && alturaVerificada(raiz) >= 0;
    }

    // ------------------------------------------------------------------ internos

    private Nodo<K, V> nodoDe(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int c = clave.compareTo(actual.clave);
            if (c == 0) {
                return actual;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    private Nodo<K, V> nodoMaximo() {
        if (raiz == null) {
            throw new IllegalStateException("El árbol está vacío");
        }
        Nodo<K, V> actual = raiz;
        while (actual.derecho != null) {
            actual = actual.derecho;
        }
        return actual;
    }

    private Nodo<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            cambio = true;
            return new Nodo<>(clave, valor);
        }
        int c = clave.compareTo(nodo.clave);
        if (c < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, clave, valor);
        } else if (c > 0) {
            nodo.derecho = insertar(nodo.derecho, clave, valor);
        } else {
            return nodo; // clave repetida: el árbol no cambia
        }
        return balancear(nodo);
    }

    private Nodo<K, V> eliminar(Nodo<K, V> nodo, K clave) {
        if (nodo == null) {
            return null;
        }
        int c = clave.compareTo(nodo.clave);
        if (c < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, clave);
        } else if (c > 0) {
            nodo.derecho = eliminar(nodo.derecho, clave);
        } else {
            cambio = true;
            valorQuitado = nodo.valor;
            if (nodo.izquierdo == null) {
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                return nodo.izquierdo;
            }
            // Dos hijos: el sucesor (mínimo del subárbol derecho) ocupa su lugar.
            Nodo<K, V> sucesor = minimo(nodo.derecho);
            Nodo<K, V> derechoSinSucesor = eliminarMinimo(nodo.derecho);
            sucesor.izquierdo = nodo.izquierdo;
            sucesor.derecho = derechoSinSucesor;
            nodo = sucesor;
        }
        return balancear(nodo);
    }

    private Nodo<K, V> eliminarMaximo(Nodo<K, V> nodo) {
        if (nodo.derecho == null) {
            valorQuitado = nodo.valor;
            return nodo.izquierdo;
        }
        nodo.derecho = eliminarMaximo(nodo.derecho);
        return balancear(nodo);
    }

    private Nodo<K, V> eliminarMinimo(Nodo<K, V> nodo) {
        if (nodo.izquierdo == null) {
            return nodo.derecho;
        }
        nodo.izquierdo = eliminarMinimo(nodo.izquierdo);
        return balancear(nodo);
    }

    private Nodo<K, V> minimo(Nodo<K, V> nodo) {
        while (nodo.izquierdo != null) {
            nodo = nodo.izquierdo;
        }
        return nodo;
    }

    private int altura(Nodo<K, V> nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    private void actualizarAltura(Nodo<K, V> nodo) {
        nodo.altura = 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
    }

    private int factorBalance(Nodo<K, V> nodo) {
        return altura(nodo.izquierdo) - altura(nodo.derecho);
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        rotaciones++;
        return x;
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> x) {
        Nodo<K, V> y = x.derecho;
        x.derecho = y.izquierdo;
        y.izquierdo = x;
        actualizarAltura(x);
        actualizarAltura(y);
        rotaciones++;
        return y;
    }

    /** Recalcula la altura y aplica la rotación simple o doble que haga falta. O(1). */
    private Nodo<K, V> balancear(Nodo<K, V> nodo) {
        actualizarAltura(nodo);
        int factor = factorBalance(nodo);
        if (factor > 1) {
            if (factorBalance(nodo.izquierdo) < 0) {
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo); // caso izquierda-derecha
            }
            return rotarDerecha(nodo);
        }
        if (factor < -1) {
            if (factorBalance(nodo.derecho) > 0) {
                nodo.derecho = rotarDerecha(nodo.derecho); // caso derecha-izquierda
            }
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    /** @return la altura real del subárbol, o −1 si alguna altura o balance falla. */
    private int alturaVerificada(Nodo<K, V> nodo) {
        if (nodo == null) {
            return 0;
        }
        int izquierda = alturaVerificada(nodo.izquierdo);
        int derecha = alturaVerificada(nodo.derecho);
        if (izquierda < 0 || derecha < 0 || Math.abs(izquierda - derecha) > 1) {
            return -1;
        }
        int real = 1 + Math.max(izquierda, derecha);
        return real == nodo.altura ? real : -1;
    }
}
