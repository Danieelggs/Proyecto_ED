package unal.graduacion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import unal.graduacion.io.CargadorPlan;
import unal.graduacion.io.ExportadorHTML;
import unal.graduacion.io.ImpresorPlan;
import unal.graduacion.medicion.ExperimentoCalidad;
import unal.graduacion.medicion.GeneradorPlanes;
import unal.graduacion.medicion.Medidor;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.Analisis;

/**
 * Menú de consola, como en el diagrama del sistema: el estudiante da la ruta
 * del plan y k; el cargador registra cada asignatura del archivo en el AVL con
 * su ID numérico y su lista de prerrequisitos (RF1); luego se consulta una
 * asignatura por su ID (RF2) y se calcula el plan por semestres.
 */
final class MenuConsola {

    private static final String PLAN_POR_DEFECTO = "datos/plan_2A74.txt";

    private final BufferedReader entrada;
    private final PrintStream out;
    private RegistroAsignaturas registro;
    private String fuente;
    private int k;
    private Analisis analisis;

    MenuConsola(InputStream entrada, PrintStream out) {
        this.entrada = new BufferedReader(new InputStreamReader(entrada));
        this.out = out;
    }

    void iniciar() throws IOException {
        out.println("Graduación UNAL · planificador del mínimo número de semestres");
        while (true) {
            out.println();
            out.println(registro == null ? "Plan cargado: ninguno"
                    : "Plan cargado: " + fuente + " (" + ImpresorPlan.cantidad(registro.cantidad(), "asignatura", "asignaturas")
                    + ", k = " + k + ")");
            out.println("  1. Cargar plan desde archivo");
            out.println("  2. Consultar asignatura por código");
            out.println("  3. Cambiar k (máximo de asignaturas por semestre)");
            out.println("  4. Calcular plan de semestres");
            out.println("  5. Exportar el plan a HTML");
            out.println("  6. Generar un plan aleatorio y guardarlo");
            out.println("  7. Mediciones (tiempos o calidad de la cota)");
            out.println("  0. Salir");
            String opcion = leer("Opción: ");
            if (opcion == null) {
                return;
            }
            switch (opcion.trim()) {
                case "1": cargar(); break;
                case "2": consultar(); break;
                case "3": cambiarK(); break;
                case "4": calcular(); break;
                case "5": exportar(); break;
                case "6": generar(); break;
                case "7": medir(); break;
                case "0":
                    out.println("Hasta luego.");
                    return;
                default:
                    out.println("Opción no válida.");
            }
        }
    }

    private void cargar() throws IOException {
        String ruta = leer("Ruta del plan [" + PLAN_POR_DEFECTO + "]: ");
        if (ruta == null) {
            return;
        }
        ruta = ruta.trim().isEmpty() ? PLAN_POR_DEFECTO : ruta.trim();
        if (!new File(ruta).isFile()) {
            out.println("No existe el archivo " + ruta + ".");
            return;
        }
        CargadorPlan.Resultado carga = CargadorPlan.cargarArchivo(ruta);
        ImpresorPlan.imprimirCarga(carga, ruta, out);
        if (!carga.exitoso()) {
            return;
        }
        registro = carga.registro();
        fuente = ruta;
        k = carga.k();
        analisis = null;
        out.println("Plan registrado: " + ImpresorPlan.resumenRegistro(registro) + " · k = " + k + " (leído del archivo).");
    }

    private void cambiarK() throws IOException {
        if (!hayPlan()) {
            return;
        }
        Integer nuevo = Main.leerEntero(leer("Nuevo k: "));
        if (nuevo == null || nuevo < 1) {
            out.println("k debe ser un entero mayor o igual a 1.");
            return;
        }
        k = nuevo;
        analisis = null;
        out.println("k = " + k + ".");
    }

    private void consultar() throws IOException {
        if (!hayPlan()) {
            return;
        }
        Long id = Main.leerLong(leer("Código de la asignatura o de una optativa: "));
        if (id == null) {
            out.println("El código debe ser un número.");
            return;
        }
        Asignatura asignatura = registro.buscar(id);
        if (asignatura == null) {
            out.println("No hay ninguna asignatura con el código " + id + ".");
            return;
        }
        if (analisis == null) {
            analisis = Analisis.ejecutar(registro, k);
        }
        ImpresorPlan.describirAsignatura(asignatura, id, analisis, out);
    }

    private void calcular() {
        if (!hayPlan()) {
            return;
        }
        analisis = Analisis.ejecutar(registro, k);
        ImpresorPlan.imprimir(analisis, fuente, out);
    }

    private void exportar() throws IOException {
        if (!hayPlan()) {
            return;
        }
        if (analisis == null) {
            analisis = Analisis.ejecutar(registro, k);
        }
        if (!analisis.hayPlan()) {
            out.println("El plan tiene ciclos; no hay semestres que exportar.");
            return;
        }
        String ruta = leer("Archivo HTML [resultados/plan.html]: ");
        if (ruta == null) {
            return;
        }
        ruta = ruta.trim().isEmpty() ? "resultados/plan.html" : ruta.trim();
        try {
            Main.crearCarpetaDe(ruta);
            ExportadorHTML.exportar(analisis, fuente, ruta);
            out.println("Vista HTML guardada en " + ruta + ". Ábrala en el navegador.");
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
        }
    }

    private void generar() throws IOException {
        Integer n = Main.leerEntero(leer("Número de asignaturas: "));
        Integer nuevoK = Main.leerEntero(leer("k: "));
        if (n == null || n < 1 || nuevoK == null || nuevoK < 1) {
            out.println("n y k deben ser enteros mayores o iguales a 1.");
            return;
        }
        String ruta = leer("Archivo [datos/aleatorio_" + n + ".txt]: ");
        if (ruta == null) {
            return;
        }
        ruta = ruta.trim().isEmpty() ? "datos/aleatorio_" + n + ".txt" : ruta.trim();
        Main.crearCarpetaDe(ruta);
        GeneradorPlanes.PlanCrudo plan = GeneradorPlanes.generar(n, nuevoK, 3, 100, 2026);
        GeneradorPlanes.escribirArchivo(plan, ruta);
        out.println("Guardado en " + ruta + ": " + n + " asignaturas, " + plan.aristas() + " prerrequisitos (semilla 2026).");
    }

    private void medir() throws IOException {
        out.println("  1. Tiempos, medición rápida (hasta n = 50.000)");
        out.println("  2. Tiempos, medición completa (hasta n = 1.000.000, unos minutos)");
        out.println("  3. Calidad de la cota inferior en planes aleatorios");
        String tipo = leer("¿Cuál? [1]: ");
        if (tipo == null) {
            return;
        }
        switch (tipo.trim()) {
            case "2": Medidor.ejecutar(new Medidor.Configuracion(), out); break;
            case "3": ExperimentoCalidad.ejecutar("resultados/calidad.csv", out); break;
            default: Medidor.ejecutar(Medidor.Configuracion.rapida(), out);
        }
    }

    private boolean hayPlan() {
        if (registro == null) {
            out.println("Primero cargue un plan (opción 1).");
            return false;
        }
        return true;
    }

    private String leer(String mensaje) throws IOException {
        out.print(mensaje);
        out.flush();
        return entrada.readLine();
    }
}
