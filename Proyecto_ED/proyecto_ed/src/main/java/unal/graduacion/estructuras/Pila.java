package unal.graduacion.estructuras;

import java.util.NoSuchElementException;

/**
 * Pila LIFO sobre arreglo dinámico.
 *
 * Uso en el proyecto: guarda el orden topológico producido por Kahn. Al
 * desapilar se recorre en orden inverso, de modo que cada asignatura se
 * procesa después de todas sus dependientes; así se calcula su altura
 * (longitud de la cadena más larga que abre), que es su prioridad.
 *
 * Complejidades: apilar O(1) amortizado, desapilar O(1), cima O(1).
 */
public class Pila<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private Object[] datos;
    private int tamano;

    public Pila() {
        datos = new Object[CAPACIDAD_INICIAL];
    }

    public void apilar(T dato) {
        if (tamano == datos.length) {
            Object[] nuevo = new Object[datos.length * 2];
            System.arraycopy(datos, 0, nuevo, 0, tamano);
            datos = nuevo;
        }
        datos[tamano++] = dato;
    }

    @SuppressWarnings("unchecked")
    public T desapilar() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila está vacía");
        }
        T dato = (T) datos[--tamano];
        datos[tamano] = null;
        return dato;
    }

    @SuppressWarnings("unchecked")
    public T cima() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila está vacía");
        }
        return (T) datos[tamano - 1];
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }
}
