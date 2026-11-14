package unal.graduacion.medicion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.CalculadoraAlturas;
import unal.graduacion.planificador.CotaInferior;
import unal.graduacion.planificador.Plan;
import unal.graduacion.planificador.Planificador;
import unal.graduacion.planificador.ValidadorCiclos;
import unal.graduacion.planificador.VerificadorPlan;

/**
 * Mide la calidad de los planes, no su velocidad: en cuántos planes aleatorios
 * el resultado iguala la cota inferior (y por tanto es óptimo con certeza),
 * con la cota simple y con la reforzada, y cuánto se aleja cuando no la iguala.
 *
 * <p>Usa dos familias de planes:</p>
 * <ul>
 *   <li><b>general</b>: hasta 3 prerrequisitos elegidos entre cualquier
 *       asignatura anterior;</li>
 *   <li><b>árbol de ensamble</b>: cada asignatura es prerrequisito de a lo
 *       sumo otra. Ahí el algoritmo de Hu es óptimo, y la cota reforzada
 *       debería certificarlo en el 100 % de los casos.</li>
 * </ul>
 *
 * <p>Es la evidencia para la justificación de la solución: como el problema
 * general es NP-difícil no se puede prometer el mínimo siempre, pero sí medir
 * cuántas veces se certifica.</p>
 */
public final class ExperimentoCalidad {

    private static final int[] TAMANOS = {20, 50, 100, 200};
    private static final int[] VALORES_K = {2, 3, 4, 5, 6};
    private static final int PLANES_POR_CASO = 300;
    private static final String[] FAMILIAS = {"general", "arbol"};

    private ExperimentoCalidad() {
    }

    /**
     * Corre el experimento, escribe el CSV y muestra la tabla.
     *
     * @param archivoCsv archivo de salida
     * @param out        salida para la tabla
     * @throws IOException si no se puede escribir el CSV
     */
    public static void ejecutar(String archivoCsv, PrintStream out) throws IOException {
        File archivo = new File(archivoCsv);
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta);
        }
        out.println("Calidad de los planes: " + PLANES_POR_CASO + " planes aleatorios por caso.");

        try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            salida.write("familia,n,k,planes,certificados_cota_simple,certificados_cota_reforzada,pct_simple,pct_reforzada,brecha_promedio,brecha_maxima");
            salida.newLine();
            for (String familia : FAMILIAS) {
                out.println();
                out.println(familia.equals("arbol")
                        ? "Árbol de ensamble (cada asignatura es prerrequisito de a lo sumo otra; hasta 2 prerrequisitos):"
                        : "General (hasta 3 prerrequisitos entre cualquier asignatura anterior):");
                out.println(String.format("%6s %4s %22s %25s %16s %14s", "n", "k", "certifica cota simple",
                        "certifica cota reforzada", "brecha promedio", "brecha máxima"));
                for (int n : TAMANOS) {
                    for (int k : VALORES_K) {
                        medirCaso(familia, n, k, salida, out);
                    }
                }
            }
        }
        out.println();
        out.println("Certifica = el plan iguala la cota, así que es óptimo con certeza.");
        out.println("Brecha = semestres del plan - cota reforzada: un máximo de lo que se podría mejorar, no el error real.");
        out.println("Datos en " + archivoCsv + ".");
    }

    private static void medirCaso(String familia, int n, int k, BufferedWriter salida, PrintStream out) throws IOException {
        int simple = 0;
        int reforzada = 0;
        long sumaBrechas = 0;
        int brechaMaxima = 0;
        for (int s = 1; s <= PLANES_POR_CASO; s++) {
            long semilla = 1_000_003L * n + 10_007L * k + s + (familia.equals("arbol") ? 500_000_000L : 0);
            GeneradorPlanes.PlanCrudo crudo = familia.equals("arbol")
                    ? GeneradorPlanes.generarArbol(n, k, 2, semilla)
                    : GeneradorPlanes.generar(n, k, 3, n, semilla);
            RegistroAsignaturas registro = GeneradorPlanes.registrar(crudo);
            ValidadorCiclos.Resultado validacion = ValidadorCiclos.validar(registro);
            int[] altura = CalculadoraAlturas.calcular(registro, validacion.ordenTopologico());
            Plan plan = Planificador.planificar(registro, k, altura);
            String error = VerificadorPlan.verificar(registro, plan);
            if (error != null) {
                throw new IllegalStateException("Plan inválido (" + familia + ", n = " + n + ", k = " + k + "): " + error);
            }
            CotaInferior cota = CotaInferior.calcular(altura, n, k);
            int semestres = plan.cantidadSemestres();
            if (semestres == cota.simple()) {
                simple++;
            }
            if (semestres == cota.reforzada()) {
                reforzada++;
            }
            int brecha = semestres - cota.reforzada();
            sumaBrechas += brecha;
            if (brecha > brechaMaxima) {
                brechaMaxima = brecha;
            }
        }
        double pctSimple = 100.0 * simple / PLANES_POR_CASO;
        double pctReforzada = 100.0 * reforzada / PLANES_POR_CASO;
        double brechaPromedio = (double) sumaBrechas / PLANES_POR_CASO;
        out.println(String.format("%6d %4d %21.1f%% %24.1f%% %16.3f %14d", n, k, pctSimple, pctReforzada,
                brechaPromedio, brechaMaxima));
        salida.write(familia + "," + n + "," + k + "," + PLANES_POR_CASO + "," + simple + "," + reforzada + ","
                + decimal(pctSimple) + "," + decimal(pctReforzada) + "," + decimal(brechaPromedio) + "," + brechaMaxima);
        salida.newLine();
    }

    /** Número con 3 decimales y punto decimal, sin depender de la configuración regional. */
    private static String decimal(double valor) {
        long milesimas = Math.round(valor * 1000);
        long enteros = milesimas / 1000;
        long resto = Math.abs(milesimas % 1000);
        String texto = String.valueOf(resto);
        while (texto.length() < 3) {
            texto = "0" + texto;
        }
        return enteros + "." + texto;
    }
}
