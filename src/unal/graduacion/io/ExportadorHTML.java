package unal.graduacion.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.Analisis;
import unal.graduacion.planificador.CotaInferior;
import unal.graduacion.planificador.Plan;
import unal.graduacion.planificador.Semestre;

/**
 * Exporta el plan a una página HTML autocontenida: una columna por semestre,
 * como la malla de la universidad. Al pasar el mouse por una asignatura se
 * resaltan sus prerrequisitos y lo que abre.
 *
 * <p>Es la representación visual opcional que sugiere el enunciado. Está
 * pensada para planes pequeños (como la malla real) y nunca se usa mientras
 * se miden tiempos.</p>
 */
public final class ExportadorHTML {

    /** Con más asignaturas que esto la página dejaría de ser legible. */
    public static final int MAX_ASIGNATURAS = 1500;

    private ExportadorHTML() {
    }

    /**
     * Escribe la página.
     *
     * @param analisis análisis con plan
     * @param fuente   nombre del archivo del plan
     * @param ruta     archivo .html de salida
     * @throws IOException si no se puede escribir
     */
    public static void exportar(Analisis analisis, String fuente, String ruta) throws IOException {
        if (!analisis.hayPlan()) {
            throw new IllegalArgumentException("El plan tiene ciclos: no hay semestres que dibujar.");
        }
        if (analisis.registro().cantidad() > MAX_ASIGNATURAS) {
            throw new IllegalArgumentException("El plan tiene " + analisis.registro().cantidad()
                    + " asignaturas; la vista HTML admite hasta " + MAX_ASIGNATURAS + ".");
        }
        try (PrintWriter salida = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) {
            salida.print(generar(analisis, fuente));
        }
    }

    /**
     * Genera el HTML como texto.
     *
     * @param analisis análisis con plan
     * @param fuente   nombre del archivo del plan
     * @return la página completa
     */
    public static String generar(Analisis analisis, String fuente) {
        RegistroAsignaturas registro = analisis.registro();
        Plan plan = analisis.plan();
        CotaInferior cota = analisis.cota();
        int[] altura = analisis.altura();
        boolean conCreditos = registro.creditosTotales() > 0;

        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            .append("<title>Plan de semestres</title>\n<style>\n").append(ESTILOS).append("</style>\n</head>\n<body>\n");

        html.append("<header><h1>").append(plan.cantidadSemestres()).append(" semestres");
        if (analisis.esOptimoGarantizado()) {
            html.append(" <span class=\"sello\">óptimo garantizado</span>");
        }
        html.append("</h1>\n<p class=\"fuente\">").append(escapar(fuente)).append("</p>\n<dl class=\"datos\">")
            .append(dato("Asignaturas", String.valueOf(registro.cantidad())))
            .append(dato("Prerrequisitos", String.valueOf(registro.cantidadPrerrequisitos())));
        if (conCreditos) {
            html.append(dato("Créditos", String.valueOf(registro.creditosTotales())));
        }
        html.append(dato("k por semestre", String.valueOf(analisis.k())))
            .append(dato("Cota inferior", cota.reforzada() + " (cantidad " + cota.porCantidad() + ", cadena " + cota.porCadena() + ")"))
            .append("</dl>\n");
        if (!plan.esCompleto()) {
            html.append("<p class=\"alerta\">").append(escapar(plan.problema())).append("</p>\n");
        }
        html.append("<p class=\"leyenda\"><span class=\"muestra critica\"></span> crítica: si se atrasa, se atrasa la graduación ")
            .append("<span class=\"muestra pre\"></span> prerrequisito <span class=\"muestra dep\"></span> la abre ")
            .append("· h = cadena más larga que abre</p>\n</header>\n<main class=\"malla\">\n");

        for (ListaEnlazada.Nodo<Semestre> nodo = plan.semestres().primero(); nodo != null; nodo = nodo.siguiente()) {
            Semestre semestre = nodo.dato();
            html.append("<section class=\"semestre\"><h2>Semestre ").append(semestre.numero()).append("<small>")
                .append(semestre.asignaturas().tamano()).append(" asig.");
            if (conCreditos) {
                html.append(" · ").append(semestre.creditos()).append(" cr");
            }
            html.append("</small></h2>\n");
            for (ListaEnlazada.Nodo<Asignatura> a = semestre.asignaturas().primero(); a != null; a = a.siguiente()) {
                tarjeta(html, a.dato(), altura, analisis.esCritica(a.dato().indice()), conCreditos);
            }
            html.append("</section>\n");
        }
        html.append("</main>\n<script>\n").append(SCRIPT).append("</script>\n</body>\n</html>\n");
        return html.toString();
    }

    private static void tarjeta(StringBuilder html, Asignatura a, int[] altura, boolean critica, boolean conCreditos) {
        html.append("<article class=\"asig").append(critica ? " critica" : "").append("\" data-id=\"").append(a.id())
            .append("\" data-pre=\"").append(ids(a.prerrequisitos())).append("\" data-dep=\"").append(ids(a.dependientes()))
            .append("\" title=\"").append(escapar(titulo(a))).append("\"><span class=\"nombre\">")
            .append(escapar(a.nombre())).append("</span><span class=\"meta\">").append(a.id());
        if (conCreditos) {
            html.append(" · ").append(a.creditos()).append(" cr");
        }
        html.append(" · h").append(altura[a.indice()]).append("</span></article>\n");
    }

    private static String titulo(Asignatura a) {
        StringBuilder texto = new StringBuilder(a.nombre());
        if (a.componente() != ' ') {
            texto.append("\nComponente: ").append(Asignatura.nombreComponente(a.componente()));
        }
        texto.append("\nPrerrequisitos: ").append(nombres(a.prerrequisitos()));
        if (a.creditosMinimos() > 0) {
            texto.append("\nRequisito especial: ").append(a.requisitoCreditosTexto());
        }
        if (a.tieneOptativas()) {
            texto.append("\nOptativas: ");
            boolean primera = true;
            for (ListaEnlazada.Nodo<String> n = a.optativas().primero(); n != null; n = n.siguiente()) {
                texto.append(primera ? "" : "; ").append(n.dato());
                primera = false;
            }
        }
        return texto.toString();
    }

    private static String nombres(ListaEnlazada<Asignatura> lista) {
        if (lista.estaVacia()) {
            return "ninguno";
        }
        StringBuilder texto = new StringBuilder();
        for (ListaEnlazada.Nodo<Asignatura> n = lista.primero(); n != null; n = n.siguiente()) {
            texto.append(texto.length() == 0 ? "" : ", ").append(n.dato().nombre());
        }
        return texto.toString();
    }

    private static String ids(ListaEnlazada<Asignatura> lista) {
        StringBuilder texto = new StringBuilder();
        for (ListaEnlazada.Nodo<Asignatura> n = lista.primero(); n != null; n = n.siguiente()) {
            texto.append(texto.length() == 0 ? "" : " ").append(n.dato().id());
        }
        return texto.toString();
    }

    private static String dato(String nombre, String valor) {
        return "<div><dt>" + escapar(nombre) + "</dt><dd>" + escapar(valor) + "</dd></div>";
    }

    private static String escapar(String texto) {
        StringBuilder salida = new StringBuilder(texto.length() + 16);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '&': salida.append("&amp;"); break;
                case '<': salida.append("&lt;"); break;
                case '>': salida.append("&gt;"); break;
                case '"': salida.append("&quot;"); break;
                case '\n': salida.append("&#10;"); break;
                default: salida.append(c);
            }
        }
        return salida.toString();
    }

    private static final String ESTILOS = String.join("\n",
        ":root{--fondo:#f6f5f2;--panel:#ffffff;--texto:#1d1c1a;--suave:#6b6863;--borde:#dedbd5;",
        "--acento:#1f5fbf;--acento-tenue:#e3ecfa;--pre:#b4561b;--pre-tenue:#fbeadf;--dep:#2c7a4b;--dep-tenue:#e1f2e7;}",
        "@media (prefers-color-scheme:dark){:root{--fondo:#161615;--panel:#21201e;--texto:#ecebe8;--suave:#a19d97;",
        "--borde:#393733;--acento:#7aa7ec;--acento-tenue:#1e2b40;--pre:#e9945e;--pre-tenue:#3a2416;--dep:#6cc591;--dep-tenue:#173323;}}",
        "*{box-sizing:border-box}body{margin:0;background:var(--fondo);color:var(--texto);",
        "font:14px/1.4 system-ui,-apple-system,'Segoe UI',Roboto,sans-serif}",
        "header{padding:24px 24px 8px}h1{margin:0;font-size:26px;display:flex;gap:12px;align-items:center;flex-wrap:wrap}",
        ".sello{font-size:12px;font-weight:600;color:var(--acento);background:var(--acento-tenue);padding:4px 10px;border-radius:999px}",
        ".fuente{margin:4px 0 12px;color:var(--suave)}",
        ".datos{display:flex;flex-wrap:wrap;gap:8px 24px;margin:0 0 12px}.datos div{display:flex;flex-direction:column}",
        ".datos dt{font-size:12px;color:var(--suave)}.datos dd{margin:0;font-weight:600}",
        ".alerta{color:var(--pre);font-weight:600}",
        ".leyenda{color:var(--suave);font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap}",
        ".muestra{display:inline-block;width:12px;height:12px;border-radius:3px;border:2px solid var(--borde)}",
        ".muestra.critica{border-color:var(--acento);background:var(--acento-tenue)}",
        ".muestra.pre{border-color:var(--pre);background:var(--pre-tenue)}.muestra.dep{border-color:var(--dep);background:var(--dep-tenue)}",
        ".malla{display:flex;gap:10px;padding:8px 24px 32px;overflow-x:auto;align-items:flex-start}",
        ".semestre{flex:1 0 146px;max-width:220px;display:flex;flex-direction:column;gap:8px}",
        ".semestre h2{font-size:14px;margin:0 0 4px;display:flex;flex-direction:column}",
        ".semestre h2 small{font-weight:400;color:var(--suave);font-size:12px}",
        ".asig{background:var(--panel);border:2px solid var(--borde);border-radius:8px;padding:8px 10px;min-height:64px;",
        "display:flex;flex-direction:column;justify-content:space-between;gap:6px;cursor:default;transition:opacity .15s}",
        ".asig.critica{border-color:var(--acento)}",
        ".nombre{font-weight:600;font-size:13px}.meta{color:var(--suave);font-size:11px}",
        ".malla.enfoque .asig{opacity:.35}.malla.enfoque .asig.activa,.malla.enfoque .asig.pre,.malla.enfoque .asig.dep{opacity:1}",
        ".asig.activa{box-shadow:0 0 0 2px var(--texto) inset}",
        ".asig.pre{border-color:var(--pre);background:var(--pre-tenue)}.asig.dep{border-color:var(--dep);background:var(--dep-tenue)}",
        "@media (max-width:600px){header{padding:16px}.malla{padding:8px 16px 24px}.semestre{flex-basis:160px}}",
        "");

    private static final String SCRIPT = String.join("\n",
        "const malla = document.querySelector('.malla');",
        "const porId = new Map();",
        "document.querySelectorAll('.asig').forEach(a => porId.set(a.dataset.id, a));",
        "function marcar(lista, clase) {",
        "  (lista || '').split(' ').filter(Boolean).forEach(id => { const t = porId.get(id); if (t) t.classList.add(clase); });",
        "}",
        "function limpiar() {",
        "  malla.classList.remove('enfoque');",
        "  document.querySelectorAll('.asig').forEach(a => a.classList.remove('activa', 'pre', 'dep'));",
        "}",
        "document.querySelectorAll('.asig').forEach(a => {",
        "  a.addEventListener('mouseenter', () => {",
        "    limpiar(); malla.classList.add('enfoque'); a.classList.add('activa');",
        "    marcar(a.dataset.pre, 'pre'); marcar(a.dataset.dep, 'dep');",
        "  });",
        "  a.addEventListener('mouseleave', limpiar);",
        "});",
        "");
}
