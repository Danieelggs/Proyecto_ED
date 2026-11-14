package unal.graduacion.planificador;

import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Detecta ciclos de prerrequisitos con el algoritmo de Kahn (RF3).
 *
 * <p>Una cola guarda las asignaturas cuyo grado de entrada ya llegó a 0. Cada
 * vez que se procesa una, se descuenta un grado a cada dependiente. Si al
 * final quedan asignaturas sin procesar, hay un ciclo y esas asignaturas
 * están bloqueadas (están en el ciclo o dependen de él). Costo: O(n + e).</p>
 *
 * <p>Además, cada asignatura procesada se apila: la pila queda en orden
 * topológico y el cálculo de alturas la desapila en orden inverso.</p>
 */
public final class ValidadorCiclos {

    /** Resultado de la validación. */
    public static final class Resultado {
        private final boolean aciclico;
        private final Pila<Asignatura> ordenTopologico;
        private final int[] nivel;
        private final ListaEnlazada<Asignatura> bloqueadas;
        private final ListaEnlazada<Asignatura> ciclo;

        private Resultado(boolean aciclico, Pila<Asignatura> ordenTopologico, int[] nivel,
                          ListaEnlazada<Asignatura> bloqueadas, ListaEnlazada<Asignatura> ciclo) {
            this.aciclico = aciclico;
            this.ordenTopologico = ordenTopologico;
            this.nivel = nivel;
            this.bloqueadas = bloqueadas;
            this.ciclo = ciclo;
        }

        /** @return {@code true} si el plan no tiene ciclos */
        public boolean esAciclico() {
            return aciclico;
        }

        /** @return pila con las asignaturas en orden topológico (el tope es la última). Se consume al calcular alturas. */
        public Pila<Asignatura> ordenTopologico() {
            return ordenTopologico;
        }

        /**
         * Nivel de cada asignatura (por índice interno): el semestre más
         * temprano en que podría cursarse si no hubiera límite k.
         *
         * @return arreglo de niveles; 0 para las bloqueadas
         */
        public int[] nivel() {
            return nivel;
        }

        /** @return asignaturas en un ciclo o que dependen de uno (vacía si el plan es válido) */
        public ListaEnlazada<Asignatura> bloqueadas() {
            return bloqueadas;
        }

        /**
         * Un ciclo concreto, en el sentido de las flechas: cada asignatura es
         * prerrequisito de la siguiente y la última lo es de la primera.
         *
         * @return asignaturas del ciclo (vacía si el plan es válido)
         */
        public ListaEnlazada<Asignatura> ciclo() {
            return ciclo;
        }
    }

    private ValidadorCiclos() {
    }

    /**
     * Valida el plan. O(n + e).
     *
     * @param registro plan cargado
     * @return resultado con el orden topológico o con el ciclo encontrado
     */
    public static Resultado validar(RegistroAsignaturas registro) {
        int n = registro.cantidad();
        int[] grado = new int[n];
        int[] nivel = new int[n];
        Cola<Asignatura> cola = new Cola<>(Math.max(1, n));
        Pila<Asignatura> orden = new Pila<>(Math.max(1, n));

        for (int i = 0; i < n; i++) {
            Asignatura asignatura = registro.obtener(i);
            grado[i] = asignatura.gradoEntrada();
            if (grado[i] == 0) {
                nivel[i] = 1;
                cola.encolar(asignatura);
            }
        }

        int procesadas = 0;
        while (!cola.estaVacia()) {
            Asignatura actual = cola.desencolar();
            orden.apilar(actual);
            procesadas++;
            int nivelSiguiente = nivel[actual.indice()] + 1;
            for (ListaEnlazada.Nodo<Asignatura> nodo = actual.dependientes().primero(); nodo != null; nodo = nodo.siguiente()) {
                int d = nodo.dato().indice();
                if (nivelSiguiente > nivel[d]) {
                    nivel[d] = nivelSiguiente;
                }
                grado[d]--;
                if (grado[d] == 0) {
                    cola.encolar(nodo.dato());
                }
            }
        }

        ListaEnlazada<Asignatura> bloqueadas = new ListaEnlazada<>();
        ListaEnlazada<Asignatura> ciclo = new ListaEnlazada<>();
        if (procesadas < n) {
            for (int i = 0; i < n; i++) {
                if (grado[i] > 0) {
                    bloqueadas.agregarAlFinal(registro.obtener(i));
                    nivel[i] = 0;
                }
            }
            encontrarCiclo(registro, grado, bloqueadas.obtenerPrimero(), ciclo);
        }
        return new Resultado(procesadas == n, orden, nivel, bloqueadas, ciclo);
    }

    /**
     * Toda asignatura bloqueada tiene al menos un prerrequisito bloqueado (si
     * no, su grado habría llegado a 0). Retrocediendo de bloqueada en
     * bloqueada, a lo sumo en n pasos se repite una: ese tramo es un ciclo.
     * O(n + e).
     */
    private static void encontrarCiclo(RegistroAsignaturas registro, int[] grado, Asignatura inicio,
                                       ListaEnlazada<Asignatura> ciclo) {
        int n = registro.cantidad();
        int[] posicion = new int[n];
        for (int i = 0; i < n; i++) {
            posicion[i] = -1;
        }
        Asignatura[] camino = new Asignatura[n];
        int largo = 0;
        Asignatura actual = inicio;
        while (posicion[actual.indice()] < 0) {
            posicion[actual.indice()] = largo;
            camino[largo++] = actual;
            Asignatura anterior = null;
            for (ListaEnlazada.Nodo<Asignatura> nodo = actual.prerrequisitos().primero(); nodo != null; nodo = nodo.siguiente()) {
                if (grado[nodo.dato().indice()] > 0) {
                    anterior = nodo.dato();
                    break;
                }
            }
            actual = anterior;
        }
        // El camino va de dependiente a prerrequisito; se invierte para seguir las flechas.
        for (int i = largo - 1; i >= posicion[actual.indice()]; i--) {
            ciclo.agregarAlFinal(camino[i]);
        }
    }
}
