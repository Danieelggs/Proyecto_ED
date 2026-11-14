package unal.graduacion.modelo;

import unal.graduacion.estructuras.ListaEnlazada;

/**
 * Una asignatura del plan (una casilla de la malla): un nodo del grafo de
 * prerrequisitos.
 *
 * <p>Las aristas se guardan en dos listas enlazadas con <b>referencias</b> a
 * otras asignaturas (nunca con IDs), para que recorrer una arista cueste O(1)
 * y no una búsqueda en el AVL. Cada asignatura tiene además un índice interno
 * de 0 a n − 1 con el que los algoritmos guardan sus datos en arreglos.</p>
 */
public class Asignatura {

    /** Componente para requisitos que cuentan créditos de cualquier componente. */
    public static final char TODOS_LOS_COMPONENTES = '*';

    private final long id;
    private final String nombre;
    private final int creditos;
    private final int indice;
    private final ListaEnlazada<Asignatura> prerrequisitos = new ListaEnlazada<>();
    private final ListaEnlazada<Asignatura> dependientes = new ListaEnlazada<>();
    private ListaEnlazada<String> optativas;
    private char componente = ' ';
    private int creditosMinimos;
    private char componenteRequisito = TODOS_LOS_COMPONENTES;
    private int lineaArchivo;

    /**
     * Crea una asignatura. Solo la crea {@link RegistroAsignaturas}.
     *
     * @param id       código de la asignatura
     * @param nombre   nombre; si es {@code null} se muestra "Asignatura &lt;id&gt;"
     * @param creditos créditos (0 si no se conocen)
     * @param indice   índice interno, de 0 a n − 1
     */
    Asignatura(long id, String nombre, int creditos, int indice) {
        this.id = id;
        this.nombre = nombre;
        this.creditos = creditos;
        this.indice = indice;
    }

    /** @return el código. O(1). */
    public long id() {
        return id;
    }

    /** @return el nombre (los planes generados no guardan nombre, para ahorrar memoria). O(1). */
    public String nombre() {
        return nombre != null ? nombre : "Asignatura " + id;
    }

    /** @return créditos de la asignatura. O(1). */
    public int creditos() {
        return creditos;
    }

    /** @return índice interno, de 0 a n − 1. O(1). */
    public int indice() {
        return indice;
    }

    /**
     * Componente del plan: B fundamentación, C disciplinar o profesional,
     * L libre elección, P nivelación; espacio si no se indicó.
     *
     * @return la letra del componente. O(1).
     */
    public char componente() {
        return componente;
    }

    /** @return lista de prerrequisitos (aristas que llegan). O(1). */
    public ListaEnlazada<Asignatura> prerrequisitos() {
        return prerrequisitos;
    }

    /** @return lista de asignaturas que la tienen como prerrequisito (aristas que salen). O(1). */
    public ListaEnlazada<Asignatura> dependientes() {
        return dependientes;
    }

    /** @return número de prerrequisitos (grado de entrada). O(1). */
    public int gradoEntrada() {
        return prerrequisitos.tamano();
    }

    /**
     * Requisito especial de créditos aprobados (los marcados con * en la
     * malla). Por ejemplo, Trabajo de grado exige 60 créditos del componente
     * disciplinar.
     *
     * @return créditos mínimos aprobados, o 0 si no tiene ese requisito. O(1).
     */
    public int creditosMinimos() {
        return creditosMinimos;
    }

    /**
     * @return componente cuyos créditos cuenta el requisito, o
     *         {@link #TODOS_LOS_COMPONENTES} si cuentan todos. O(1).
     */
    public char componenteRequisito() {
        return componenteRequisito;
    }

    /** @return el requisito de créditos en palabras, o {@code null} si no tiene. O(1). */
    public String requisitoCreditosTexto() {
        if (creditosMinimos <= 0) {
            return null;
        }
        if (componenteRequisito == TODOS_LOS_COMPONENTES) {
            return "haber aprobado " + creditosMinimos + " créditos";
        }
        return "haber aprobado " + creditosMinimos + " créditos del componente " + componenteRequisito
                + " (" + nombreComponente(componenteRequisito) + ")";
    }

    /**
     * Optativas que cumplen la misma casilla de la malla, como texto
     * "código nombre (créditos)".
     *
     * @return lista de optativas; vacía si no tiene. O(1).
     */
    public ListaEnlazada<String> optativas() {
        if (optativas == null) {
            optativas = new ListaEnlazada<>();
        }
        return optativas;
    }

    /** @return {@code true} si tiene optativas registradas. O(1). */
    public boolean tieneOptativas() {
        return optativas != null && !optativas.estaVacia();
    }

    /** @return línea del archivo donde se definió (0 si se generó en memoria). O(1). */
    public int lineaArchivo() {
        return lineaArchivo;
    }

    /**
     * @param componente letra del componente
     * @return su nombre ("fundamentación", "disciplinar"...)
     */
    public static String nombreComponente(char componente) {
        switch (componente) {
            case 'B': return "fundamentación";
            case 'C': return "disciplinar o profesional";
            case 'L': return "libre elección";
            case 'P': return "nivelación";
            default: return "sin componente";
        }
    }

    void agregarPrerrequisito(Asignatura prerrequisito) {
        prerrequisitos.agregarAlFinal(prerrequisito);
        prerrequisito.dependientes.agregarAlFinal(this);
    }

    void fijarComponente(char componente) {
        this.componente = componente;
    }

    void fijarRequisitoCreditos(int creditosMinimos, char componenteRequisito) {
        this.creditosMinimos = creditosMinimos;
        this.componenteRequisito = componenteRequisito;
    }

    void fijarLineaArchivo(int lineaArchivo) {
        this.lineaArchivo = lineaArchivo;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre();
    }
}
