package unal.graduacion.medicion;

import java.util.Random;

import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Genera planes de estudio sintéticos (DAG aleatorios) para medir tiempos
 * de ejecución en las entregas 2 y 3 (RF7).
 *
 * Para garantizar que no haya ciclos, cada asignatura i solo puede tener
 * prerrequisitos con índice menor que i. Los IDs se generan ordenados a
 * propósito: es el peor caso de un ABB sin balancear y muestra la ventaja
 * del AVL.
 */
public final class GeneradorPlanes {

    private GeneradorPlanes() { }

    /**
     * @param n                número de asignaturas
     * @param maxPrerrequisitos prerrequisitos máximos por asignatura
     * @param semilla          semilla fija para que las mediciones sean reproducibles
     */
    public static RegistroAsignaturas generar(int n, int maxPrerrequisitos, long semilla) {
        Random azar = new Random(semilla);
        RegistroAsignaturas registro = new RegistroAsignaturas();
        Asignatura[] creadas = new Asignatura[n];
        for (int i = 0; i < n; i++) {
            creadas[i] = new Asignatura(1000 + i, "Asignatura " + i);
            registro.registrar(creadas[i]);
            if (i > 0) {
                int cantidad = azar.nextInt(Math.min(maxPrerrequisitos, i) + 1);
                for (int j = 0; j < cantidad; j++) {
                    creadas[i].agregarPrerrequisito(creadas[azar.nextInt(i)]);
                }
            }
        }
        return registro;
    }

    // TODO (entrega 2): clase MedidorTiempos que, para n = 10^2 ... 10^6,
    //   1. genere el plan con generar(n, 4, semilla),
    //   2. mida con System.nanoTime() la carga, la validación y la planificación
    //      por separado (varias repeticiones, descartando el calentamiento de la JVM),
    //   3. escriba resultados/tiempos_entrega2.csv con columnas n,fase,nanosegundos.
    // TODO (entrega 3): repetir con la versión hash + heap + grafo y comparar.
}
