package unal.graduacion.planificador;

import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Ejecuta el proceso completo sobre un plan cargado: validar con Kahn,
 * calcular alturas con la pila, planificar con el AVL de prioridad y
 * calcular las cotas inferiores.
 */
public final class Analisis {

    private final RegistroAsignaturas registro;
    private final int k;
    private final ValidadorCiclos.Resultado validacion;
    private final int[] altura;
    private final Plan plan;
    private final CotaInferior cota;

    private Analisis(RegistroAsignaturas registro, int k, ValidadorCiclos.Resultado validacion,
                     int[] altura, Plan plan, CotaInferior cota) {
        this.registro = registro;
        this.k = k;
        this.validacion = validacion;
        this.altura = altura;
        this.plan = plan;
        this.cota = cota;
    }

    /**
     * Valida y, si no hay ciclos, planifica. O(n + e) la validación y las
     * alturas; O(n log n + e) la planificación.
     *
     * @param registro plan cargado
     * @param k        máximo de asignaturas por semestre
     * @return el análisis completo (sin plan si hay ciclos)
     */
    public static Analisis ejecutar(RegistroAsignaturas registro, int k) {
        ValidadorCiclos.Resultado validacion = ValidadorCiclos.validar(registro);
        if (!validacion.esAciclico()) {
            return new Analisis(registro, k, validacion, null, null, null);
        }
        int[] altura = CalculadoraAlturas.calcular(registro, validacion.ordenTopologico());
        Plan plan = Planificador.planificar(registro, k, altura);
        CotaInferior cota = CotaInferior.calcular(altura, registro.cantidad(), k);
        return new Analisis(registro, k, validacion, altura, plan, cota);
    }

    /** @return el plan de estudios analizado */
    public RegistroAsignaturas registro() {
        return registro;
    }

    /** @return k usado */
    public int k() {
        return k;
    }

    /** @return resultado de Kahn */
    public ValidadorCiclos.Resultado validacion() {
        return validacion;
    }

    /** @return {@code true} si el plan no tiene ciclos y se pudo planificar */
    public boolean hayPlan() {
        return plan != null;
    }

    /** @return altura de cada asignatura (null si hay ciclos) */
    public int[] altura() {
        return altura;
    }

    /** @return el plan por semestres (null si hay ciclos) */
    public Plan plan() {
        return plan;
    }

    /** @return las cotas inferiores (null si hay ciclos) */
    public CotaInferior cota() {
        return cota;
    }

    /** @return {@code true} si el plan está completo e iguala la cota reforzada */
    public boolean esOptimoGarantizado() {
        return plan != null && plan.esCompleto() && plan.cantidadSemestres() == cota.reforzada();
    }

    /**
     * Una asignatura es crítica si no tiene holgura: está en el semestre s,
     * tiene altura h y s + h − 1 es el último semestre. Si se atrasa, la
     * cadena que abre termina después y la graduación se atrasa.
     *
     * @param indice índice interno de la asignatura
     * @return {@code true} si es crítica
     */
    public boolean esCritica(int indice) {
        if (plan == null || !plan.esCompleto()) {
            return false;
        }
        return plan.semestreDe(indice) + altura[indice] - 1 == plan.cantidadSemestres();
    }
}
