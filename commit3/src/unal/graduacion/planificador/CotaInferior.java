package unal.graduacion.planificador;

/**
 * Cotas inferiores del número de semestres (RF6). Ningún plan puede usar
 * menos semestres que la cota, así que si el plan obtenido la alcanza, es
 * óptimo con certeza.
 *
 * <ul>
 *   <li><b>Cota simple</b>: max(⌈n/k⌉, H), con H la cadena más larga.</li>
 *   <li><b>Cota reforzada</b>: max sobre h de (h − 1 + ⌈N(h)/k⌉), con N(h)
 *       el número de asignaturas de altura ≥ h. Cada una de esas N(h)
 *       asignaturas deja al menos h − 1 semestres por delante, así que todas
 *       deben cursarse en los primeros T − h + 1 semestres:
 *       N(h) ≤ k·(T − h + 1). Con h = 1 se recupera ⌈n/k⌉ y con h = H se
 *       recupera H, de modo que nunca es menor que la cota simple.</li>
 * </ul>
 *
 * <p>Si el plan tiene requisitos de créditos (cr&gt;=N), estas cotas siguen
 * siendo válidas: un requisito extra nunca reduce los semestres necesarios.</p>
 */
public final class CotaInferior {

    private final int porCantidad;
    private final int porCadena;
    private final int reforzada;
    private final int alturaCritica;

    private CotaInferior(int porCantidad, int porCadena, int reforzada, int alturaCritica) {
        this.porCantidad = porCantidad;
        this.porCadena = porCadena;
        this.reforzada = reforzada;
        this.alturaCritica = alturaCritica;
    }

    /**
     * Calcula ambas cotas. O(n + H) con un conteo por altura.
     *
     * @param altura altura de cada asignatura (por índice interno)
     * @param n      número de asignaturas
     * @param k      máximo de asignaturas por semestre
     * @return las cotas
     */
    public static CotaInferior calcular(int[] altura, int n, int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k debe ser al menos 1");
        }
        if (n == 0) {
            return new CotaInferior(0, 0, 0, 0);
        }
        int alturaMaxima = 0;
        for (int i = 0; i < n; i++) {
            if (altura[i] > alturaMaxima) {
                alturaMaxima = altura[i];
            }
        }
        int[] conteo = new int[alturaMaxima + 2];
        for (int i = 0; i < n; i++) {
            conteo[altura[i]]++;
        }
        int reforzada = 0;
        int alturaCritica = 1;
        int alMenosH = 0; // N(h): se acumula de la altura mayor hacia abajo
        for (int h = alturaMaxima; h >= 1; h--) {
            alMenosH += conteo[h];
            int candidata = h - 1 + techo(alMenosH, k);
            if (candidata > reforzada) {
                reforzada = candidata;
                alturaCritica = h;
            }
        }
        return new CotaInferior(techo(n, k), alturaMaxima, reforzada, alturaCritica);
    }

    /** @return ⌈n/k⌉ */
    public int porCantidad() {
        return porCantidad;
    }

    /** @return H, la cadena de prerrequisitos más larga (en asignaturas) */
    public int porCadena() {
        return porCadena;
    }

    /** @return max(⌈n/k⌉, H) */
    public int simple() {
        return Math.max(porCantidad, porCadena);
    }

    /** @return la cota reforzada (siempre ≥ la simple) */
    public int reforzada() {
        return reforzada;
    }

    /** @return la altura h donde se alcanza la cota reforzada */
    public int alturaCritica() {
        return alturaCritica;
    }

    private static int techo(int a, int b) {
        return (a + b - 1) / b;
    }
}
