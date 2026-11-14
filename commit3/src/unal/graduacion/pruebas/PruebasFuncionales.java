package unal.graduacion.pruebas;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import unal.graduacion.estructuras.ArbolAVL;
import unal.graduacion.estructuras.ArbolBST;
import unal.graduacion.estructuras.Cola;
import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.estructuras.Pila;
import unal.graduacion.io.CargadorPlan;
import unal.graduacion.medicion.GeneradorAleatorio;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.Analisis;
import unal.graduacion.planificador.CalculadoraAlturas;
import unal.graduacion.planificador.CotaInferior;
import unal.graduacion.planificador.DisponiblesLista;
import unal.graduacion.planificador.Plan;
import unal.graduacion.planificador.Planificador;
import unal.graduacion.planificador.Semestre;
import unal.graduacion.planificador.ValidadorCiclos;
import unal.graduacion.planificador.VerificadorPlan;

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
        p.grupo("Registro de asignaturas desde el archivo (RF1, RF2)", p::carga);
        p.grupo("Ciclos (RF3)", p::ciclos);
        p.grupo("Planificador y cotas (RF4 a RF6)", p::planificador);
        p.grupo("Requisito de créditos", p::creditos);
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

    // ------------------------------------------------------------------ carga

    private void carga() {
        CargadorPlan.Resultado ejemplo = cargar("datos/plan_ejemplo.txt");
        if (ejemplo != null) {
            RegistroAsignaturas r = ejemplo.registro();
            verificar(ejemplo.exitoso() && ejemplo.k() == 3, "carga el ejemplo del README con k = 3");
            verificar(r.cantidad() == 6 && r.cantidadPrerrequisitos() == 4, "6 asignaturas y 4 prerrequisitos");
            verificar(prerrequisitosDe(r, 107).equals("105,106") && prerrequisitosDe(r, 104).equals("101")
                    && prerrequisitosDe(r, 101).isEmpty(), "registra cada asignatura con su ID y su lista de prerrequisitos");
            boolean todas = true;
            for (long id : new long[] {101, 102, 104, 105, 106, 107}) {
                todas &= r.buscar(id) != null && r.buscar(id).id() == id;
            }
            verificar(todas && r.buscar(103) == null, "cada ID del archivo se encuentra en el AVL; uno que no está da null (RF2)");
            Asignatura ed = r.buscar(107);
            verificar(ed != null && ed.gradoEntrada() == 2 && "Estructuras de Datos".equals(ed.nombre()), "acceso por ID (RF2)");
        }

        CargadorPlan.Resultado errores = cargar("datos/errores.txt");
        if (errores != null) {
            verificar(errores.errores().tamano() == 5, "detecta los 5 errores del archivo de errores (hay " + errores.errores().tamano() + ")");
            verificar(errores.advertencias().tamano() == 1, "el prerrequisito repetido es solo una advertencia");
            verificar(contiene(errores.errores(), "duplicado"), "ID duplicado");
            verificar(contiene(errores.errores(), "no existe"), "prerrequisito inexistente");
            verificar(contiene(errores.errores(), "sí misma"), "una asignatura no puede exigirse a sí misma");
            verificar(contiene(errores.errores(), "no es un número"), "ID no numérico");
            verificar(contiene(errores.errores(), "créditos"), "créditos inválidos");
            Asignatura once = errores.registro().buscar(11);
            verificar(once != null && once.gradoEntrada() == 1, "acepta prerrequisitos definidos más abajo (dos pasadas)");
        }

        verificar(!CargadorPlan.cargarTexto("1;A;\n2;B;1\n").exitoso(), "rechaza un archivo sin k");
        verificar(!CargadorPlan.cargarTexto("0\n1;A;\n").exitoso(), "rechaza k = 0");
        verificar(!CargadorPlan.cargarTexto("2\n1\n").exitoso(), "rechaza una línea con un solo campo");
        verificar(!CargadorPlan.cargarTexto("2\n1;A;cr>=x\n").exitoso(), "rechaza un requisito de créditos mal escrito");
        CargadorPlan.Resultado bom = CargadorPlan.cargarTexto("﻿2\n1;A;\n");
        verificar(bom.exitoso() && bom.k() == 2, "ignora la marca BOM de Windows");
        CargadorPlan.Resultado completa = CargadorPlan.cargarTexto("2\n1;A;;3;C;77 Opción X (4 cr)|Opción Y\n2;B;1,crC>=3;4;B\n");
        Asignatura b = completa.registro().buscar(2);
        verificar(completa.exitoso() && b.creditos() == 4 && b.componente() == 'B', "lee créditos y componente");
        verificar(b.creditosMinimos() == 3 && b.componenteRequisito() == 'C', "lee el requisito crC>=N");
        verificar(completa.registro().buscar(1).optativas().tamano() == 2, "lee las optativas separadas por |");
        verificar(completa.registro().buscar(77) == completa.registro().buscar(1), "el código de una optativa encuentra su casilla");
        verificar(!CargadorPlan.cargarTexto("2\n1;A;;3;X\n").exitoso(), "rechaza un componente distinto de B, C, L o P");
        verificar(!CargadorPlan.cargarTexto("2\n1;A;cr>=3,crC>=2;3;C\n").exitoso(), "rechaza dos requisitos de créditos en una asignatura");
    }

    private void ciclos() {
        CargadorPlan.Resultado carga = cargar("datos/ciclo.txt");
        if (carga == null) {
            return;
        }
        ValidadorCiclos.Resultado v = ValidadorCiclos.validar(carga.registro());
        verificar(!v.esAciclico(), "detecta el ciclo");
        verificar(v.bloqueadas().tamano() == 4, "bloqueadas: las 3 del ciclo y la que depende de él");
        verificar(v.ciclo().tamano() == 3, "el ciclo tiene 3 asignaturas");
        boolean cerrado = true;
        ListaEnlazada<Asignatura> ciclo = v.ciclo();
        for (int i = 0; i < ciclo.tamano(); i++) {
            Asignatura actual = ciclo.obtener(i);
            Asignatura siguiente = ciclo.obtener((i + 1) % ciclo.tamano());
            cerrado &= siguiente.prerrequisitos().contiene(actual);
        }
        verificar(cerrado, "cada asignatura del ciclo es prerrequisito de la siguiente");
        verificar(!Analisis.ejecutar(carga.registro(), 2).hayPlan(), "no se planifica un plan con ciclos");
    }

    // ------------------------------------------------------------------ planificador

    private void planificador() {
        CargadorPlan.Resultado ejemplo = cargar("datos/plan_ejemplo.txt");
        if (ejemplo != null) {
            Analisis a = Analisis.ejecutar(ejemplo.registro(), 3);
            Plan plan = a.plan();
            verificarPlan(ejemplo.registro(), plan, "ejemplo del README");
            verificar(plan.cantidadSemestres() == 3 && a.esOptimoGarantizado(), "ejemplo del README: 3 semestres, óptimo garantizado");
            Semestre primero = plan.semestres().obtenerPrimero();
            verificar(primero.asignaturas().obtener(0).id() == 102 && primero.asignaturas().obtener(1).id() == 101
                    && primero.asignaturas().obtener(2).id() == 106, "desempate: mayor altura y luego ID menor (102, 101, 106)");
        }

        RegistroAsignaturas cadena = CargadorPlan.cargarTexto("4\n1;A;\n2;B;1\n3;C;2\n4;D;3\n5;E;4\n6;F;5\n").registro();
        verificar(Analisis.ejecutar(cadena, 4).plan().cantidadSemestres() == 6, "una cadena de 6 necesita 6 semestres con cualquier k");

        RegistroAsignaturas libres = CargadorPlan.cargarTexto("3\n1;A;\n2;B;\n3;C;\n4;D;\n5;E;\n6;F;\n7;G;\n8;H;\n9;I;\n10;J;\n").registro();
        verificar(Analisis.ejecutar(libres, 3).plan().cantidadSemestres() == 4, "10 asignaturas sin prerrequisitos y k = 3: techo(10/3) = 4");
        verificar(Analisis.ejecutar(libres, 1).plan().cantidadSemestres() == 10, "k = 1: un semestre por asignatura");

        CargadorPlan.Resultado reforzada = cargar("datos/cota_reforzada.txt");
        if (reforzada != null) {
            Analisis a = Analisis.ejecutar(reforzada.registro(), 2);
            verificar(a.plan().cantidadSemestres() == 4, "S1, S2, S3 -> M -> N con k = 2: 4 semestres");
            verificar(a.cota().simple() == 3 && a.cota().reforzada() == 4, "cota simple 3, cota reforzada 4");
            verificar(a.esOptimoGarantizado(), "solo la cota reforzada certifica que es óptimo");
        }
    }

    private void creditos() {
        RegistroAsignaturas r = CargadorPlan.cargarTexto("3\n1;A;;3\n2;B;;3\n3;Trabajo;cr>=6;6\n").registro();
        Plan plan = Analisis.ejecutar(r, 3).plan();
        verificarPlan(r, plan, "requisito de créditos totales");
        verificar(plan.cantidadSemestres() == 2 && plan.semestreDe(r.buscar(3).indice()) == 2,
                "cr>=6 espera a que A y B sumen 6 créditos");

        RegistroAsignaturas porComponente = CargadorPlan.cargarTexto(
                "4\n1;A;;3;C\n2;B;;4;B\n3;D;1;3;C\n4;Trabajo;crC>=6;6;C\n").registro();
        Plan planComponente = Analisis.ejecutar(porComponente, 4).plan();
        verificarPlan(porComponente, planComponente, "requisito por componente");
        verificar(planComponente.semestreDe(porComponente.buscar(4).indice()) == 3,
                "crC>=6 no cuenta los créditos de B: espera a A y D (semestre 3)");

        RegistroAsignaturas imposible = CargadorPlan.cargarTexto("3\n1;A;;3;C\n2;B;;9;B\n3;Trabajo;crC>=6;6;C\n").registro();
        Plan incompleto = Analisis.ejecutar(imposible, 3).plan();
        verificar(!incompleto.esCompleto() && incompleto.problema().contains("componente C"),
                "avisa si un requisito de créditos es inalcanzable");
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

    private void verificarPlan(RegistroAsignaturas registro, Plan plan, String nombre) {
        String error = VerificadorPlan.verificar(registro, plan);
        verificar(error == null, nombre + ": el plan cumple k, prerrequisitos y créditos" + (error == null ? "" : " (" + error + ")"));
    }

    private CargadorPlan.Resultado cargar(String ruta) {
        if (!new File(ruta).isFile()) {
            fallidas++;
            out.println("  FALLA  no se encontró " + ruta + " (corra las pruebas desde la raíz del repositorio)");
            return null;
        }
        try {
            return CargadorPlan.cargarArchivo(ruta);
        } catch (IOException e) {
            fallidas++;
            out.println("  FALLA  no se pudo leer " + ruta + ": " + e.getMessage());
            return null;
        }
    }

    private static long creditosAntes(Plan plan, int semestre, char componente) {
        long creditos = 0;
        for (ListaEnlazada.Nodo<Semestre> nodo = plan.semestres().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (nodo.dato().numero() >= semestre) {
                continue;
            }
            for (ListaEnlazada.Nodo<Asignatura> a = nodo.dato().asignaturas().primero(); a != null; a = a.siguiente()) {
                if (a.dato().componente() == componente) {
                    creditos += a.dato().creditos();
                }
            }
        }
        return creditos;
    }

    /** @return los IDs de los prerrequisitos registrados de una asignatura, separados por coma */
    private static String prerrequisitosDe(RegistroAsignaturas registro, long id) {
        StringBuilder texto = new StringBuilder();
        for (ListaEnlazada.Nodo<Asignatura> nodo = registro.buscar(id).prerrequisitos().primero(); nodo != null; nodo = nodo.siguiente()) {
            if (texto.length() > 0) {
                texto.append(',');
            }
            texto.append(nodo.dato().id());
        }
        return texto.toString();
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
