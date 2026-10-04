package unal.graduacion;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.io.CargadorPlan;
import unal.graduacion.io.CargadorPlan.PlanCargado;
import unal.graduacion.medicion.GeneradorPlanes;
import unal.graduacion.modelo.Asignatura;
import unal.graduacion.modelo.RegistroAsignaturas;
import unal.graduacion.planificador.CicloPrerrequisitosException;
import unal.graduacion.planificador.Planificador;
import unal.graduacion.planificador.ResultadoPlan;

class PlanificadorTest {

    private static PlanCargado plan(String texto) throws IOException {
        return CargadorPlan.cargar(new StringReader(texto));
    }

    /** Verifica límite k, prerrequisitos en semestres anteriores y que estén todas. */
    private static void verificarPlanValido(RegistroAsignaturas registro, ResultadoPlan r, int k) {
        int[] semestreDe = new int[registro.tamano()];
        int numero = 0;
        int total = 0;
        for (ListaEnlazada<Asignatura> semestre : r.getSemestres()) {
            numero++;
            assertTrue(semestre.tamano() <= k, "semestre " + numero + " supera k");
            for (Asignatura a : semestre) {
                semestreDe[a.getIndice()] = numero;
                total++;
            }
        }
        assertEquals(registro.tamano(), total, "faltan o sobran asignaturas");
        for (Asignatura a : registro.todas()) {
            for (Asignatura pre : a.getPrerrequisitos()) {
                assertTrue(semestreDe[pre.getIndice()] < semestreDe[a.getIndice()],
                        pre + " debe ir antes que " + a);
            }
        }
        assertTrue(r.numeroSemestres() >= r.getCotaInferior());
    }

    @Test
    void cadenaSimpleNecesitaUnSemestrePorEslabon() throws IOException {
        PlanCargado p = plan("2\n1;A;\n2;B;1\n3;C;2\n");
        ResultadoPlan r = new Planificador(p.registro()).planificar(p.limitePorSemestre());
        assertEquals(3, r.numeroSemestres());
        assertTrue(r.esOptimoGarantizado());
    }

    @Test
    void sinPrerrequisitosSeReparteSegunK() throws IOException {
        PlanCargado p = plan("3\n1;A;\n2;B;\n3;C;\n4;D;\n5;E;\n6;F;\n7;G;\n");
        ResultadoPlan r = new Planificador(p.registro()).planificar(3);
        assertEquals(3, r.numeroSemestres()); // ceil(7/3)
        verificarPlanValido(p.registro(), r, 3);
    }

    @Test
    void priorizaLaCadenaMasLarga() throws IOException {
        // Con k = 1 da igual; con k = 2, cursar primero las sueltas (2, 3)
        // retrasaría la cadena 1 -> 4 -> 5. La altura evita ese error.
        PlanCargado p = plan("2\n1;Inicio cadena;\n2;Suelta 1;\n3;Suelta 2;\n4;Medio;1\n5;Fin;4\n");
        ResultadoPlan r = new Planificador(p.registro()).planificar(2);
        assertEquals(3, r.numeroSemestres());
        verificarPlanValido(p.registro(), r, 2);
    }

    @Test
    void detectaCiclos() throws IOException {
        PlanCargado p = plan("2\n1;A;3\n2;B;1\n3;C;2\n4;D;\n");
        CicloPrerrequisitosException e = assertThrows(CicloPrerrequisitosException.class,
                () -> new Planificador(p.registro()).planificar(2));
        assertEquals(3, e.getBloqueadas().tamano());
    }

    @Test
    void rechazaPrerrequisitoInexistenteEIdDuplicado() {
        assertThrows(IllegalArgumentException.class, () -> plan("2\n1;A;99\n"));
        assertThrows(IllegalArgumentException.class, () -> plan("2\n1;A;\n1;B;\n"));
    }

    @Test
    void planesGrandesAleatoriosSonValidos() {
        for (int k = 1; k <= 6; k++) {
            RegistroAsignaturas registro = GeneradorPlanes.generar(2_000, 4, 42 + k);
            ResultadoPlan r = new Planificador(registro).planificar(k);
            verificarPlanValido(registro, r, k);
        }
    }
}
