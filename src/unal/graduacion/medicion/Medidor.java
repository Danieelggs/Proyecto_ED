package unal.graduacion.medicion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.ArbolBST;
import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.CalculadoraAlturas;
import unal.graduacion.planificador.CotaInferior;
import unal.graduacion.planificador.DisponiblesLista;
import unal.graduacion.planificador.Plan;
import unal.graduacion.planificador.Planificador;
import unal.graduacion.planificador.ValidadorCiclos;
import unal.graduacion.planificador.VerificadorPlan;

/**
 * Mide tiempos de ejecución con planes aleatorios de tamaño creciente y los
 * exporta a CSV (RF7, aclaración 3.3 del enunciado).
 *
 * <p>Metodología:</p>
 * <ul>
 *   <li>Cronómetro {@code System.nanoTime()}.</li>
 *   <li>Tres corridas de calentamiento con n = 10.000 que no se registran: el
 *       compilador JIT de Java optimiza el código durante las primeras corridas
 *       y las haría ver más lentas.</li>
 *   <li>Varias repeticiones por tamaño, cada una con otra semilla; el resumen
 *       muestra la mediana, que no se afecta por una corrida atípica.</li>
 *   <li>Generar los datos, verificar el plan y llamar al recolector de basura
 *       quedan fuera de los tramos medidos, y nada se imprime dentro de ellos.</li>
 * </ul>
 *
 * <p>Se mide cada fase del sistema y cada operación clave de las estructuras.
 * Las comparativas con un BST y con una lista solo corren hasta
 * {@code limiteCuadratico}, porque son O(n²).</p>
 */
public final class Medidor {

    /** Parámetros de una medición. */
    public static final class Configuracion {
        /** Tamaños de entrada (número de asignaturas). */
        public int[] tamanos = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, 1_000_000};
        /** Repeticiones por tamaño. */
        public int repeticiones = 5;
        /** Máximo de asignaturas por semestre. */
        public int k = 5;
        /** Semilla base; la repetición r usa semilla + r. */
        public long semilla = 2026;
        /** Máximo de prerrequisitos por asignatura generada. */
        public int maxPrerrequisitos = 3;
        /** Los prerrequisitos se eligen entre las {@code ventana} asignaturas anteriores. */
        public int ventana = 100;
        /** Tamaño máximo para las comparativas O(n²). */
        public int limiteCuadratico = 20_000;
        /** Archivo CSV de salida. */
        public String archivoCsv = "resultados/tiempos.csv";

        /** @return configuración corta para revisar que todo funciona */
        public static Configuracion rapida() {
            Configuracion c = new Configuracion();
            c.tamanos = new int[] {1_000, 5_000, 10_000, 50_000};
            c.repeticiones = 3;
            return c;
        }
    }

    private static final class Fila {
        private final String experimento;
        private final String estructura;
        private final int n;
        private final int aristas;
        private final int k;
        private final int repeticion;
        private final long nanos;
        private final int semestres;
        private final int cota;
        private final int garantizado;
        private final int alturaArbol;

        private Fila(String experimento, String estructura, int n, int aristas, int k, int repeticion, long nanos,
                     int semestres, int cota, int garantizado, int alturaArbol) {
            this.experimento = experimento;
            this.estructura = estructura;
            this.n = n;
            this.aristas = aristas;
            this.k = k;
            this.repeticion = repeticion;
            this.nanos = nanos;
            this.semestres = semestres;
            this.cota = cota;
            this.garantizado = garantizado;
            this.alturaArbol = alturaArbol;
        }
    }

    /** Orden en que se muestran los experimentos en el resumen. */
    private static final String[] EXPERIMENTOS = {
        "registro_avl", "validacion_kahn", "alturas_pila", "planificacion_avl", "cota_inferior", "proceso_total",
        "avl_insertar", "avl_buscar", "lista_agregar_final", "cola_encolar_desencolar", "pila_apilar_desapilar",
        "planificacion_lista", "bst_insertar_ordenados", "avl_insertar_ordenados"
    };

    private static final Object MARCA = new Object();
    private static final int NO_APLICA = -1;
    /** Acumula resultados para que el JIT no elimine el trabajo medido. */
    private static long sumidero;

    private Medidor() {
    }

    /**
     * Corre todas las mediciones, escribe el CSV y muestra el resumen.
     *
     * @param c   parámetros
     * @param out salida para el progreso y el resumen
     * @throws IOException si no se puede escribir el CSV
     */
    public static void ejecutar(Configuracion c, PrintStream out) throws IOException {
        out.println("Mediciones de tiempo (RF7): " + c.repeticiones + " repeticiones por tamaño, k = " + c.k
                + ", semilla " + c.semilla + ".");
        out.println("Calentando la JVM con 3 corridas de n = 10000 que no se registran...");
        ListaEnlazada<Fila> descartadas = new ListaEnlazada<>();
        for (int i = 0; i < 3; i++) {
            medirTamano(10_000, i, c, descartadas);
        }

        ListaEnlazada<Fila> filas = new ListaEnlazada<>();
        for (int n : c.tamanos) {
            out.print("  n = " + n + " ... ");
            out.flush();
            long inicio = System.nanoTime();
            for (int r = 0; r < c.repeticiones; r++) {
                medirTamano(n, r, c, filas);
            }
            out.println("listo en " + segundos(System.nanoTime() - inicio) + " s");
        }

        escribirCsv(filas, c.archivoCsv);
        String resumen = rutaResumen(c.archivoCsv);
        escribirResumen(filas, c, resumen);
        imprimirResumen(filas, c, out);
        out.println();
        out.println("Datos completos en " + c.archivoCsv + " (una fila por repetición).");
        out.println("Medianas por tamaño en " + resumen + " (las leen las gráficas del reporte).");
        if (sumidero == 42) {
            out.println(); // nunca ocurre en la práctica; solo usa el sumidero
        }
    }

    // ------------------------------------------------------------------ mediciones

    private static void medirTamano(int n, int r, Configuracion c, ListaEnlazada<Fila> filas) {
        GeneradorPlanes.PlanCrudo crudo = GeneradorPlanes.generar(n, c.k, c.maxPrerrequisitos, c.ventana, c.semilla + r);
        int e = crudo.aristas();
        boolean grande = n >= 100_000;

        // Fases del sistema
        pausa(grande);
        long t0 = System.nanoTime();
        RegistroAsignaturas registro = GeneradorPlanes.registrar(crudo);
        long t1 = System.nanoTime();
        pausa(grande);
        long t2 = System.nanoTime();
        ValidadorCiclos.Resultado validacion = ValidadorCiclos.validar(registro);
        long t3 = System.nanoTime();
        pausa(grande);
        long t4 = System.nanoTime();
        int[] altura = CalculadoraAlturas.calcular(registro, validacion.ordenTopologico());
        long t5 = System.nanoTime();
        pausa(grande);
        long t6 = System.nanoTime();
        Plan plan = Planificador.planificar(registro, c.k, altura);
        long t7 = System.nanoTime();
        long t8 = System.nanoTime();
        CotaInferior cota = CotaInferior.calcular(altura, n, c.k);
        long t9 = System.nanoTime();

        String error = VerificadorPlan.verificar(registro, plan);
        if (error != null) {
            throw new IllegalStateException("Plan inválido con n = " + n + ": " + error);
        }
        int semestres = plan.cantidadSemestres();
        int garantizado = semestres == cota.reforzada() ? 1 : 0;
        filas.agregarAlFinal(new Fila("registro_avl", "AVL por ID", n, e, c.k, r, t1 - t0, NO_APLICA, NO_APLICA, NO_APLICA,
                registro.indicePorId().altura()));
        filas.agregarAlFinal(new Fila("validacion_kahn", "Cola circular", n, e, c.k, r, t3 - t2, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));
        filas.agregarAlFinal(new Fila("alturas_pila", "Pila", n, e, c.k, r, t5 - t4, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));
        filas.agregarAlFinal(new Fila("planificacion_avl", "AVL de prioridad", n, e, c.k, r, t7 - t6, semestres,
                cota.reforzada(), garantizado, NO_APLICA));
        filas.agregarAlFinal(new Fila("cota_inferior", "Arreglos", n, e, c.k, r, t9 - t8, NO_APLICA, cota.reforzada(), NO_APLICA, NO_APLICA));
        filas.agregarAlFinal(new Fila("proceso_total", "Todas", n, e, c.k, r, (t1 - t0) + (t3 - t2) + (t5 - t4) + (t7 - t6) + (t9 - t8),
                semestres, cota.reforzada(), garantizado, NO_APLICA));

        // Línea base O(n²): disponibles en una lista sin orden
        if (n <= c.limiteCuadratico) {
            pausa(grande);
            long a = System.nanoTime();
            Plan conLista = Planificador.planificar(registro, c.k, new DisponiblesLista(altura));
            long b = System.nanoTime();
            if (conLista.cantidadSemestres() != semestres) {
                throw new IllegalStateException("La lista y el AVL dieron planes distintos con n = " + n);
            }
            filas.agregarAlFinal(new Fila("planificacion_lista", "Lista sin orden", n, e, c.k, r, b - a,
                    conLista.cantidadSemestres(), cota.reforzada(), garantizado, NO_APLICA));
        }
        registro = null;
        plan = null;

        // Operaciones clave de cada estructura
        long[] ids = crudo.ids();
        pausa(grande);
        long a = System.nanoTime();
        ArbolAVL<Long, Object> avl = new ArbolAVL<>();
        for (long id : ids) {
            avl.insertar(id, MARCA);
        }
        long b = System.nanoTime();
        int encontrados = 0;
        for (long id : ids) {
            if (avl.buscar(id) != null) {
                encontrados++;
            }
        }
        long d = System.nanoTime();
        sumidero += encontrados;
        filas.agregarAlFinal(new Fila("avl_insertar", "AVL", n, e, c.k, r, b - a, NO_APLICA, NO_APLICA, NO_APLICA, avl.altura()));
        filas.agregarAlFinal(new Fila("avl_buscar", "AVL", n, e, c.k, r, d - b, NO_APLICA, NO_APLICA, NO_APLICA, avl.altura()));
        avl = null;

        pausa(grande);
        a = System.nanoTime();
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        for (int i = 0; i < n; i++) {
            lista.agregarAlFinal(MARCA);
        }
        b = System.nanoTime();
        sumidero += lista.tamano();
        lista = null;
        filas.agregarAlFinal(new Fila("lista_agregar_final", "Lista enlazada", n, e, c.k, r, b - a, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));

        pausa(grande);
        a = System.nanoTime();
        Cola<Object> cola = new Cola<>();
        for (int i = 0; i < n; i++) {
            cola.encolar(MARCA);
        }
        while (!cola.estaVacia()) {
            cola.desencolar();
        }
        b = System.nanoTime();
        sumidero += cola.capacidad();
        filas.agregarAlFinal(new Fila("cola_encolar_desencolar", "Cola circular", n, e, c.k, r, b - a, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));

        pausa(grande);
        a = System.nanoTime();
        Pila<Object> pila = new Pila<>();
        for (int i = 0; i < n; i++) {
            pila.apilar(MARCA);
        }
        while (!pila.estaVacia()) {
            pila.desapilar();
        }
        b = System.nanoTime();
        filas.agregarAlFinal(new Fila("pila_apilar_desapilar", "Pila", n, e, c.k, r, b - a, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));

        // Peor caso del BST: IDs ordenados
        if (n <= c.limiteCuadratico) {
            long[] ordenados = GeneradorPlanes.idsOrdenados(n);
            a = System.nanoTime();
            ArbolBST<Long, Object> bst = new ArbolBST<>();
            for (long id : ordenados) {
                bst.insertar(id, MARCA);
            }
            b = System.nanoTime();
            ArbolAVL<Long, Object> avlOrdenado = new ArbolAVL<>();
            long c1 = System.nanoTime();
            for (long id : ordenados) {
                avlOrdenado.insertar(id, MARCA);
            }
            long c2 = System.nanoTime();
            filas.agregarAlFinal(new Fila("bst_insertar_ordenados", "BST sin balanceo", n, e, c.k, r, b - a,
                    NO_APLICA, NO_APLICA, NO_APLICA, bst.altura()));
            filas.agregarAlFinal(new Fila("avl_insertar_ordenados", "AVL", n, e, c.k, r, c2 - c1,
                    NO_APLICA, NO_APLICA, NO_APLICA, avlOrdenado.altura()));
        }
    }

    private static void pausa(boolean grande) {
        if (grande) {
            System.gc();
        }
    }

    // ------------------------------------------------------------------ salida

    private static void escribirCsv(ListaEnlazada<Fila> filas, String ruta) throws IOException {
        File archivo = new File(ruta);
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta);
        }
        try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            salida.write("experimento,estructura,n,e,k,repeticion,tiempo_ns,tiempo_ms,semestres,cota_inferior,optimo_garantizado,altura_arbol");
            salida.newLine();
            for (ListaEnlazada.Nodo<Fila> nodo = filas.primero(); nodo != null; nodo = nodo.siguiente()) {
                Fila f = nodo.dato();
                salida.write(f.experimento + "," + f.estructura + "," + f.n + "," + f.aristas + "," + f.k + "," + f.repeticion
                        + "," + f.nanos + "," + milisegundos(f.nanos) + "," + celda(f.semestres) + "," + celda(f.cota)
                        + "," + celda(f.garantizado) + "," + celda(f.alturaArbol));
                salida.newLine();
            }
        }
    }

    /**
     * @param rutaCsv ruta del CSV completo
     * @return la ruta del resumen: tiempos.csv da tiempos_resumen.csv
     */
    static String rutaResumen(String rutaCsv) {
        return rutaCsv.endsWith(".csv")
                ? rutaCsv.substring(0, rutaCsv.length() - 4) + "_resumen.csv"
                : rutaCsv + "_resumen.csv";
    }

    /**
     * Escribe las medianas en una tabla ancha: una fila por tamaño y una
     * columna por experimento, en milisegundos, vacía si ese experimento no se
     * midió en ese tamaño. Al final van la mediana de e y las alturas de los
     * árboles. Es el archivo que leen las gráficas del reporte en LaTeX.
     */
    private static void escribirResumen(ListaEnlazada<Fila> filas, Configuracion c, String ruta) throws IOException {
        try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) {
            StringBuilder cabecera = new StringBuilder("n");
            for (String experimento : EXPERIMENTOS) {
                cabecera.append(',').append(experimento);
            }
            cabecera.append(",e,altura_avl_id,altura_bst_ordenados,altura_avl_ordenados");
            salida.write(cabecera.toString());
            salida.newLine();
            for (int n : c.tamanos) {
                StringBuilder linea = new StringBuilder(String.valueOf(n));
                for (String experimento : EXPERIMENTOS) {
                    long[] tiempos = new long[c.repeticiones];
                    int cuantos = 0;
                    for (ListaEnlazada.Nodo<Fila> nodo = filas.primero(); nodo != null; nodo = nodo.siguiente()) {
                        Fila f = nodo.dato();
                        if (f.n == n && f.experimento.equals(experimento)) {
                            tiempos[cuantos++] = f.nanos;
                        }
                    }
                    linea.append(',');
                    if (cuantos > 0) {
                        linea.append(milisegundos(mediana(tiempos, cuantos)));
                    }
                }
                linea.append(',').append(medianaDeAristas(filas, n, c.repeticiones));
                linea.append(',').append(celda(alturaMaxima(filas, n, "registro_avl")));
                linea.append(',').append(celda(alturaMaxima(filas, n, "bst_insertar_ordenados")));
                linea.append(',').append(celda(alturaMaxima(filas, n, "avl_insertar_ordenados")));
                salida.write(linea.toString());
                salida.newLine();
            }
        }
    }

    private static long medianaDeAristas(ListaEnlazada<Fila> filas, int n, int repeticiones) {
        long[] aristas = new long[repeticiones];
        int cuantos = 0;
        for (ListaEnlazada.Nodo<Fila> nodo = filas.primero(); nodo != null; nodo = nodo.siguiente()) {
            Fila f = nodo.dato();
            if (f.n == n && f.experimento.equals("registro_avl")) {
                aristas[cuantos++] = f.aristas;
            }
        }
        return cuantos == 0 ? 0 : mediana(aristas, cuantos);
    }

    private static int alturaMaxima(ListaEnlazada<Fila> filas, int n, String experimento) {
        int altura = NO_APLICA;
        for (ListaEnlazada.Nodo<Fila> nodo = filas.primero(); nodo != null; nodo = nodo.siguiente()) {
            Fila f = nodo.dato();
            if (f.n == n && f.experimento.equals(experimento) && f.alturaArbol > altura) {
                altura = f.alturaArbol;
            }
        }
        return altura;
    }

    private static void imprimirResumen(ListaEnlazada<Fila> filas, Configuracion c, PrintStream out) {
        out.println();
        out.println("Mediana por experimento y tamaño. ns/n y ns/(n·log2 n) se acercan a una constante");
        out.println("cuando el tiempo crece como O(n) y O(n log n), respectivamente.");
        out.println();
        out.println(String.format("%-25s %9s %12s %10s %14s %8s", "experimento", "n", "mediana ms", "ns/n", "ns/(n·log2 n)", "altura"));
        for (String experimento : EXPERIMENTOS) {
            boolean hubo = false;
            for (int n : c.tamanos) {
                long[] tiempos = new long[c.repeticiones];
                int cuantos = 0;
                int altura = NO_APLICA;
                for (ListaEnlazada.Nodo<Fila> nodo = filas.primero(); nodo != null; nodo = nodo.siguiente()) {
                    Fila f = nodo.dato();
                    if (f.n == n && f.experimento.equals(experimento)) {
                        tiempos[cuantos++] = f.nanos;
                        altura = f.alturaArbol;
                    }
                }
                if (cuantos == 0) {
                    continue;
                }
                hubo = true;
                long mediana = mediana(tiempos, cuantos);
                double porElemento = (double) mediana / n;
                double porNLogN = (double) mediana / (n * (Math.log(n) / Math.log(2)));
                out.println(String.format("%-25s %9d %12.3f %10.1f %14.2f %8s", experimento, n, mediana / 1e6,
                        porElemento, porNLogN, altura == NO_APLICA ? "" : String.valueOf(altura)));
            }
            if (hubo) {
                out.println();
            }
        }
    }

    /** Mediana de los primeros {@code cuantos} valores (ordenamiento por inserción propio). */
    private static long mediana(long[] valores, int cuantos) {
        for (int i = 1; i < cuantos; i++) {
            long actual = valores[i];
            int j = i - 1;
            while (j >= 0 && valores[j] > actual) {
                valores[j + 1] = valores[j];
                j--;
            }
            valores[j + 1] = actual;
        }
        if (cuantos % 2 == 1) {
            return valores[cuantos / 2];
        }
        return (valores[cuantos / 2 - 1] + valores[cuantos / 2]) / 2;
    }

    private static String celda(int valor) {
        return valor == NO_APLICA ? "" : String.valueOf(valor);
    }

    /** Milisegundos con 3 decimales y punto decimal, sin depender de la configuración regional. */
    private static String milisegundos(long nanos) {
        long micros = nanos / 1000;
        long enteros = micros / 1000;
        long decimales = micros % 1000;
        String texto = String.valueOf(decimales);
        while (texto.length() < 3) {
            texto = "0" + texto;
        }
        return enteros + "." + texto;
    }

    private static String segundos(long nanos) {
        return milisegundos(nanos / 1000);
    }
}
