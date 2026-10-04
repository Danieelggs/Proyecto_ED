package unal.graduacion;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;

class EstructurasTest {

    @Test
    void listaMantieneOrdenDeInsercion() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregarFinal(2);
        lista.agregarFinal(3);
        lista.agregarInicio(1);
        assertEquals(3, lista.tamano());
        assertEquals("[1, 2, 3]", lista.toString());
        assertEquals(3, lista.obtener(2));
    }

    @Test
    void colaCircularCreceYRespetaFifo() {
        Cola<Integer> cola = new Cola<>();
        for (int i = 0; i < 100; i++) {
            cola.encolar(i);
            if (i % 3 == 0) {
                cola.desencolar(); // fuerza que el inicio dé la vuelta al arreglo
            }
        }
        int anterior = -1;
        while (!cola.estaVacia()) {
            int actual = cola.desencolar();
            assertTrue(actual > anterior);
            anterior = actual;
        }
    }

    @Test
    void pilaEsLifo() {
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        pila.apilar("b");
        assertEquals("b", pila.desapilar());
        assertEquals("a", pila.cima());
    }

    @Test
    void avlSeMantieneBalanceadoConClavesOrdenadas() {
        ArbolAVL<Integer, Integer> avl = new ArbolAVL<>();
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            avl.insertar(i, i); // peor caso para un ABB sin balancear
        }
        assertEquals(n, avl.tamano());
        // Un AVL tiene altura < 1.45 log2(n + 2)
        assertTrue(avl.altura() <= 1.45 * (Math.log(n + 2) / Math.log(2)));
        assertEquals(500, avl.buscar(500));
        assertNull(avl.buscar(n));
    }

    @Test
    void avlExtraeMaximosEnOrdenDescendente() {
        ArbolAVL<Integer, Integer> avl = new ArbolAVL<>();
        int[] valores = {5, 1, 9, 3, 7, 2, 8};
        for (int v : valores) {
            avl.insertar(v, v);
        }
        int anterior = Integer.MAX_VALUE;
        while (!avl.estaVacio()) {
            int max = avl.extraerMaximo();
            assertTrue(max < anterior);
            anterior = max;
        }
        assertEquals(0, avl.tamano());
    }
}
