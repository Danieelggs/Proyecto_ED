package unal.graduacion.estructuras.entrega3;

/**
 * PLANTILLA - Entrega 3.
 *
 * Tabla hash ID -> valor con encadenamiento separado.
 * Reemplazará al AVL del registro de asignaturas: el acceso por ID no
 * necesita orden entre IDs, así que buscar/insertar pasan de O(log n) a
 * O(1) promedio.
 *
 * Decisiones previstas:
 *  - Función hash: id mod capacidad, con capacidad potencia de 2 y mezcla de bits.
 *  - Factor de carga máximo 0.75; al superarlo se duplica la capacidad (rehash O(n)).
 *  - Cubetas: ListaEnlazada propia del proyecto.
 */
public class TablaHash<V> {

    // TODO: arreglo de cubetas, tamaño y factor de carga.

    public void insertar(int clave, V valor) {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }

    public V buscar(int clave) {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }

    public int tamano() {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }
}
