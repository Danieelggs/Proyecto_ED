package unal.graduacion;

import java.io.IOException;
import java.nio.file.Path;

import unal.graduacion.io.CargadorPlan;
import unal.graduacion.io.CargadorPlan.PlanCargado;
import unal.graduacion.planificador.CicloPrerrequisitosException;
import unal.graduacion.planificador.Planificador;
import unal.graduacion.planificador.ResultadoPlan;

/**
 * Punto de entrada por consola.
 *
 * Uso:
 *   java -cp target/classes unal.graduacion.Main &lt;archivo-del-plan&gt; [k]
 *
 * Si se pasa k, reemplaza el límite escrito en el archivo.
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: Main <archivo-del-plan> [k]");
            System.out.println("Ejemplo: Main datos/plan_ejemplo.txt 3");
            return;
        }
        try {
            PlanCargado plan = CargadorPlan.cargar(Path.of(args[0]));
            int k = args.length > 1 ? Integer.parseInt(args[1]) : plan.limitePorSemestre();

            System.out.println("Plan: " + args[0] + " | asignaturas: " + plan.registro().tamano()
                    + " | altura del AVL de registro: " + plan.registro().alturaArbol());

            ResultadoPlan resultado = new Planificador(plan.registro()).planificar(k);
            System.out.println(resultado.comoTexto());
        } catch (CicloPrerrequisitosException e) {
            System.err.println("Plan imposible: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Entrada inválida: " + e.getMessage());
            System.exit(1);
        }
    }
}
