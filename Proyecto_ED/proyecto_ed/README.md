# Graduación UNAL: mínimo número de semestres

Proyecto de clase de **Estructuras de Datos (2016699)**, Universidad Nacional de Colombia.
Profesor: David Alberto Herrera Álvarez. Equipo N.° `[número]`.

El sistema calcula **el número mínimo de semestres** para terminar un plan de estudios y **qué asignaturas cursar en cada semestre**, respetando:

- todas las asignaturas obligatorias,
- los **prerrequisitos** (cada uno se aprueba en un semestre anterior),
- el **límite máximo `k` de asignaturas por semestre**.

El enfoque es técnico: aplicación de consola, entrada por archivo y estructuras de datos implementadas desde cero (sin `java.util.List`, `Map`, `Queue`, etc.).

## Integrantes

| Integrante | Responsabilidad principal (entrega 1) |
| --- | --- |
| Raúl Santiago Bermúdez Camacho | Modelo `Asignatura`, árbol AVL y registro por ID |
| `[Integrante 2]` | Cargador de archivos de entrada y validaciones |
| `[Integrante 3]` | Planificador (Kahn, alturas, prioridades) y pruebas |

## Lenguaje y herramientas

- **Java 17** o superior (probado con OpenJDK 21).
- **Maven 3.8+** (opcional: también compila solo con `javac`).
- **JUnit 5** únicamente para las pruebas.

## Instalación y ejecución

```bash
git clone https://github.com/Danieelggs/Proyecto_ED.git
cd Proyecto_ED
```

**Con Maven**

```bash
mvn compile
mvn exec:java -Dexec.args="datos/plan_ejemplo.txt"        # usa el k del archivo
mvn exec:java -Dexec.args="datos/plan_ejemplo.txt 2"      # fuerza k = 2
mvn test                                                   # pruebas unitarias
```

**Solo con el JDK (sin Maven)**

```bash
# Linux / macOS
javac -d out $(find src/main/java -name "*.java")
# Windows (PowerShell)
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName

java -cp out unal.graduacion.Main datos/plan_ejemplo.txt
```

> Si las tildes salen como `?` en la consola de Windows, ejecute antes `chcp 65001` o agregue `-Dstdout.encoding=UTF-8` al comando `java`.

## Formato de entrada

Archivo de texto UTF-8. Las líneas vacías y las que empiezan por `#` se ignoran.

```text
# primera línea útil: k = máximo de asignaturas por semestre
3
# id;nombre;prerrequisitos separados por coma (vacío si no tiene)
101;Cálculo Diferencial;
104;Cálculo Integral;101
107;Estructuras de Datos;105,106
```

Validaciones: IDs duplicados, prerrequisitos inexistentes, una asignatura como prerrequisito de sí misma y **ciclos** de prerrequisitos (plan imposible).

## Ejemplo de salida

```text
$ java -cp out unal.graduacion.Main datos/plan_ejemplo.txt
Plan: datos/plan_ejemplo.txt | asignaturas: 14 | altura del AVL de registro: 4
Semestres necesarios: 5 (máximo 3 asignaturas por semestre)
Cota inferior: 5 -> el plan es óptimo con certeza

Semestre 1 (3)
  [102] Fundamentos de Programación
  [106] Matemáticas Discretas I
  [101] Cálculo Diferencial

Semestre 2 (3)
  [105] Programación Orientada a Objetos
  [103] Álgebra Lineal
  [104] Cálculo Integral
...
```

```text
$ java -cp out unal.graduacion.Main datos/plan_con_ciclo.txt
Plan imposible: El plan tiene un ciclo de prerrequisitos. Asignaturas en el ciclo o bloqueadas por él: [[201] Asignatura A, [202] Asignatura B, [203] Asignatura C]
```

## Cómo funciona

El plan es un **grafo dirigido**: cada asignatura es un nodo y cada prerrequisito una arista `A → B` ("A antes de B").

1. **Registrar** cada asignatura en un **árbol AVL por ID** y enlazar sus prerrequisitos y dependientes en **listas enlazadas**.
2. **Validar** con el algoritmo de **Kahn** (usa una **cola**): si no se pueden procesar todas las asignaturas, hay un ciclo.
3. **Calcular la altura** de cada asignatura recorriendo el orden topológico al revés con una **pila**: altura = 1 + máxima altura de sus dependientes (la cadena más larga que abre).
4. **Llenar cada semestre** con las `k` asignaturas disponibles de mayor altura, extraídas de un **AVL de prioridad** `(altura, ID)`. Las que quedan sin prerrequisitos pendientes entran para el semestre siguiente.
5. **Reportar** el plan y la **cota inferior** `LB = max(⌈n/k⌉, cadena más larga)`. Si el plan usa `LB` semestres, es óptimo con certeza.

Con límite `k` el problema exacto es NP-difícil en general (planificación con precedencias), por eso el sistema combina una heurística eficiente con una cota que certifica la optimalidad. La entrega 3 añade una solución exacta para planes pequeños.

| Paso | Estructura | Complejidad |
| --- | --- | --- |
| Registro y acceso por ID | Árbol AVL | O(log n) por operación |
| Adyacencias | Lista enlazada | O(1) agregar, O(grado) recorrer |
| Validación (Kahn) | Cola circular | O(n + e) |
| Alturas | Pila | O(n + e) |
| Reparto por semestres | AVL de prioridad | O(n log n + e log n) |

`n` = número de asignaturas, `e` = número de prerrequisitos.

## Hoja de ruta

| Entrega | Contenido |
| --- | --- |
| 1 | Problema, diseño, justificación de estructuras, prototipo funcional y pruebas (este repositorio) |
| 2 | Medición de tiempos (`System.nanoTime`, CSV y gráficas) de la versión con listas, pilas, colas y AVL |
| 3 | Tabla hash para el registro, heap de máximos para la prioridad, grafo con DFS de tres colores y solución exacta con conjuntos (n ≤ 20); comparación con la entrega 2 |

## Estructura del proyecto

```text
Proyecto_ED/
├── README.md
├── pom.xml
├── datos/
│   ├── plan_ejemplo.txt          # 14 asignaturas, k = 3
│   └── plan_con_ciclo.txt        # plan inválido para probar RF3
└── src/
    ├── main/java/unal/graduacion/
    │   ├── Main.java                         # consola
    │   ├── estructuras/
    │   │   ├── ListaEnlazada.java            # adyacencias y semestres
    │   │   ├── Cola.java                     # Kahn
    │   │   ├── Pila.java                     # alturas
    │   │   ├── ArbolAVL.java                 # registro por ID y prioridad
    │   │   └── entrega3/                     # plantillas: TablaHash, MonticuloMaximo, GrafoDirigido
    │   ├── modelo/
    │   │   ├── Asignatura.java
    │   │   └── RegistroAsignaturas.java
    │   ├── io/
    │   │   └── CargadorPlan.java
    │   ├── planificador/
    │   │   ├── Planificador.java
    │   │   ├── Prioridad.java
    │   │   ├── ResultadoPlan.java
    │   │   └── CicloPrerrequisitosException.java
    │   └── medicion/
    │       └── GeneradorPlanes.java          # planes aleatorios para medir tiempos
    └── test/java/unal/graduacion/
        ├── EstructurasTest.java
        └── PlanificadorTest.java
```

## Documentación

El reporte técnico de la entrega 1 (problema, diagramas, justificación de estructuras y MVP) se entrega en PDF por Moodle como `Proyecto-Entrega1-Reporte-[número].pdf`.
