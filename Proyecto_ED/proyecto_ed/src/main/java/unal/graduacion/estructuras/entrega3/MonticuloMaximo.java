package unal.graduacion.estructuras.entrega3;

/**
 * PLANTILLA - Entrega 3.
 *
 * Montículo (heap) binario de máximos sobre arreglo dinámico.
 * Reemplazará al AVL de prioridad del planificador: solo se necesita
 * extraer el máximo, no mantener el orden total. Misma complejidad
 * O(log n) pero con menor constante (arreglo contiguo, sin rotaciones).
 *
 * Operaciones previstas: insertar (subir), extraerMaximo (bajar), verMaximo,
 * construir desde arreglo en O(n) (heapify).
 */
public class MonticuloMaximo<T extends Comparable<T>> {

    // TODO: arreglo de elementos y tamaño.

    public void insertar(T elemento) {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }

    public T extraerMaximo() {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }

    public boolean estaVacio() {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }
}
