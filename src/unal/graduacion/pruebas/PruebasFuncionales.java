package unal.graduacion.pruebas;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;

/**
 * Pruebas funcionales con respuesta conocida. No dependen de JUnit, así que
 * corren con el mismo {@code javac} del proyecto:
 * <pre>java -cp out unal.graduacion.pruebas.PruebasFuncionales</pre>
 * o, con el programa completo, {@code java -cp out unal.graduacion.Main --pruebas}.
 * Deben ejecutarse desde la raíz del repositorio (usan los archivos de datos/).
 */
public final class PruebasFuncionales {

    private PrintStream out;
    private int aprobadas;
    private int fallidas;

    private PruebasFuncionales(PrintStream out) {
        this.out = out;
    }

    /**
     * Corre todas las pruebas.
     *
     * @param out salida
     * @return número de pruebas fallidas
     */
    public static int ejecutar(PrintStream out) {
        PruebasFuncionales p = new PruebasFuncionales(out);
        p.grupo("Lista enlazada", p::lista);
        p.grupo("Cola circular", p::cola);
        p.grupo("Pila", p::pila);
        out.println();
        out.println("Resultado: " + p.aprobadas + " verificaciones correctas, " + p.fallidas + " fallidas.");
        return p.fallidas;
    }

    /** @param args no se usan */
    public static void main(String[] args) {
        System.exit(ejecutar(System.out) == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ estructuras

    private void lista() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        verificar(lista.estaVacia(), "una lista nueva está vacía");
        lista.agregarAlFinal("b");
        lista.agregarAlFinal("c");
        lista.agregarAlInicio("a");
        verificar(lista.tamano() == 3 && "a".equals(lista.obtener(0)) && "c".equals(lista.obtener(2)), "agrega al inicio y al final");
        verificar(lista.contiene("b") && !lista.contiene("z"), "contiene");
        verificar(lista.eliminar("c") && "b".equals(lista.obtenerUltimo()), "eliminar el último actualiza la cola de la lista");
        lista.agregarAlFinal("d");
        verificar("d".equals(lista.obtenerUltimo()) && lista.tamano() == 3, "agregar después de eliminar el último");
        verificar("a".equals(lista.eliminarPrimero()) && "b".equals(lista.obtenerPrimero()), "eliminar el primero");
        lista.limpiar();
        verificar(lista.estaVacia() && lista.primero() == null, "limpiar");
    }

    private void cola() {
        Cola<Integer> cola = new Cola<>(4);
        for (int i = 1; i <= 3; i++) {
            cola.encolar(i);
        }
        verificar(cola.desencolar() == 1 && cola.desencolar() == 2, "FIFO");
        for (int i = 4; i <= 8; i++) {
            cola.encolar(i); // da la vuelta al arreglo y obliga a crecer
        }
        boolean enOrden = true;
        for (int esperado = 3; esperado <= 8; esperado++) {
            enOrden &= cola.desencolar() == esperado;
        }
        verificar(enOrden && cola.estaVacia(), "conserva el orden al dar la vuelta y al crecer");
        verificar(lanza(cola::desencolar), "desencolar una cola vacía lanza excepción");
    }

    private void pila() {
        Pila<Integer> pila = new Pila<>(2);
        for (int i = 1; i <= 100; i++) {
            pila.apilar(i);
        }
        boolean lifo = true;
        for (int esperado = 100; esperado >= 1; esperado--) {
            lifo &= pila.desapilar() == esperado;
        }
        verificar(lifo && pila.estaVacia(), "LIFO con crecimiento del arreglo");
        verificar(lanza(pila::desapilar), "desapilar una pila vacía lanza excepción");
    }

    // ------------------------------------------------------------------ utilidades

    private void grupo(String nombre, Runnable prueba) {
        out.println();
        out.println(nombre);
        try {
            prueba.run();
        } catch (RuntimeException e) {
            fallidas++;
            out.println("  FALLA  excepción inesperada: " + e);
        }
    }

    private void verificar(boolean condicion, String descripcion) {
        if (condicion) {
            aprobadas++;
            out.println("  ok     " + descripcion);
        } else {
            fallidas++;
            out.println("  FALLA  " + descripcion);
        }
    }

    private static boolean contiene(ListaEnlazada<String> mensajes, String fragmento) {
        for (ListaEnlazada.Nodo<String> nodo = mensajes.primero(); nodo != null; nodo = nodo.siguiente()) {
            if (nodo.dato().contains(fragmento)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
