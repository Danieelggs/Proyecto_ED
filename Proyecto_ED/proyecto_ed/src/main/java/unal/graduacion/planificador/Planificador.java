package unal.graduacion.planificador;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Planificador del mínimo número de semestres (versión entrega 2: estructuras
 * vistas hasta AVL).
 *
 * Flujo:
 *  1. Kahn con {@link Cola}: valida que no haya ciclos y produce un orden
 *     topológico.                                                    O(n + e)
 *  2. {@link Pila} con ese orden: al desapilar (orden inverso) se calcula la
 *     altura de cada asignatura = 1 + máx. altura de sus dependientes. O(n + e)
 *  3. Semestre a semestre se extraen del {@link ArbolAVL} de disponibles las
 *     k asignaturas de mayor altura (algoritmo de Hu / lista con prioridad).
 *     Sus dependientes que quedan sin prerrequisitos pendientes entran al
 *     AVL para el semestre siguiente.                               O(n log n + e)
 *
 * Con límite k el problema exacto es NP-difícil en general, por eso se reporta
 * la cota inferior LB = max(ceil(n/k), cadena más larga): si el plan usa LB
 * semestres, es óptimo con certeza. La versión exacta para planes pequeños
 * (búsqueda sobre conjuntos) se implementará en la entrega 3.
 */
public class Planificador {

    private final RegistroAsignaturas registro;

    public Planificador(RegistroAsignaturas registro) {
        this.registro = registro;
    }

    public ResultadoPlan planificar(int limitePorSemestre) {
        if (limitePorSemestre < 1) {
            throw new IllegalArgumentException("El límite por semestre debe ser al menos 1");
        }
        int n = registro.tamano();
        Pila<Asignatura> ordenTopologico = ordenTopologico(n);
        int[] altura = calcularAlturas(ordenTopologico, n);

        int cadenaMasLarga = 0;
        for (int h : altura) {
            cadenaMasLarga = Math.max(cadenaMasLarga, h);
        }
        int cotaInferior = Math.max((n + limitePorSemestre - 1) / limitePorSemestre, cadenaMasLarga);

        return repartirPorSemestres(limitePorSemestre, altura, cotaInferior);
    }

    /** Paso 1: algoritmo de Kahn. Lanza excepción si hay ciclo. */
    private Pila<Asignatura> ordenTopologico(int n) {
        int[] gradoEntrada = new int[n];
        Cola<Asignatura> cola = new Cola<>();
        for (Asignatura a : registro.todas()) {
            gradoEntrada[a.getIndice()] = a.getPrerrequisitos().tamano();
            if (gradoEntrada[a.getIndice()] == 0) {
                cola.encolar(a);
            }
        }

        Pila<Asignatura> orden = new Pila<>();
        while (!cola.estaVacia()) {
            Asignatura actual = cola.desencolar();
            orden.apilar(actual);
            for (Asignatura dep : actual.getDependientes()) {
                if (--gradoEntrada[dep.getIndice()] == 0) {
                    cola.encolar(dep);
                }
            }
        }

        if (orden.tamano() < n) {
            ListaEnlazada<Asignatura> bloqueadas = new ListaEnlazada<>();
            for (Asignatura a : registro.todas()) {
                if (gradoEntrada[a.getIndice()] > 0) {
                    bloqueadas.agregarFinal(a);
                }
            }
            throw new CicloPrerrequisitosException(bloqueadas);
        }
        return orden;
    }

    /** Paso 2: alturas recorriendo el orden topológico al revés. */
    private int[] calcularAlturas(Pila<Asignatura> ordenTopologico, int n) {
        int[] altura = new int[n];
        while (!ordenTopologico.estaVacia()) {
            Asignatura a = ordenTopologico.desapilar();
            int maxDependiente = 0;
            for (Asignatura dep : a.getDependientes()) {
                maxDependiente = Math.max(maxDependiente, altura[dep.getIndice()]);
            }
            altura[a.getIndice()] = 1 + maxDependiente;
        }
        return altura;
    }

    /** Paso 3: llenar cada semestre con las k disponibles de mayor altura. */
    private ResultadoPlan repartirPorSemestres(int k, int[] altura, int cotaInferior) {
        int[] pendientes = new int[registro.tamano()];
        ArbolAVL<Prioridad, Asignatura> disponibles = new ArbolAVL<>();
        for (Asignatura a : registro.todas()) {
            pendientes[a.getIndice()] = a.getPrerrequisitos().tamano();
            if (pendientes[a.getIndice()] == 0) {
                disponibles.insertar(new Prioridad(altura[a.getIndice()], a.getId()), a);
            }
        }

        ListaEnlazada<ListaEnlazada<Asignatura>> semestres = new ListaEnlazada<>();
        while (!disponibles.estaVacio()) {
            ListaEnlazada<Asignatura> semestre = new ListaEnlazada<>();
            while (semestre.tamano() < k && !disponibles.estaVacio()) {
                semestre.agregarFinal(disponibles.extraerMaximo());
            }
            // Los dependientes liberados entran DESPUÉS de cerrar el semestre:
            // un prerrequisito nunca comparte semestre con su dependiente.
            for (Asignatura cursada : semestre) {
                for (Asignatura dep : cursada.getDependientes()) {
                    if (--pendientes[dep.getIndice()] == 0) {
                        disponibles.insertar(new Prioridad(altura[dep.getIndice()], dep.getId()), dep);
                    }
                }
            }
            semestres.agregarFinal(semestre);
        }
        return new ResultadoPlan(semestres, cotaInferior, k);
    }
}
