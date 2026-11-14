package unal.graduacion.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Lee un plan de estudios desde un archivo de texto UTF-8 (RF1).
 *
 * <p>Formato:</p>
 * <pre>
 * # comentario (las líneas que empiezan por # y las vacías se ignoran)
 * 6                                        ← primera línea útil: k
 * id;nombre;prerrequisitos;créditos;componente;optativas
 * 1000005;Cálculo integral;1000004;4;B;2015556 Cálculo integral en una variable (4 cr)
 * </pre>
 * <ul>
 *   <li>Solo {@code id} y {@code nombre} son obligatorios; el formato corto
 *       {@code id;nombre;prerrequisitos} sigue siendo válido.</li>
 *   <li>Prerrequisitos: códigos separados por coma. Además se admite un
 *       requisito de créditos: {@code cr>=N} (N créditos aprobados de
 *       cualquier componente) o {@code crC>=N} (N créditos del componente C).</li>
 *   <li>Componente: B fundamentación, C disciplinar, L libre elección,
 *       P nivelación.</li>
 *   <li>Optativas: separadas por {@code |}. Si empiezan por un código, ese
 *       código queda registrado y buscarlo devuelve la casilla.</li>
 * </ul>
 *
 * <p>La carga se hace en <b>dos pasadas</b>: primero se registran todos los
 * IDs y luego se enlazan los prerrequisitos y los códigos de optativas. Así
 * una asignatura puede citar a otra que aparece más abajo en el archivo.
 * Costo: O((n + e) log n), por las búsquedas en el AVL.</p>
 */
public final class CargadorPlan {

    /** Resultado de una carga: el registro, k y los mensajes. */
    public static final class Resultado {
        private final RegistroAsignaturas registro;
        private final int k;
        private final ListaEnlazada<String> errores;
        private final ListaEnlazada<String> advertencias;

        private Resultado(RegistroAsignaturas registro, int k,
                          ListaEnlazada<String> errores, ListaEnlazada<String> advertencias) {
            this.registro = registro;
            this.k = k;
            this.errores = errores;
            this.advertencias = advertencias;
        }

        /** @return el registro con las asignaturas leídas */
        public RegistroAsignaturas registro() {
            return registro;
        }

        /** @return k leído del archivo (0 si no se pudo leer) */
        public int k() {
            return k;
        }

        /** @return errores que impiden usar el plan */
        public ListaEnlazada<String> errores() {
            return errores;
        }

        /** @return avisos que no impiden usar el plan */
        public ListaEnlazada<String> advertencias() {
            return advertencias;
        }

        /** @return {@code true} si no hubo errores */
        public boolean exitoso() {
            return errores.estaVacia();
        }
    }

    /** Prerrequisitos de una línea, guardados para la segunda pasada. */
    private static final class Pendiente {
        private final Asignatura asignatura;
        private final String texto;
        private final int linea;

        private Pendiente(Asignatura asignatura, String texto, int linea) {
            this.asignatura = asignatura;
            this.texto = texto;
            this.linea = linea;
        }
    }

    private static final int MAX_CAMPOS = 6;
    private static final String COMPONENTES = "BCLP";

    private CargadorPlan() {
    }

    /**
     * Carga un plan desde un archivo UTF-8.
     *
     * @param ruta ruta del archivo
     * @return resultado de la carga
     * @throws IOException si el archivo no se puede leer
     */
    public static Resultado cargarArchivo(String ruta) throws IOException {
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(ruta), StandardCharsets.UTF_8))) {
            return cargar(lector);
        }
    }

    /**
     * Carga un plan escrito en una cadena (útil en las pruebas).
     *
     * @param texto contenido con el mismo formato del archivo
     * @return resultado de la carga
     */
    public static Resultado cargarTexto(String texto) {
        try {
            return cargar(new BufferedReader(new StringReader(texto)));
        } catch (IOException e) {
            throw new IllegalStateException("Error imposible al leer una cadena", e);
        }
    }

    /**
     * Carga un plan desde un lector.
     *
     * @param lector origen del texto
     * @return resultado de la carga
     * @throws IOException si falla la lectura
     */
    public static Resultado cargar(BufferedReader lector) throws IOException {
        RegistroAsignaturas registro = new RegistroAsignaturas();
        ListaEnlazada<String> errores = new ListaEnlazada<>();
        ListaEnlazada<String> advertencias = new ListaEnlazada<>();
        ListaEnlazada<Pendiente> pendientes = new ListaEnlazada<>();

        int k = -1;
        int numeroLinea = 0;
        String linea;
        while ((linea = lector.readLine()) != null) {
            numeroLinea++;
            if (numeroLinea == 1 && !linea.isEmpty() && linea.charAt(0) == '﻿') {
                linea = linea.substring(1); // marca BOM que agregan algunos editores de Windows
            }
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            if (k < 0) {
                k = leerK(limpia, numeroLinea, errores);
                if (k == 0 && limpia.indexOf(';') >= 0) {
                    leerAsignatura(limpia, numeroLinea, registro, pendientes, errores);
                }
                continue;
            }
            leerAsignatura(limpia, numeroLinea, registro, pendientes, errores);
        }

        if (k < 0) {
            errores.agregarAlFinal("El archivo está vacío: falta k y las asignaturas.");
            k = 0;
        } else if (registro.cantidad() == 0) {
            errores.agregarAlFinal("El plan no tiene asignaturas.");
        }

        enlazarPrerrequisitos(registro, pendientes, errores, advertencias);
        registrarCodigosDeOptativas(registro, advertencias);
        return new Resultado(registro, k, errores, advertencias);
    }

    // ------------------------------------------------------------ primera pasada

    private static int leerK(String linea, int numero, ListaEnlazada<String> errores) {
        if (linea.indexOf(';') >= 0) {
            errores.agregarAlFinal(enLinea(numero, "falta k: la primera línea útil debe ser el máximo de asignaturas por semestre."));
            return 0;
        }
        try {
            int k = Integer.parseInt(linea);
            if (k < 1) {
                errores.agregarAlFinal(enLinea(numero, "k debe ser un entero mayor o igual a 1 (se leyó " + k + ")."));
                return 0;
            }
            return k;
        } catch (NumberFormatException e) {
            errores.agregarAlFinal(enLinea(numero, "k debe ser un entero mayor o igual a 1 (se leyó \"" + linea + "\")."));
            return 0;
        }
    }

    private static void leerAsignatura(String linea, int numero, RegistroAsignaturas registro,
                                       ListaEnlazada<Pendiente> pendientes, ListaEnlazada<String> errores) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 2) {
            errores.agregarAlFinal(enLinea(numero, "faltan campos; se espera id;nombre;prerrequisitos;créditos;componente;optativas."));
            return;
        }
        if (campos.length > MAX_CAMPOS) {
            errores.agregarAlFinal(enLinea(numero, "tiene " + campos.length + " campos y se esperan a lo sumo " + MAX_CAMPOS + "."));
            return;
        }

        String textoId = campos[0].trim();
        long id;
        try {
            id = Long.parseLong(textoId);
        } catch (NumberFormatException e) {
            errores.agregarAlFinal(enLinea(numero, "el ID \"" + textoId + "\" no es un número entero."));
            return;
        }
        if (id <= 0) {
            errores.agregarAlFinal(enLinea(numero, "el ID debe ser positivo (se leyó " + id + ")."));
            return;
        }

        String nombre = campos[1].trim();
        if (nombre.isEmpty()) {
            errores.agregarAlFinal(enLinea(numero, "la asignatura " + id + " no tiene nombre."));
            return;
        }

        int creditos = 0;
        if (campos.length >= 4 && !campos[3].trim().isEmpty()) {
            String textoCreditos = campos[3].trim();
            try {
                creditos = Integer.parseInt(textoCreditos);
            } catch (NumberFormatException e) {
                creditos = -1;
            }
            if (creditos < 0) {
                errores.agregarAlFinal(enLinea(numero, "los créditos de " + id + " deben ser un entero no negativo (se leyó \"" + textoCreditos + "\")."));
                return;
            }
        }

        char componente = ' ';
        if (campos.length >= 5 && !campos[4].trim().isEmpty()) {
            String textoComponente = campos[4].trim();
            if (textoComponente.length() != 1 || COMPONENTES.indexOf(textoComponente.charAt(0)) < 0) {
                errores.agregarAlFinal(enLinea(numero, "el componente de " + id + " debe ser B, C, L o P (se leyó \"" + textoComponente + "\")."));
                return;
            }
            componente = textoComponente.charAt(0);
        }

        Asignatura asignatura = registro.registrar(id, nombre, creditos);
        if (asignatura == null) {
            Asignatura existente = registro.buscar(id);
            errores.agregarAlFinal(enLinea(numero, "el ID " + id + " está duplicado (ya se usó en la línea "
                    + existente.lineaArchivo() + " para \"" + existente.nombre() + "\")."));
            return;
        }
        registro.fijarLineaArchivo(asignatura, numero);
        registro.fijarComponente(asignatura, componente);

        if (campos.length >= 6) {
            String[] optativas = campos[5].split("\\|");
            for (String optativa : optativas) {
                String limpia = optativa.trim();
                if (!limpia.isEmpty()) {
                    asignatura.optativas().agregarAlFinal(limpia);
                }
            }
        }

        String prerrequisitos = campos.length >= 3 ? campos[2].trim() : "";
        if (!prerrequisitos.isEmpty()) {
            pendientes.agregarAlFinal(new Pendiente(asignatura, prerrequisitos, numero));
        }
    }

    // ------------------------------------------------------------ segunda pasada

    private static void enlazarPrerrequisitos(RegistroAsignaturas registro, ListaEnlazada<Pendiente> pendientes,
                                              ListaEnlazada<String> errores, ListaEnlazada<String> advertencias) {
        // marca[i] == j + 1 si la asignatura de índice j ya tomó a la de índice i como prerrequisito:
        // así se detectan repetidos en O(1) sin recorrer la lista.
        int[] marca = new int[registro.cantidad()];
        for (ListaEnlazada.Nodo<Pendiente> nodo = pendientes.primero(); nodo != null; nodo = nodo.siguiente()) {
            Pendiente pendiente = nodo.dato();
            Asignatura asignatura = pendiente.asignatura;
            boolean requisitoLeido = false;
            String[] partes = pendiente.texto.split(",");
            for (String parte : partes) {
                String token = parte.trim();
                if (token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("cr")) {
                    if (requisitoLeido) {
                        errores.agregarAlFinal(enLinea(pendiente.linea, asignatura + " tiene más de un requisito de créditos; se admite uno."));
                    } else {
                        leerRequisitoCreditos(token, pendiente, registro, errores);
                    }
                    requisitoLeido = true;
                    continue;
                }
                long idPrerrequisito;
                try {
                    idPrerrequisito = Long.parseLong(token);
                } catch (NumberFormatException e) {
                    errores.agregarAlFinal(enLinea(pendiente.linea, "el prerrequisito \"" + token + "\" de "
                            + asignatura + " no es un código (use números, cr>=N o crC>=N)."));
                    continue;
                }
                Asignatura prerrequisito = registro.buscar(idPrerrequisito);
                if (prerrequisito == null) {
                    errores.agregarAlFinal(enLinea(pendiente.linea, "el prerrequisito " + idPrerrequisito + " de "
                            + asignatura + " no existe en el plan."));
                    continue;
                }
                if (prerrequisito == asignatura) {
                    errores.agregarAlFinal(enLinea(pendiente.linea, asignatura + " no puede ser prerrequisito de sí misma."));
                    continue;
                }
                if (marca[prerrequisito.indice()] == asignatura.indice() + 1) {
                    advertencias.agregarAlFinal(enLinea(pendiente.linea, "el prerrequisito " + idPrerrequisito
                            + " está repetido en " + asignatura + "; se cuenta una sola vez."));
                    continue;
                }
                marca[prerrequisito.indice()] = asignatura.indice() + 1;
                registro.agregarPrerrequisito(asignatura, prerrequisito);
            }
        }
    }

    /** Requisito de créditos ya interpretado. */
    private static final class Requisito {
        private final int creditos;
        private final char componente;

        private Requisito(int creditos, char componente) {
            this.creditos = creditos;
            this.componente = componente;
        }
    }

    /**
     * Interpreta {@code cr>=N} o {@code crX>=N}, donde X es el componente.
     *
     * @return el requisito, o {@code null} si está mal escrito
     */
    private static Requisito parsearRequisito(String token) {
        int signo = token.indexOf(">=");
        if (!token.startsWith("cr") || signo < 2) {
            return null;
        }
        String entreMedio = token.substring(2, signo).trim();
        char componente = Asignatura.TODOS_LOS_COMPONENTES;
        if (!entreMedio.isEmpty()) {
            if (entreMedio.length() != 1 || COMPONENTES.indexOf(entreMedio.charAt(0)) < 0) {
                return null;
            }
            componente = entreMedio.charAt(0);
        }
        try {
            int creditos = Integer.parseInt(token.substring(signo + 2).trim());
            return creditos < 0 ? null : new Requisito(creditos, componente);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String FORMA_REQUISITO = " debe escribirse cr>=N o crC>=N (C = B, C, L o P; N entero no negativo).";

    private static void leerRequisitoCreditos(String token, Pendiente pendiente, RegistroAsignaturas registro,
                                              ListaEnlazada<String> errores) {
        Requisito requisito = parsearRequisito(token);
        if (requisito == null) {
            errores.agregarAlFinal(enLinea(pendiente.linea, "el requisito \"" + token + "\" de " + pendiente.asignatura
                    + FORMA_REQUISITO));
            return;
        }
        registro.fijarRequisitoCreditos(pendiente.asignatura, requisito.creditos, requisito.componente);
    }

    /**
     * Registra en el AVL el código de cada optativa ("2016377 Cálculo
     * diferencial en una variable (4 cr)") apuntando a su casilla. Una misma
     * optativa puede cumplir dos casillas; su código queda con la primera.
     */
    private static void registrarCodigosDeOptativas(RegistroAsignaturas registro, ListaEnlazada<String> advertencias) {
        for (int i = 0; i < registro.cantidad(); i++) {
            Asignatura casilla = registro.obtener(i);
            if (!casilla.tieneOptativas()) {
                continue;
            }
            for (ListaEnlazada.Nodo<String> nodo = casilla.optativas().primero(); nodo != null; nodo = nodo.siguiente()) {
                long codigo = codigoInicial(nodo.dato());
                if (codigo <= 0 || registro.registrarAlias(codigo, casilla)) {
                    continue;
                }
                Asignatura existente = registro.buscar(codigo);
                if (existente.id() == codigo) {
                    advertencias.agregarAlFinal(enLinea(casilla.lineaArchivo(), "la optativa " + codigo
                            + " ya es una asignatura del plan; buscarla devuelve " + existente + "."));
                }
            }
        }
    }

    /**
     * @param texto optativa, por ejemplo "2016377 Cálculo diferencial en una variable (4 cr)"
     * @return el código inicial, o −1 si el texto no empieza por un número
     */
    public static long codigoInicial(String texto) {
        int fin = 0;
        while (fin < texto.length() && Character.isDigit(texto.charAt(fin))) {
            fin++;
        }
        if (fin == 0 || fin > 18 || (fin < texto.length() && texto.charAt(fin) != ' ')) {
            return -1;
        }
        return Long.parseLong(texto.substring(0, fin));
    }

    private static String enLinea(int numero, String mensaje) {
        return "Línea " + numero + ": " + mensaje;
    }
}
