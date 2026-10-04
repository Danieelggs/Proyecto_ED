package unal.graduacion.estructuras.entrega3;

/**
 * PLANTILLA - Entrega 3.
 *
 * Grafo dirigido con listas de adyacencia: modelo explícito del plan.
 * Arista u -> v significa "u es prerrequisito de v". Un plan válido es un
 * DAG; no es un árbol porque una asignatura puede tener varios
 * prerrequisitos (varios padres).
 *
 * Operaciones previstas:
 *  - agregarVertice / agregarArista                         O(1)
 *  - tieneCiclo(): DFS con tres colores (blanco/gris/negro)   O(n + e)
 *  - cicloEncontrado(): devuelve el ciclo exacto para reportarlo
 *  - ordenTopologico()                                        O(n + e)
 *
 * También se implementará PlanificadorExacto: búsqueda por niveles sobre
 * conjuntos de asignaturas aprobadas (representados como bits) que da el
 * mínimo garantizado para n <= 20 y sirve para validar la heurística.
 */
public class GrafoDirigido {

    // TODO: vértices indexados 0..n-1 y listas de adyacencia.

    public boolean tieneCiclo() {
        throw new UnsupportedOperationException("Pendiente para la entrega 3");
    }
}
