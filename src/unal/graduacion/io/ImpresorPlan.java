package unal.graduacion.io;

import java.io.PrintStream;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.Analisis;
import unal.graduacion.planificador.CotaInferior;
import unal.graduacion.planificador.Plan;
import unal.graduacion.planificador.Semestre;
import unal.graduacion.planificador.ValidadorCiclos;

/**
 * Muestra en consola los resultados: errores de carga, ciclos (RF3), el plan
 * por semestres (RF5), las cotas (RF6) y la ficha de una asignatura (RF2).
 *
 * <p>Solo usa caracteres que existen en las consolas de Windows (tildes y el
 * punto medio), para que se vean bien al grabar el video.</p>
 */
public final class ImpresorPlan {

    /** Con más asignaturas que esto, solo se muestran los primeros semestres. */
    private static final int MAX_ASIGNATURAS_DETALLE = 300;
    private static final int SEMESTRES_RESUMEN = 3;
    private static final int MAX_MENSAJES = 50;

    private ImpresorPlan() {
    }

    /**
     * Muestra los errores y advertencias de una carga.
     *
     * @param resultado resultado del cargador
     * @param fuente    nombre del archivo
     * @param out       salida
     */
    public static void imprimirCarga(CargadorPlan.Resultado resultado, String fuente, PrintStream out) {
        if (!resultado.exitoso()) {
            out.println("No se pudo cargar " + fuente + ": " + resultado.errores().tamano() + " error(es).");
            imprimirMensajes(resultado.errores(), out);
        }
        if (!resultado.advertencias().estaVacia()) {
            out.println("Advertencias (" + resultado.advertencias().tamano() + "):");
            imprimirMensajes(resultado.advertencias(), out);
        }
    }

    /**
     * Muestra el análisis completo: resumen, cotas y semestres, o el ciclo.
     *
     * @param analisis resultado del proceso
     * @param fuente   nombre del archivo o descripción del plan
     * @param out      salida
     */
    public static void imprimir(Analisis analisis, String fuente, PrintStream out) {
        RegistroAsignaturas registro = analisis.registro();
        out.println();
        out.println("Plan: " + fuente);
        out.println(resumenRegistro(registro) + " · k = " + analisis.k());

        if (!analisis.hayPlan()) {
            imprimirCiclo(analisis.validacion(), out);
            return;
        }

        Plan plan = analisis.plan();
        CotaInferior cota = analisis.cota();
        int semestres = plan.cantidadSemestres();
        out.println();
        if (!plan.esCompleto()) {
            out.println("No se pudo completar el plan. " + plan.problema());
        }
        out.println("Semestres necesarios: " + semestres);
        out.println("Cota inferior:        " + cota.reforzada() + "   (" + detalleCota(registro.cantidad(), analisis.k(), cota) + ")");
        if (analisis.esOptimoGarantizado()) {
            out.println("Resultado:            óptimo garantizado; ningún plan termina en menos de " + semestres + " semestres.");
        } else if (plan.esCompleto()) {
            out.println("Resultado:            plan válido; podría existir uno con hasta " + (semestres - cota.reforzada())
                    + " semestre(s) menos (para k cualquiera el problema es NP-difícil).");
        }

        boolean conCreditos = registro.creditosTotales() > 0;
        out.println();
        out.println("h = altura: cuántas asignaturas tiene la cadena más larga que abre.  * = crítica: si se atrasa, se atrasa la graduación.");
        boolean detalle = registro.cantidad() <= MAX_ASIGNATURAS_DETALLE;
        int mostrados = 0;
        for (ListaEnlazada.Nodo<Semestre> nodo = plan.semestres().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (!detalle && mostrados == SEMESTRES_RESUMEN) {
                out.println();
                out.println("... y " + (semestres - mostrados) + " semestres más"
                        + (registro.cantidad() <= ExportadorHTML.MAX_ASIGNATURAS ? " (use --html para ver el plan completo)." : "."));
                break;
            }
            imprimirSemestre(nodo.dato(), analisis, conCreditos, out);
            mostrados++;
        }
    }

    /**
     * Muestra la ficha de una asignatura (RF2).
     *
     * @param asignatura asignatura encontrada por su código
     * @param analisis   análisis del plan, o {@code null} si aún no se planificó
     * @param out        salida
     */
    public static void describirAsignatura(Asignatura asignatura, Analisis analisis, PrintStream out) {
        describirAsignatura(asignatura, asignatura.id(), analisis, out);
    }

    /**
     * Muestra la ficha de una asignatura buscada por un código que puede ser
     * el suyo o el de una de sus optativas (RF2).
     *
     * @param asignatura    casilla encontrada
     * @param codigoBuscado código que escribió el usuario
     * @param analisis      análisis del plan, o {@code null} si aún no se planificó
     * @param out           salida
     */
    public static void describirAsignatura(Asignatura asignatura, long codigoBuscado, Analisis analisis, PrintStream out) {
        out.println();
        if (codigoBuscado != asignatura.id()) {
            out.println("El código " + codigoBuscado + " es una optativa: " + optativaConCodigo(asignatura, codigoBuscado)
                    + ". Cumple la casilla:");
        }
        String encabezado = asignatura.toString();
        if (asignatura.creditos() > 0) {
            encabezado += " · " + asignatura.creditos() + " créditos";
        }
        if (asignatura.componente() != ' ') {
            encabezado += " · componente " + asignatura.componente() + " (" + Asignatura.nombreComponente(asignatura.componente()) + ")";
        }
        out.println(encabezado);
        out.println("  Prerrequisitos: " + listar(asignatura.prerrequisitos(), "ninguno"));
        out.println("  Abre:           " + listar(asignatura.dependientes(), "ninguna"));
        if (asignatura.creditosMinimos() > 0) {
            out.println("  Requisito especial: " + asignatura.requisitoCreditosTexto() + ".");
        }
        if (asignatura.tieneOptativas()) {
            out.println("  Optativas que cumplen esta casilla:");
            for (ListaEnlazada.Nodo<String> nodo = asignatura.optativas().primero(); nodo != null; nodo = nodo.siguiente()) {
                out.println("    - " + nodo.dato());
            }
        }
        if (analisis != null && analisis.hayPlan()) {
            int i = asignatura.indice();
            out.println("  Altura: " + analisis.altura()[i] + " · semestre en el plan: " + analisis.plan().semestreDe(i)
                    + (analisis.esCritica(i) ? " · crítica" : " · tiene holgura"));
        }
    }

    /**
     * Muestra un ciclo y las asignaturas bloqueadas (RF3).
     *
     * @param validacion resultado de Kahn con ciclo
     * @param out        salida
     */
    public static void imprimirCiclo(ValidadorCiclos.Resultado validacion, PrintStream out) {
        out.println();
        out.println("El plan es imposible: hay un ciclo de prerrequisitos.");
        StringBuilder ciclo = new StringBuilder();
        Asignatura primera = null;
        for (ListaEnlazada.Nodo<Asignatura> nodo = validacion.ciclo().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (primera == null) {
                primera = nodo.dato();
            }
            ciclo.append(nodo.dato()).append(" -> ");
        }
        ciclo.append(primera);
        out.println("Ciclo (cada una es prerrequisito de la siguiente):");
        out.println("  " + ciclo);
        out.println("Asignaturas bloqueadas, en el ciclo o detrás de él (" + validacion.bloqueadas().tamano() + "):");
        int mostradas = 0;
        for (ListaEnlazada.Nodo<Asignatura> nodo = validacion.bloqueadas().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (mostradas == MAX_MENSAJES) {
                out.println("  ... y " + (validacion.bloqueadas().tamano() - mostradas) + " más.");
                break;
            }
            out.println("  " + nodo.dato());
            mostradas++;
        }
    }

    /**
     * @param registro plan cargado
     * @return "49 asignaturas · 51 prerrequisitos · 165 créditos"
     */
    public static String resumenRegistro(RegistroAsignaturas registro) {
        String texto = cantidad(registro.cantidad(), "asignatura", "asignaturas") + " · "
                + cantidad(registro.cantidadPrerrequisitos(), "prerrequisito", "prerrequisitos");
        if (registro.creditosTotales() > 0) {
            texto += " · " + registro.creditosTotales() + " créditos";
        }
        return texto;
    }

    /**
     * @param n        cantidad
     * @param singular palabra en singular
     * @param plural   palabra en plural
     * @return "1 asignatura" o "3 asignaturas"
     */
    public static String cantidad(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /**
     * @param n    asignaturas
     * @param k    máximo por semestre
     * @param cota cotas calculadas
     * @return explicación corta de la cota
     */
    public static String detalleCota(int n, int k, CotaInferior cota) {
        String texto = "por cantidad " + n + "/" + k + " -> " + cota.porCantidad() + ", por cadena " + cota.porCadena();
        if (cota.reforzada() > cota.simple()) {
            texto += ", reforzada con altura " + cota.alturaCritica() + " -> " + cota.reforzada();
        }
        return texto;
    }

    // ------------------------------------------------------------------ internos

    private static void imprimirSemestre(Semestre semestre, Analisis analisis, boolean conCreditos, PrintStream out) {
        out.println();
        int cantidad = semestre.asignaturas().tamano();
        String cabecera = "Semestre " + semestre.numero() + " · " + cantidad + (cantidad == 1 ? " asignatura" : " asignaturas");
        if (conCreditos) {
            cabecera += " · " + semestre.creditos() + " créditos · acumulados " + semestre.creditosAcumulados();
        }
        out.println(cabecera);
        int[] altura = analisis.altura();
        for (ListaEnlazada.Nodo<Asignatura> nodo = semestre.asignaturas().primero(); nodo != null; nodo = nodo.siguiente()) {
            Asignatura a = nodo.dato();
            String marca = analisis.esCritica(a.indice()) ? "*" : " ";
            if (conCreditos) {
                out.println(String.format("  %s %d cr  h%-2d %s", marca, a.creditos(), altura[a.indice()], a));
            } else {
                out.println(String.format("  %s h%-2d %s", marca, altura[a.indice()], a));
            }
        }
    }

    private static String optativaConCodigo(Asignatura casilla, long codigo) {
        for (ListaEnlazada.Nodo<String> nodo = casilla.optativas().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (CargadorPlan.codigoInicial(nodo.dato()) == codigo) {
                return nodo.dato();
            }
        }
        return String.valueOf(codigo);
    }

    private static String listar(ListaEnlazada<Asignatura> asignaturas, String siVacia) {
        if (asignaturas.estaVacia()) {
            return siVacia;
        }
        StringBuilder texto = new StringBuilder();
        for (ListaEnlazada.Nodo<Asignatura> nodo = asignaturas.primero(); nodo != null; nodo = nodo.siguiente()) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(nodo.dato());
        }
        return texto.toString();
    }

    private static void imprimirMensajes(ListaEnlazada<String> mensajes, PrintStream out) {
        int mostrados = 0;
        for (ListaEnlazada.Nodo<String> nodo = mensajes.primero(); nodo != null; nodo = nodo.siguiente()) {
            if (mostrados == MAX_MENSAJES) {
                out.println("  ... y " + (mensajes.tamano() - mostrados) + " más.");
                return;
            }
            out.println("  " + nodo.dato());
            mostrados++;
        }
    }
}
