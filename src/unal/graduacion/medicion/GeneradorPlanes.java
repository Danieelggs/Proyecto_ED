package unal.graduacion.medicion;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Genera planes de estudio aleatorios para las pruebas y las mediciones
 * (aclaración 3.1 del enunciado).
 *
 * <ul>
 *   <li><b>Sin ciclos por construcción</b>: la asignatura i solo toma
 *       prerrequisitos entre las generadas antes que ella.</li>
 *   <li><b>IDs únicos, dispersos y desordenados</b>: se baraja un arreglo con
 *       Fisher–Yates (sin {@code HashSet}) y cada posición se separa por 10.</li>
 *   <li><b>e proporcional a n</b>: cada asignatura tiene de 0 a
 *       {@code maxPrerrequisitos} prerrequisitos, elegidos entre las
 *       {@code ventana} asignaturas anteriores.</li>
 * </ul>
 *
 * <p>Generar no se mide: los datos quedan en arreglos ({@link PlanCrudo}) y
 * luego {@link #registrar(PlanCrudo)} los carga en las estructuras, que es lo
 * que sí se cronometra.</p>
 */
public final class GeneradorPlanes {

    /** Plan generado, todavía en arreglos. */
    public static final class PlanCrudo {
        private final int n;
        private final int k;
        private final long[] ids;
        private final int[] creditos;
        private final int[] inicio;
        private final int[] prerrequisitos;
        private final int aristas;

        private PlanCrudo(int n, int k, long[] ids, int[] creditos, int[] inicio, int[] prerrequisitos, int aristas) {
            this.n = n;
            this.k = k;
            this.ids = ids;
            this.creditos = creditos;
            this.inicio = inicio;
            this.prerrequisitos = prerrequisitos;
            this.aristas = aristas;
        }

        /** @return número de asignaturas */
        public int n() {
            return n;
        }

        /** @return k del plan */
        public int k() {
            return k;
        }

        /** @return número de prerrequisitos (aristas) */
        public int aristas() {
            return aristas;
        }

        /** @return IDs en orden de generación (desordenados numéricamente) */
        public long[] ids() {
            return ids;
        }
    }

    private GeneradorPlanes() {
    }

    /**
     * Genera un plan aleatorio. O(n + e).
     *
     * @param n                 número de asignaturas
     * @param k                 máximo de asignaturas por semestre
     * @param maxPrerrequisitos máximo de prerrequisitos por asignatura
     * @param ventana           los prerrequisitos se eligen entre las {@code ventana} anteriores
     * @param semilla           semilla del generador
     * @return el plan en arreglos
     */
    public static PlanCrudo generar(int n, int k, int maxPrerrequisitos, int ventana, long semilla) {
        GeneradorAleatorio azar = new GeneradorAleatorio(semilla);

        int[] permutacion = new int[n];
        for (int i = 0; i < n; i++) {
            permutacion[i] = i;
        }
        for (int i = n - 1; i > 0; i--) { // Fisher–Yates
            int j = azar.entero(i + 1);
            int temporal = permutacion[i];
            permutacion[i] = permutacion[j];
            permutacion[j] = temporal;
        }
        long[] ids = new long[n];
        int[] creditos = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = 1_000_000L + permutacion[i] * 10L + azar.entero(10);
            creditos[i] = 2 + azar.entero(3);
        }

        int[] inicio = new int[n + 1];
        int[] prerrequisitos = new int[Math.max(1, n * maxPrerrequisitos)];
        int[] elegidos = new int[Math.max(1, maxPrerrequisitos)];
        int aristas = 0;
        for (int i = 0; i < n; i++) {
            inicio[i] = aristas;
            int desde = Math.max(0, i - ventana);
            int candidatas = i - desde;
            int cuantos = Math.min(azar.entero(maxPrerrequisitos + 1), candidatas);
            int elegidosAhora = 0;
            while (elegidosAhora < cuantos) {
                int j = desde + azar.entero(candidatas);
                boolean repetido = false;
                for (int t = 0; t < elegidosAhora; t++) {
                    if (elegidos[t] == j) {
                        repetido = true;
                        break;
                    }
                }
                if (!repetido) {
                    elegidos[elegidosAhora++] = j;
                }
            }
            for (int t = 0; t < elegidosAhora; t++) {
                prerrequisitos[aristas++] = elegidos[t];
            }
        }
        inicio[n] = aristas;
        return new PlanCrudo(n, k, ids, creditos, inicio, prerrequisitos, aristas);
    }

    /**
     * Genera un "árbol de ensamble": cada asignatura es prerrequisito de a lo
     * sumo otra, como en el ejemplo S1, S2, S3 → M → N. Es el caso en que el
     * algoritmo de Hu es óptimo, y la cota reforzada debería certificarlo
     * siempre. O(n + e).
     *
     * @param n                 número de asignaturas
     * @param k                 máximo de asignaturas por semestre
     * @param maxPrerrequisitos máximo de prerrequisitos por asignatura
     * @param semilla           semilla del generador
     * @return el plan en arreglos
     */
    public static PlanCrudo generarArbol(int n, int k, int maxPrerrequisitos, long semilla) {
        GeneradorAleatorio azar = new GeneradorAleatorio(semilla);
        long[] ids = new long[n];
        int[] creditos = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = 1_000_000L + 10L * i + azar.entero(10);
            creditos[i] = 2 + azar.entero(3);
        }
        // libres[0..cuantosLibres) = asignaturas anteriores que aún no son prerrequisito de nadie
        int[] libres = new int[Math.max(1, n)];
        int cuantosLibres = 0;
        int[] inicio = new int[n + 1];
        int[] prerrequisitos = new int[Math.max(1, n)];
        int aristas = 0;
        for (int i = 0; i < n; i++) {
            inicio[i] = aristas;
            int cuantos = Math.min(azar.entero(maxPrerrequisitos + 1), cuantosLibres);
            for (int t = 0; t < cuantos; t++) {
                int posicion = azar.entero(cuantosLibres);
                prerrequisitos[aristas++] = libres[posicion];
                libres[posicion] = libres[--cuantosLibres]; // se quita sin dejar huecos
            }
            libres[cuantosLibres++] = i;
        }
        inicio[n] = aristas;
        return new PlanCrudo(n, k, ids, creditos, inicio, prerrequisitos, aristas);
    }

    /**
     * Carga un plan generado en las estructuras, igual que el cargador de
     * archivos: primero registra todos los IDs en el AVL y después enlaza
     * cada prerrequisito buscándolo por su ID. O((n + e) log n).
     *
     * @param plan plan en arreglos
     * @return el registro
     */
    public static RegistroAsignaturas registrar(PlanCrudo plan) {
        RegistroAsignaturas registro = new RegistroAsignaturas(plan.n);
        Asignatura[] creadas = new Asignatura[plan.n];
        for (int i = 0; i < plan.n; i++) {
            creadas[i] = registro.registrar(plan.ids[i], null, plan.creditos[i]);
        }
        for (int i = 0; i < plan.n; i++) {
            for (int p = plan.inicio[i]; p < plan.inicio[i + 1]; p++) {
                Asignatura prerrequisito = registro.buscar(plan.ids[plan.prerrequisitos[p]]);
                registro.agregarPrerrequisito(creadas[i], prerrequisito);
            }
        }
        return registro;
    }

    /**
     * Guarda el plan en el formato de los archivos de entrada (aclaración 3.2:
     * persistir los datos de prueba). O(n + e).
     *
     * @param plan plan en arreglos
     * @param ruta archivo de salida
     * @throws IOException si no se puede escribir
     */
    public static void escribirArchivo(PlanCrudo plan, String ruta) throws IOException {
        try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) {
            salida.write("# Plan aleatorio: " + plan.n + " asignaturas, " + plan.aristas + " prerrequisitos.");
            salida.newLine();
            salida.write("# id;nombre;prerrequisitos;créditos");
            salida.newLine();
            salida.write(String.valueOf(plan.k));
            salida.newLine();
            StringBuilder linea = new StringBuilder(64);
            for (int i = 0; i < plan.n; i++) {
                linea.setLength(0);
                linea.append(plan.ids[i]).append(";Asignatura ").append(i + 1).append(';');
                for (int p = plan.inicio[i]; p < plan.inicio[i + 1]; p++) {
                    if (p > plan.inicio[i]) {
                        linea.append(',');
                    }
                    linea.append(plan.ids[plan.prerrequisitos[p]]);
                }
                linea.append(';').append(plan.creditos[i]);
                salida.write(linea.toString());
                salida.newLine();
            }
        }
    }

    /**
     * IDs consecutivos y ordenados: el peor caso de un BST sin balanceo.
     *
     * @param n cantidad
     * @return IDs en orden creciente
     */
    public static long[] idsOrdenados(int n) {
        long[] ids = new long[n];
        for (int i = 0; i < n; i++) {
            ids[i] = 1_000_000L + 10L * i;
        }
        return ids;
    }
}
