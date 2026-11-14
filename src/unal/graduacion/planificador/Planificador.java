package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;

/**
 * Arma los semestres (RF4, RF5) con el algoritmo de Hu: cada semestre cursa
 * las k asignaturas disponibles de mayor altura.
 *
 * <p>Una asignatura está disponible cuando todos sus prerrequisitos se
 * aprobaron en semestres anteriores y, si tiene requisito de créditos
 * (cr&gt;=N o crC&gt;=N), cuando ya se aprobaron esos créditos, en total o
 * del componente indicado. Las que se desbloquean en un
 * semestre entran a la estructura de disponibles <b>después</b> de las k
 * extracciones, así un prerrequisito nunca comparte semestre con su
 * dependiente.</p>
 *
 * <p>Costo con {@link DisponiblesAVL}: cada asignatura entra y sale una vez,
 * O(n log n), y cada prerrequisito se descuenta una vez, O(e). Total
 * O(n log n + e). Las que esperan créditos se revisan al iniciar cada
 * semestre: O(T·w), con w las asignaturas en espera (2 en la malla real).</p>
 *
 * <p>Si k puede tomar cualquier valor, el problema es NP-difícil (Ullman, 1975), así que el plan
 * puede no ser mínimo; es óptimo con certeza cuando iguala la cota inferior
 * ({@link CotaInferior}).</p>
 */
public final class Planificador {

    private Planificador() {
    }

    /**
     * Planifica con el AVL de prioridad (la estructura de la entrega 2).
     *
     * @param registro plan validado (sin ciclos)
     * @param k        máximo de asignaturas por semestre
     * @param altura   altura de cada asignatura, por índice interno
     * @return el plan por semestres
     */
    public static Plan planificar(RegistroAsignaturas registro, int k, int[] altura) {
        return planificar(registro, k, new DisponiblesAVL(altura));
    }

    /**
     * Planifica con la estructura de disponibles indicada (para comparar).
     *
     * @param registro    plan validado (sin ciclos)
     * @param k           máximo de asignaturas por semestre
     * @param disponibles estructura vacía donde se guardan las disponibles
     * @return el plan por semestres
     */
    public static Plan planificar(RegistroAsignaturas registro, int k, Disponibles disponibles) {
        if (k < 1) {
            throw new IllegalArgumentException("k debe ser al menos 1");
        }
        int n = registro.cantidad();
        Plan plan = new Plan(n, k, disponibles.nombre());
        int[] grado = new int[n];
        ListaEnlazada<Asignatura> enEspera = new ListaEnlazada<>();

        for (int i = 0; i < n; i++) {
            Asignatura asignatura = registro.obtener(i);
            grado[i] = asignatura.gradoEntrada();
            if (grado[i] == 0) {
                liberar(asignatura, disponibles, enEspera);
            }
        }

        CreditosAprobados aprobados = new CreditosAprobados();
        int ubicadas = 0;
        int numero = 0;
        while (ubicadas < n) {
            if (!enEspera.estaVacia()) {
                enEspera = moverLasQueCumplen(enEspera, aprobados, disponibles);
            }
            if (disponibles.estaVacia()) {
                plan.marcarProblema(explicarBloqueo(n - ubicadas, enEspera, aprobados));
                break;
            }

            numero++;
            Semestre semestre = new Semestre(numero);
            int cupo = Math.min(k, disponibles.tamano());
            for (int i = 0; i < cupo; i++) {
                semestre.agregar(disponibles.extraerMejor());
            }
            ubicadas += cupo;

            // Cierre del semestre: se suman créditos y se desbloquean dependientes.
            for (ListaEnlazada.Nodo<Asignatura> nodo = semestre.asignaturas().primero(); nodo != null; nodo = nodo.siguiente()) {
                Asignatura cursada = nodo.dato();
                aprobados.sumar(cursada);
                for (ListaEnlazada.Nodo<Asignatura> dep = cursada.dependientes().primero(); dep != null; dep = dep.siguiente()) {
                    int d = dep.dato().indice();
                    grado[d]--;
                    if (grado[d] == 0) {
                        liberar(dep.dato(), disponibles, enEspera);
                    }
                }
            }
            semestre.fijarCreditosAcumulados(aprobados.total());
            plan.agregarSemestre(semestre);
        }
        return plan;
    }

    private static void liberar(Asignatura asignatura, Disponibles disponibles, ListaEnlazada<Asignatura> enEspera) {
        if (asignatura.creditosMinimos() > 0) {
            enEspera.agregarAlFinal(asignatura);
        } else {
            disponibles.agregar(asignatura);
        }
    }

    private static ListaEnlazada<Asignatura> moverLasQueCumplen(ListaEnlazada<Asignatura> enEspera, CreditosAprobados aprobados,
                                                               Disponibles disponibles) {
        ListaEnlazada<Asignatura> siguenEsperando = new ListaEnlazada<>();
        for (ListaEnlazada.Nodo<Asignatura> nodo = enEspera.primero(); nodo != null; nodo = nodo.siguiente()) {
            Asignatura asignatura = nodo.dato();
            if (aprobados.cumpleRequisito(asignatura)) {
                disponibles.agregar(asignatura);
            } else {
                siguenEsperando.agregarAlFinal(asignatura);
            }
        }
        return siguenEsperando;
    }

    private static String explicarBloqueo(int sinUbicar, ListaEnlazada<Asignatura> enEspera, CreditosAprobados aprobados) {
        if (enEspera.estaVacia()) {
            return "Quedaron " + sinUbicar + " asignaturas sin ubicar: el plan tiene un ciclo de prerrequisitos.";
        }
        Asignatura ejemplo = enEspera.obtenerPrimero();
        return "Quedaron " + sinUbicar + " asignaturas sin ubicar: " + ejemplo + " exige "
                + ejemplo.requisitoCreditosTexto() + " y el resto del plan solo suma "
                + aprobados.de(ejemplo.componenteRequisito()) + ".";
    }
}
