package unal.graduacion.estructuras;

/**
 * Pila LIFO sobre un arreglo que crece cuando se llena.
 *
 * <p>Kahn apila cada asignatura en orden topológico; al desapilar salen en el
 * orden inverso, que es justo el que necesita el cálculo de alturas: la altura
 * de una asignatura depende de las de sus dependientes, y estas ya salieron.</p>
 *
 * @param <T> tipo de los elementos
 */
public class Pila<T> {

    private Object[] datos;
    private int tamano;

    /** Crea una pila con capacidad inicial 16. */
    public Pila() {
        this(16);
    }

    /**
     * Crea una pila con la capacidad indicada.
     *
     * @param capacidadInicial capacidad del arreglo (mínimo 1)
     */
    public Pila(int capacidadInicial) {
        datos = new Object[Math.max(1, capacidadInicial)];
    }

    /**
     * Pone un elemento en el tope. O(1) amortizado.
     *
     * @param dato elemento a apilar
     */
    public void apilar(T dato) {
        if (tamano == datos.length) {
            Object[] nuevo = new Object[datos.length * 2];
            for (int i = 0; i < tamano; i++) {
                nuevo[i] = datos[i];
            }
            datos = nuevo;
        }
        datos[tamano++] = dato;
    }

    /**
     * Quita y devuelve el elemento del tope. O(1).
     *
     * @return el último elemento apilado
     * @throws IllegalStateException si la pila está vacía
     */
    @SuppressWarnings("unchecked")
    public T desapilar() {
        if (tamano == 0) {
            throw new IllegalStateException("La pila está vacía");
        }
        T dato = (T) datos[--tamano];
        datos[tamano] = null;
        return dato;
    }

    /**
     * Devuelve el elemento del tope sin quitarlo. O(1).
     *
     * @return el último elemento apilado
     * @throws IllegalStateException si la pila está vacía
     */
    @SuppressWarnings("unchecked")
    public T verTope() {
        if (tamano == 0) {
            throw new IllegalStateException("La pila está vacía");
        }
        return (T) datos[tamano - 1];
    }

    /** @return cantidad de elementos. O(1). */
    public int tamano() {
        return tamano;
    }

    /** @return {@code true} si no tiene elementos. O(1). */
    public boolean estaVacia() {
        return tamano == 0;
    }
}
