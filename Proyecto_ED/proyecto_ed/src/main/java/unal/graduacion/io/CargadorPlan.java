package unal.graduacion.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Lee un plan de estudios desde un archivo de texto.
 *
 * Formato (líneas vacías y las que empiezan por # se ignoran):
 * <pre>
 * 3                                   &lt;- k: máximo de asignaturas por semestre
 * 101;Cálculo Diferencial;            &lt;- id;nombre;prerrequisitos separados por coma
 * 104;Cálculo Integral;101
 * 107;Estructuras de Datos;105,106
 * </pre>
 *
 * Se lee en dos pasadas: primero se registran todas las asignaturas y luego
 * se enlazan los prerrequisitos, así el orden de las líneas no importa.
 */
public final class CargadorPlan {

    /** Plan leído: registro de asignaturas + límite por semestre. */
    public record PlanCargado(RegistroAsignaturas registro, int limitePorSemestre) { }

    private CargadorPlan() { }

    public static PlanCargado cargar(Path ruta) throws IOException {
        try (BufferedReader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            return cargar(lector);
        }
    }

    public static PlanCargado cargar(Reader fuente) throws IOException {
        BufferedReader lector = fuente instanceof BufferedReader b ? b : new BufferedReader(fuente);
        RegistroAsignaturas registro = new RegistroAsignaturas();
        ListaEnlazada<String[]> pendientesDeEnlazar = new ListaEnlazada<>();
        Integer limite = null;

        String linea;
        int numeroLinea = 0;
        while ((linea = lector.readLine()) != null) {
            numeroLinea++;
            linea = linea.strip();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            if (limite == null) {
                limite = leerEntero(linea, numeroLinea, "el límite k");
                if (limite < 1) {
                    throw new FormatoInvalidoException(numeroLinea, "k debe ser al menos 1");
                }
                continue;
            }
            String[] partes = linea.split(";", -1);
            if (partes.length < 2) {
                throw new FormatoInvalidoException(numeroLinea, "se esperaba 'id;nombre;prerrequisitos'");
            }
            int id = leerEntero(partes[0], numeroLinea, "el ID");
            try {
                registro.registrar(new Asignatura(id, partes[1]));
            } catch (IllegalArgumentException e) {
                throw new FormatoInvalidoException(numeroLinea, e.getMessage());
            }
            String prerreqs = partes.length > 2 ? partes[2].strip() : "";
            if (!prerreqs.isEmpty()) {
                pendientesDeEnlazar.agregarFinal(new String[] {String.valueOf(id), prerreqs, String.valueOf(numeroLinea)});
            }
        }
        if (limite == null) {
            throw new FormatoInvalidoException(0, "el archivo está vacío: falta el límite k");
        }

        // Segunda pasada: enlazar prerrequisitos (acceso por ID en el AVL).
        for (String[] p : pendientesDeEnlazar) {
            int linea2 = Integer.parseInt(p[2]);
            Asignatura asignatura = registro.obtener(Integer.parseInt(p[0]));
            for (String token : p[1].split(",")) {
                if (token.isBlank()) {
                    continue;
                }
                int idPrerreq = leerEntero(token, linea2, "un prerrequisito");
                Asignatura prerreq = registro.buscar(idPrerreq);
                if (prerreq == null) {
                    throw new FormatoInvalidoException(linea2, "el prerrequisito " + idPrerreq + " no existe");
                }
                try {
                    asignatura.agregarPrerrequisito(prerreq);
                } catch (IllegalArgumentException e) {
                    throw new FormatoInvalidoException(linea2, e.getMessage());
                }
            }
        }
        return new PlanCargado(registro, limite);
    }

    private static int leerEntero(String texto, int linea, String campo) {
        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new FormatoInvalidoException(linea, campo + " debe ser un número entero: '" + texto.strip() + "'");
        }
    }

    /** Error de formato con el número de línea del archivo. */
    public static class FormatoInvalidoException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public FormatoInvalidoException(int linea, String mensaje) {
            super(linea > 0 ? "Línea " + linea + ": " + mensaje : mensaje);
        }
    }
}
