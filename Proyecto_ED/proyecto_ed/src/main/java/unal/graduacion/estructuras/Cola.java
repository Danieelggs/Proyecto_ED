package unal.graduacion.estructuras;

import java.util.NoSuchElementException;

/**
 * Cola FIFO sobre arreglo dinámico circular.
 *
 * Uso en el proyecto: algoritmo de Kahn (orden topológico). Las asignaturas
 * cuyo grado de entrada llega a 0 se encolan; si al final no se procesaron
 * todas, el plan tiene un ciclo de prerrequisitos.
 *
 * Complejidades: encolar O(1) amortizado, desencolar O(1), frente O(1).
 */
public class Cola<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private Object[] datos;
    private int inicio;
    private int tamano;

    public Cola() {
        datos = new Object[CAPACIDAD_INICIAL];
    }

    public void encolar(T dato) {
        if (tamano == datos.length) {
            redimensionar(datos.length * 2);
        }
        datos[(inicio + tamano) % datos.length] = dato;
        tamano++;
    }

    @SuppressWarnings("unchecked")
    public T desencolar() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola está vacía");
        }
        T dato = (T) datos[inicio];
        datos[inicio] = null;
        inicio = (inicio + 1) % datos.length;
        tamano--;
        return dato;
    }

    @SuppressWarnings("unchecked")
    public T frente() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola está vacía");
        }
        return (T) datos[inicio];
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }

    private void redimensionar(int nuevaCapacidad) {
        Object[] nuevo = new Object[nuevaCapacidad];
        for (int i = 0; i < tamano; i++) {
            nuevo[i] = datos[(inicio + i) % datos.length];
        }
        datos = nuevo;
        inicio = 0;
    }
}
