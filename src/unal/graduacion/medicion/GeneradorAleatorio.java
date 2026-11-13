package unal.graduacion.medicion;

/**
 * Generador de números pseudoaleatorios SplitMix64.
 *
 * <p>Se implementa aquí para no depender de {@code java.util} y para que una
 * misma semilla produzca siempre los mismos planes: así las mediciones se
 * pueden repetir y los planes de prueba no tienen que subirse al repositorio.</p>
 */
public final class GeneradorAleatorio {

    private long estado;

    /**
     * @param semilla semilla; la misma semilla da la misma secuencia
     */
    public GeneradorAleatorio(long semilla) {
        this.estado = semilla;
    }

    /** @return siguiente número de 64 bits. O(1). */
    public long siguiente() {
        long z = (estado += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Entero uniforme en [0, limite). O(1).
     *
     * @param limite cota superior exclusiva, positiva
     * @return el entero
     */
    public int entero(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite debe ser positivo");
        }
        // Multiplicación de 64 bits: sesgo despreciable y sin divisiones.
        return (int) (((siguiente() >>> 32) * limite) >>> 32);
    }
}
