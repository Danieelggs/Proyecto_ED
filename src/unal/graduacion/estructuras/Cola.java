package unal.graduacion.estructuras;

/**
 * Cola FIFO sobre un arreglo circular.
 *
 * <p>La usa el algoritmo de Kahn: procesa las asignaturas en el orden en que
 * quedan disponibles. Si se crea con capacidad n (el número de asignaturas),
 * nunca necesita crecer, porque cada asignatura entra a la cola una sola vez.</p>
 *
 * @param <T> tipo de los elementos
 */
public class Cola<T> {

    private Object[] datos;
    private int frente;
    private int tamano;

    /** Crea una cola con capacidad inicial 16. */
    public Cola() {
        this(16);
    }

    /**
     * Crea una cola con la capacidad indicada.
     *
     * @param capacidadInicial capacidad del arreglo (mínimo 1)
     */
    public Cola(int capacidadInicial) {
        datos = new Object[Math.max(1, capacidadInicial)];
    }

    /**
     * Agrega un elemento al final. O(1); si el arreglo está lleno, duplica su
     * capacidad, lo que cuesta O(n) esa vez y O(1) amortizado.
     *
     * @param dato elemento a encolar
     */
    public void encolar(T dato) {
        if (tamano == datos.length) {
            crecer();
        }
        int fin = (frente + tamano) % datos.length;
        datos[fin] = dato;
        tamano++;
    }

    /**
     * Quita y devuelve el elemento del frente. O(1).
     *
     * @return el elemento más antiguo
     * @throws IllegalStateException si la cola está vacía
     */
    @SuppressWarnings("unchecked")
    public T desencolar() {
        if (tamano == 0) {
            throw new IllegalStateException("La cola está vacía");
        }
        T dato = (T) datos[frente];
        datos[frente] = null;
        frente = (frente + 1) % datos.length;
        tamano--;
        return dato;
    }

    /**
     * Devuelve el elemento del frente sin quitarlo. O(1).
     *
     * @return el elemento más antiguo
     * @throws IllegalStateException si la cola está vacía
     */
    @SuppressWarnings("unchecked")
    public T verFrente() {
        if (tamano == 0) {
            throw new IllegalStateException("La cola está vacía");
        }
        return (T) datos[frente];
    }

    /** @return cantidad de elementos. O(1). */
    public int tamano() {
        return tamano;
    }

    /** @return {@code true} si no tiene elementos. O(1). */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /** @return tamaño actual del arreglo interno. O(1). */
    public int capacidad() {
        return datos.length;
    }

    private void crecer() {
        Object[] nuevo = new Object[datos.length * 2];
        for (int i = 0; i < tamano; i++) {
            nuevo[i] = datos[(frente + i) % datos.length];
        }
        datos = nuevo;
        frente = 0;
    }
}
