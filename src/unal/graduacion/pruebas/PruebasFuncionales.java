package unal.graduacion.pruebas;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.ArbolBST;
import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.medicion.GeneradorAleatorio;

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
        p.grupo("Árbol AVL", p::avl);
        p.grupo("BST sin balanceo", p::bst);
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

    private void avl() {
        ArbolAVL<Long, String> avl = new ArbolAVL<>();
        int n = 10_000;
        for (long i = 1; i <= n; i++) {
            avl.insertar(i, "v" + i); // claves ordenadas: el peor caso para un BST
        }
        double maxima = 1.4405 * (Math.log(n + 2) / Math.log(2)) - 0.3277;
        verificar(avl.esValido(), "insertar 10.000 claves ordenadas deja un AVL válido");
        verificar(avl.altura() <= maxima, "altura " + avl.altura() + " <= 1,44·log2(n + 2)");
        verificar(!avl.insertar(5L, "otra") && avl.tamano() == n, "no admite claves repetidas");
        verificar("v777".equals(avl.buscar(777L)) && avl.buscar(0L) == null, "buscar");
        for (long i = 2; i <= n; i += 2) {
            avl.eliminar(i);
        }
        verificar(avl.esValido() && avl.tamano() == n / 2 && !avl.contiene(500L) && avl.contiene(501L), "eliminar la mitad");
        verificar(avl.extraerMaximo().equals("v" + (n - 1)) && avl.extraerMinimo().equals("v1"), "extraer máximo y mínimo");

        // Operaciones aleatorias contra un arreglo de referencia
        ArbolAVL<Long, Long> aleatorio = new ArbolAVL<>();
        boolean[] presente = new boolean[2000];
        GeneradorAleatorio azar = new GeneradorAleatorio(7);
        boolean coincide = true;
        for (int paso = 0; paso < 20_000; paso++) {
            int clave = azar.entero(presente.length);
            if (azar.entero(3) == 0) {
                Long quitado = aleatorio.eliminar((long) clave);
                coincide &= (quitado != null) == presente[clave];
                presente[clave] = false;
            } else {
                coincide &= aleatorio.insertar((long) clave, (long) clave) != presente[clave];
                presente[clave] = true;
            }
        }
        int cuenta = 0;
        for (boolean p : presente) {
            cuenta += p ? 1 : 0;
        }
        verificar(coincide && aleatorio.esValido() && aleatorio.tamano() == cuenta, "20.000 inserciones y eliminaciones aleatorias");

        ListaEnlazada<Long> claves = new ListaEnlazada<>();
        aleatorio.clavesEnOrden(claves);
        boolean ascendente = true;
        Long anterior = null;
        for (ListaEnlazada.Nodo<Long> nodo = claves.primero(); nodo != null; nodo = nodo.siguiente()) {
            ascendente &= anterior == null || anterior < nodo.dato();
            anterior = nodo.dato();
        }
        verificar(ascendente, "recorrido en orden ascendente");
    }

    private void bst() {
        ArbolBST<Long, String> bst = new ArbolBST<>();
        for (long i = 1; i <= 2000; i++) {
            bst.insertar(i, "v");
        }
        verificar(bst.altura() == 2000, "con claves ordenadas degenera en una lista (altura = n)");
        verificar(bst.buscar(1999L) != null && bst.buscar(0L) == null, "buscar en el BST degenerado");
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
