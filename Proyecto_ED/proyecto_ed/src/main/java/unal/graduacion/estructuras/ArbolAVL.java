package unal.graduacion.estructuras;

import java.util.NoSuchElementException;

/**
 * Árbol AVL (árbol binario de búsqueda autobalanceado) clave → valor.
 *
 * Se usa con dos claves distintas en el proyecto:
 *  1. Registro de asignaturas: clave = ID numérico. Los catálogos suelen
 *     venir ordenados por código; un ABB simple degeneraría en una lista
 *     (O(n) por búsqueda). El AVL garantiza altura O(log n).
 *  2. Asignaturas disponibles por prioridad: clave = (altura, ID). Cada
 *     semestre se extraen las k de mayor prioridad con extraerMaximo().
 *
 * Complejidades: insertar, buscar, eliminar y extraerMaximo en O(log n).
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private static class Nodo<K, V> {
        K clave;
        V valor;
        int altura = 1;
        Nodo<K, V> izq;
        Nodo<K, V> der;

        Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;

    // ------------------------------------------------------------------ API

    /** Inserta o reemplaza el valor asociado a la clave. */
    public void insertar(K clave, V valor) {
        if (clave == null) {
            throw new IllegalArgumentException("La clave no puede ser null");
        }
        raiz = insertar(raiz, clave, valor);
    }

    /** Devuelve el valor asociado a la clave o null si no existe. */
    public V buscar(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int cmp = clave.compareTo(actual.clave);
            if (cmp == 0) {
                return actual.valor;
            }
            actual = cmp < 0 ? actual.izq : actual.der;
        }
        return null;
    }

    public boolean contiene(K clave) {
        return buscar(clave) != null;
    }

    /** Elimina la clave si existe. */
    public void eliminar(K clave) {
        raiz = eliminar(raiz, clave);
    }

    /** Elimina y devuelve el valor con la clave máxima. */
    public V extraerMaximo() {
        if (raiz == null) {
            throw new NoSuchElementException("El árbol está vacío");
        }
        Nodo<K, V> max = raiz;
        while (max.der != null) {
            max = max.der;
        }
        V valor = max.valor;
        eliminar(max.clave);
        return valor;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    /** Altura del árbol (0 si está vacío). Útil para verificar el balanceo. */
    public int altura() {
        return altura(raiz);
    }

    /** Valores en orden ascendente de clave, O(n). */
    public ListaEnlazada<V> valoresEnOrden() {
        ListaEnlazada<V> lista = new ListaEnlazada<>();
        enOrden(raiz, lista);
        return lista;
    }

    // -------------------------------------------------------- recursividad

    private Nodo<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            tamano++;
            return new Nodo<>(clave, valor);
        }
        int cmp = clave.compareTo(nodo.clave);
        if (cmp < 0) {
            nodo.izq = insertar(nodo.izq, clave, valor);
        } else if (cmp > 0) {
            nodo.der = insertar(nodo.der, clave, valor);
        } else {
            nodo.valor = valor;
            return nodo;
        }
        return balancear(nodo);
    }

    private Nodo<K, V> eliminar(Nodo<K, V> nodo, K clave) {
        if (nodo == null) {
            return null;
        }
        int cmp = clave.compareTo(nodo.clave);
        if (cmp < 0) {
            nodo.izq = eliminar(nodo.izq, clave);
        } else if (cmp > 0) {
            nodo.der = eliminar(nodo.der, clave);
        } else {
            if (nodo.izq == null || nodo.der == null) {
                tamano--;
                return nodo.izq != null ? nodo.izq : nodo.der;
            }
            // Dos hijos: se reemplaza por el sucesor (mínimo del subárbol derecho).
            Nodo<K, V> sucesor = nodo.der;
            while (sucesor.izq != null) {
                sucesor = sucesor.izq;
            }
            nodo.clave = sucesor.clave;
            nodo.valor = sucesor.valor;
            nodo.der = eliminar(nodo.der, sucesor.clave);
        }
        return balancear(nodo);
    }

    private void enOrden(Nodo<K, V> nodo, ListaEnlazada<V> lista) {
        if (nodo == null) {
            return;
        }
        enOrden(nodo.izq, lista);
        lista.agregarFinal(nodo.valor);
        enOrden(nodo.der, lista);
    }

    // ------------------------------------------------------------ balanceo

    private int altura(Nodo<K, V> n) {
        return n == null ? 0 : n.altura;
    }

    private int factorBalance(Nodo<K, V> n) {
        return altura(n.izq) - altura(n.der);
    }

    private void actualizarAltura(Nodo<K, V> n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> x) {
        Nodo<K, V> y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    private Nodo<K, V> balancear(Nodo<K, V> n) {
        actualizarAltura(n);
        int fb = factorBalance(n);
        if (fb > 1) {
            if (factorBalance(n.izq) < 0) {
                n.izq = rotarIzquierda(n.izq); // caso izquierda-derecha
            }
            return rotarDerecha(n);            // caso izquierda-izquierda
        }
        if (fb < -1) {
            if (factorBalance(n.der) > 0) {
                n.der = rotarDerecha(n.der);   // caso derecha-izquierda
            }
            return rotarIzquierda(n);          // caso derecha-derecha
        }
        return n;
    }
}
