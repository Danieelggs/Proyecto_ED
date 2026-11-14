package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Calcula la altura de cada asignatura: cuántas asignaturas tiene la cadena de
 * prerrequisitos más larga que empieza en ella, contándola a ella misma. Una
 * asignatura sin dependientes tiene altura 1.
 *
 * <p>La altura dice cuántos semestres hacen falta, como mínimo, desde que se
 * cursa esa asignatura hasta terminar todo lo que ella abre. Por eso el
 * planificador cursa primero las de mayor altura (algoritmo de Hu).</p>
 *
 * <p>Se usa la pila que dejó Kahn: al desapilar, cada asignatura sale después
 * de todos sus dependientes, cuyas alturas ya están calculadas. O(n + e).</p>
 */
public final class CalculadoraAlturas {

    private CalculadoraAlturas() {
    }

    /**
     * Calcula las alturas. Vacía la pila que recibe.
     *
     * @param registro        plan validado (sin ciclos)
     * @param ordenTopologico pila de Kahn, con el orden topológico
     * @return altura de cada asignatura, por índice interno
     */
    public static int[] calcular(RegistroAsignaturas registro, Pila<Asignatura> ordenTopologico) {
        int[] altura = new int[registro.cantidad()];
        while (!ordenTopologico.estaVacia()) {
            Asignatura actual = ordenTopologico.desapilar();
            int mayor = 0;
            for (ListaEnlazada.Nodo<Asignatura> nodo = actual.dependientes().primero(); nodo != null; nodo = nodo.siguiente()) {
                int alturaDependiente = altura[nodo.dato().indice()];
                if (alturaDependiente > mayor) {
                    mayor = alturaDependiente;
                }
            }
            altura[actual.indice()] = mayor + 1;
        }
        return altura;
    }
}
