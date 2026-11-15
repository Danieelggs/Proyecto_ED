package unal.graduacion;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import unal.graduacion.io.CargadorPlan;
import unal.graduacion.io.ExportadorHTML;
import unal.graduacion.io.ImpresorPlan;
import unal.graduacion.medicion.ExperimentoCalidad;
import unal.graduacion.medicion.GeneradorPlanes;
import unal.graduacion.medicion.Medidor;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.planificador.Analisis;
import unal.graduacion.pruebas.PruebasFuncionales;

/**
 * Punto de entrada.
 *
 * <pre>
 * java -cp out unal.graduacion.Main                          menú interactivo
 * java -cp out unal.graduacion.Main plan.txt [k]             calcula el plan (k del archivo o el indicado)
 * java -cp out unal.graduacion.Main plan.txt [k] --html p.html   además exporta la vista HTML
 * java -cp out unal.graduacion.Main plan.txt --id 2016699    ficha de una asignatura (RF2)
 * java -cp out unal.graduacion.Main --medir [--rapido]       mediciones de tiempo a CSV (RF7)
 * java -cp out unal.graduacion.Main --calidad                certificación de la cota en planes aleatorios
 * java -cp out unal.graduacion.Main --generar n k archivo [semilla]
 * java -cp out unal.graduacion.Main --pruebas                pruebas funcionales
 * </pre>
 */
public final class Main {

    private static final PrintStream OUT = System.out;

    private Main() {
    }

    /**
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        int codigo;
        try {
            codigo = ejecutar(args);
        } catch (IOException e) {
            OUT.println("Error de lectura o escritura: " + e.getMessage());
            codigo = 2;
        }
        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    static int ejecutar(String[] args) throws IOException {
        if (args.length == 0) {
            new MenuConsola(System.in, OUT).iniciar();
            return 0;
        }
        switch (args[0]) {
            case "-h":
            case "--ayuda":
                ayuda();
                return 0;
            case "--medir":
                return medir(args);
            case "--calidad":
                ExperimentoCalidad.ejecutar(opcion(args, "--csv", "resultados/calidad.csv"), OUT);
                return 0;
            case "--generar":
                return generar(args);
            case "--pruebas":
                return PruebasFuncionales.ejecutar(OUT) == 0 ? 0 : 1;
            default:
                if (args[0].startsWith("--")) {
                    OUT.println("Opción desconocida: " + args[0]);
                    ayuda();
                    return 1;
                }
                return planificar(args);
        }
    }

    // ------------------------------------------------------------------ modos

    private static int planificar(String[] args) throws IOException {
        String ruta = args[0];
        int k = 0;
        String html = null;
        Long id = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--html") && i + 1 < args.length) {
                html = args[++i];
            } else if (args[i].equals("--id") && i + 1 < args.length) {
                Long leido = leerLong(args[++i]);
                if (leido == null) {
                    OUT.println("El código \"" + args[i] + "\" no es un número.");
                    return 1;
                }
                id = leido;
            } else {
                Integer leido = leerEntero(args[i]);
                if (leido == null || leido < 1) {
                    OUT.println("Argumento no válido: \"" + args[i] + "\". k debe ser un entero mayor o igual a 1.");
                    return 1;
                }
                k = leido;
            }
        }
        if (!new File(ruta).isFile()) {
            OUT.println("No existe el archivo " + ruta + ".");
            return 1;
        }

        CargadorPlan.Resultado carga = CargadorPlan.cargarArchivo(ruta);
        ImpresorPlan.imprimirCarga(carga, ruta, OUT);
        if (!carga.exitoso()) {
            return 1;
        }
        int kUsado = k > 0 ? k : carga.k();
        Analisis analisis = Analisis.ejecutar(carga.registro(), kUsado);

        if (id != null) {
            Asignatura asignatura = carga.registro().buscar(id);
            if (asignatura == null) {
                OUT.println("No hay ninguna asignatura con el código " + id + ".");
                return 1;
            }
            ImpresorPlan.describirAsignatura(asignatura, id, analisis, OUT);
            return 0;
        }

        ImpresorPlan.imprimir(analisis, ruta, OUT);
        if (html != null && analisis.hayPlan()) {
            crearCarpetaDe(html);
            ExportadorHTML.exportar(analisis, ruta, html);
            OUT.println();
            OUT.println("Vista HTML guardada en " + html + ".");
        }
        return analisis.hayPlan() ? 0 : 1;
    }

    private static int medir(String[] args) throws IOException {
        Medidor.Configuracion c = tieneOpcion(args, "--rapido") ? Medidor.Configuracion.rapida() : new Medidor.Configuracion();
        c.archivoCsv = opcion(args, "--csv", c.archivoCsv);
        Integer repeticiones = leerEntero(opcion(args, "--repeticiones", String.valueOf(c.repeticiones)));
        Integer k = leerEntero(opcion(args, "--k", String.valueOf(c.k)));
        Long semilla = leerLong(opcion(args, "--semilla", String.valueOf(c.semilla)));
        if (repeticiones == null || repeticiones < 1 || k == null || k < 1 || semilla == null) {
            OUT.println("Revise --repeticiones, --k y --semilla: deben ser enteros positivos.");
            return 1;
        }
        c.repeticiones = repeticiones;
        c.k = k;
        c.semilla = semilla;
        String tamanos = opcion(args, "--tamanos", null);
        if (tamanos != null) {
            String[] partes = tamanos.split(",");
            c.tamanos = new int[partes.length];
            for (int i = 0; i < partes.length; i++) {
                Integer n = leerEntero(partes[i].trim());
                if (n == null || n < 1) {
                    OUT.println("Tamaño no válido en --tamanos: " + partes[i]);
                    return 1;
                }
                c.tamanos[i] = n;
            }
        }
        Medidor.ejecutar(c, OUT);
        return 0;
    }

    private static int generar(String[] args) throws IOException {
        if (args.length < 4) {
            OUT.println("Uso: --generar n k archivo [semilla]");
            return 1;
        }
        Integer n = leerEntero(args[1]);
        Integer k = leerEntero(args[2]);
        Long semilla = args.length >= 5 ? leerLong(args[4]) : Long.valueOf(2026);
        if (n == null || n < 1 || k == null || k < 1 || semilla == null) {
            OUT.println("n y k deben ser enteros mayores o iguales a 1, y la semilla un entero.");
            return 1;
        }
        crearCarpetaDe(args[3]);
        GeneradorPlanes.PlanCrudo plan = GeneradorPlanes.generar(n, k, 3, 100, semilla);
        GeneradorPlanes.escribirArchivo(plan, args[3]);
        OUT.println("Plan aleatorio guardado en " + args[3] + ": " + n + " asignaturas, " + plan.aristas()
                + " prerrequisitos, k = " + k + ", semilla " + semilla + ".");
        return 0;
    }

    private static void ayuda() {
        OUT.println("Graduación UNAL · planificador del mínimo número de semestres");
        OUT.println();
        OUT.println("  java -cp out unal.graduacion.Main                              menú interactivo");
        OUT.println("  java -cp out unal.graduacion.Main <plan.txt> [k]               calcula el plan; k del archivo si no se da");
        OUT.println("  java -cp out unal.graduacion.Main <plan.txt> [k] --html <p.html>   y exporta la vista HTML");
        OUT.println("  java -cp out unal.graduacion.Main <plan.txt> --id <código>     ficha de una asignatura");
        OUT.println("  java -cp out unal.graduacion.Main --medir [--rapido] [--repeticiones R] [--k K] [--tamanos 1000,5000] [--csv ruta]");
        OUT.println("  java -cp out unal.graduacion.Main --calidad [--csv ruta]");
        OUT.println("  java -cp out unal.graduacion.Main --generar <n> <k> <archivo> [semilla]");
        OUT.println("  java -cp out unal.graduacion.Main --pruebas");
    }

    // ------------------------------------------------------------------ utilidades

    static Integer leerEntero(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Long leerLong(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Long.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static void crearCarpetaDe(String ruta) throws IOException {
        File carpeta = new File(ruta).getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta);
        }
    }

    private static boolean tieneOpcion(String[] args, String nombre) {
        for (String arg : args) {
            if (arg.equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static String opcion(String[] args, String nombre, String porDefecto) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(nombre)) {
                return args[i + 1];
            }
        }
        return porDefecto;
    }
}
