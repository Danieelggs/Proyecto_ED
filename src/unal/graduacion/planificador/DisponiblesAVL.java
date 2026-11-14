package unal.graduacion.planificador;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.modelo.Asignatura;

/**
 * Asignaturas disponibles en un AVL ordenado por (altura, ID).
 * Agregar y extraer la mejor cuestan O(log n), sin reordenar nada entre semestres.
 */
public final class DisponiblesAVL implements Disponibles {

    private final ArbolAVL<ClavePrioridad, Asignatura> arbol = new ArbolAVL<>();
    private final int[] altura;

    /**
     * @param altura altura de cada asignatura, por índice interno
     */
    public DisponiblesAVL(int[] altura) {
        this.altura = altura;
    }

    /** O(log n). */
    @Override
    public void agregar(Asignatura asignatura) {
        arbol.insertar(new ClavePrioridad(altura[asignatura.indice()], asignatura.id()), asignatura);
    }

    /** O(log n). */
    @Override
    public Asignatura extraerMejor() {
        return arbol.extraerMaximo();
    }

    @Override
    public int tamano() {
        return arbol.tamano();
    }

    @Override
    public boolean estaVacia() {
        return arbol.estaVacio();
    }

    @Override
    public String nombre() {
        return "AVL";
    }
}
