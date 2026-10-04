package unal.graduacion.planificador;

import unal.graduacion.estructuras.ListaEnlazada;
import unal.graduacion.modelo.Asignatura;

/**
 * Resultado de planificar: asignaturas por semestre y cota inferior.
 *
 * La cota inferior LB = max(ceil(n / k), cadena más larga) es un mínimo
 * teórico: ningún plan puede usar menos semestres. Si el plan obtenido
 * usa exactamente LB semestres, es óptimo con certeza (RF6).
 */
public class ResultadoPlan {

    private final ListaEnlazada<ListaEnlazada<Asignatura>> semestres;
    private final int cotaInferior;
    private final int limitePorSemestre;

    public ResultadoPlan(ListaEnlazada<ListaEnlazada<Asignatura>> semestres, int cotaInferior, int limitePorSemestre) {
        this.semestres = semestres;
        this.cotaInferior = cotaInferior;
        this.limitePorSemestre = limitePorSemestre;
    }

    public int numeroSemestres() {
        return semestres.tamano();
    }

    public ListaEnlazada<ListaEnlazada<Asignatura>> getSemestres() {
        return semestres;
    }

    public int getCotaInferior() {
        return cotaInferior;
    }

    public boolean esOptimoGarantizado() {
        return numeroSemestres() == cotaInferior;
    }

    /** Texto para la consola (RF5 y RF6). */
    public String comoTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("Semestres necesarios: ").append(numeroSemestres())
          .append(" (máximo ").append(limitePorSemestre).append(" asignaturas por semestre)\n");
        sb.append("Cota inferior: ").append(cotaInferior);
        if (esOptimoGarantizado()) {
            sb.append(" -> el plan es óptimo con certeza\n");
        } else {
            sb.append(" -> brecha de ").append(numeroSemestres() - cotaInferior)
              .append(" semestre(s); el óptimo está entre ").append(cotaInferior)
              .append(" y ").append(numeroSemestres()).append("\n");
        }
        int numero = 1;
        for (ListaEnlazada<Asignatura> semestre : semestres) {
            sb.append("\nSemestre ").append(numero++).append(" (").append(semestre.tamano()).append(")\n");
            for (Asignatura a : semestre) {
                sb.append("  ").append(a).append('\n');
            }
        }
        return sb.toString();
    }
}
