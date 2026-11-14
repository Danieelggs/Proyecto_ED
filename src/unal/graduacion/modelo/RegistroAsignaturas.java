package unal.graduacion.modelo;

import unal.graduacion.estructuras.ArbolAVL;

/**
 * Registro del plan: todas las asignaturas y sus prerrequisitos (RF1, RF2).
 *
 * <p>Guarda cada asignatura en dos lugares:</p>
 * <ul>
 *   <li>un <b>árbol AVL por ID</b>, para encontrarla por su código en
 *       O(log n) aunque los códigos sean dispersos (2016699, 1000004...);</li>
 *   <li>un <b>arreglo por índice interno</b>, para que los algoritmos la
 *       recorran en O(1) por asignatura.</li>
 * </ul>
 */
public class RegistroAsignaturas {

    private final ArbolAVL<Long, Asignatura> porId = new ArbolAVL<>();
    private Asignatura[] porIndice;
    private int cantidad;
    private int prerrequisitos;
    private int alias;
    private long creditosTotales;

    /** Crea un registro vacío. */
    public RegistroAsignaturas() {
        this(16);
    }

    /**
     * Crea un registro con espacio reservado.
     *
     * @param capacidadEsperada número de asignaturas que se espera registrar
     */
    public RegistroAsignaturas(int capacidadEsperada) {
        porIndice = new Asignatura[Math.max(16, capacidadEsperada)];
    }

    /**
     * Registra una asignatura nueva. O(log n) por la inserción en el AVL.
     *
     * @param id       código
     * @param nombre   nombre (puede ser {@code null} en planes generados)
     * @param creditos créditos
     * @return la asignatura creada, o {@code null} si el ID ya estaba registrado
     */
    public Asignatura registrar(long id, String nombre, int creditos) {
        Asignatura nueva = new Asignatura(id, nombre, creditos, cantidad);
        if (!porId.insertar(id, nueva)) {
            return null;
        }
        if (cantidad == porIndice.length) {
            Asignatura[] mayor = new Asignatura[porIndice.length * 2];
            for (int i = 0; i < cantidad; i++) {
                mayor[i] = porIndice[i];
            }
            porIndice = mayor;
        }
        porIndice[cantidad++] = nueva;
        creditosTotales += creditos;
        return nueva;
    }

    /**
     * Busca una asignatura por su código (RF2). Si el código es de una
     * optativa, devuelve la casilla que esa optativa cumple. O(log n).
     *
     * @param id código buscado
     * @return la asignatura, o {@code null} si no existe
     */
    public Asignatura buscar(long id) {
        return porId.buscar(id);
    }

    /**
     * Devuelve la asignatura con un índice interno. O(1).
     *
     * @param indice de 0 a {@code cantidad() - 1}
     * @return la asignatura
     */
    public Asignatura obtener(int indice) {
        if (indice < 0 || indice >= cantidad) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera del registro de tamaño " + cantidad);
        }
        return porIndice[indice];
    }

    /**
     * Agrega la arista prerrequisito → asignatura. O(1).
     *
     * @param asignatura    asignatura que exige el prerrequisito
     * @param prerrequisito asignatura que debe aprobarse antes
     */
    public void agregarPrerrequisito(Asignatura asignatura, Asignatura prerrequisito) {
        asignatura.agregarPrerrequisito(prerrequisito);
        prerrequisitos++;
    }

    /**
     * Fija el requisito especial de créditos aprobados. O(1).
     *
     * @param asignatura asignatura con el requisito
     * @param creditos   créditos mínimos aprobados
     * @param componente componente cuyos créditos cuentan, o
     *                   {@link Asignatura#TODOS_LOS_COMPONENTES}
     */
    public void fijarRequisitoCreditos(Asignatura asignatura, int creditos, char componente) {
        asignatura.fijarRequisitoCreditos(creditos, componente);
    }

    /**
     * Fija el componente del plan (B, C, L o P). O(1).
     *
     * @param asignatura asignatura
     * @param componente letra del componente
     */
    public void fijarComponente(Asignatura asignatura, char componente) {
        asignatura.fijarComponente(componente);
    }

    /**
     * Registra el código de una optativa para que buscarlo devuelva la casilla
     * que cumple (RF2). Va en el mismo AVL que los IDs. O(log n).
     *
     * @param codigo  código de la optativa
     * @param casilla asignatura de la malla que la optativa cumple
     * @return {@code false} si el código ya estaba (como asignatura o como otra optativa)
     */
    public boolean registrarAlias(long codigo, Asignatura casilla) {
        if (!porId.insertar(codigo, casilla)) {
            return false;
        }
        alias++;
        return true;
    }

    /** @return códigos de optativas registrados como alias. O(1). */
    public int cantidadAlias() {
        return alias;
    }

    /**
     * Guarda la línea del archivo donde se definió la asignatura (para los mensajes de error). O(1).
     *
     * @param asignatura asignatura
     * @param linea      número de línea
     */
    public void fijarLineaArchivo(Asignatura asignatura, int linea) {
        asignatura.fijarLineaArchivo(linea);
    }

    /** @return número de asignaturas (n). O(1). */
    public int cantidad() {
        return cantidad;
    }

    /** @return número de prerrequisitos registrados (e, aristas del grafo). O(1). */
    public int cantidadPrerrequisitos() {
        return prerrequisitos;
    }

    /** @return suma de créditos del plan. O(1). */
    public long creditosTotales() {
        return creditosTotales;
    }

    /** @return el AVL por ID (para consultar su altura y rotaciones en el análisis). O(1). */
    public ArbolAVL<Long, Asignatura> indicePorId() {
        return porId;
    }
}
