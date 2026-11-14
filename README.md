#  Graduación UNAL

> Planificador del **mínimo número de semestres** para terminar un plan de estudios, respetando prerrequisitos y el límite de asignaturas por semestre.

**Curso:** Estructuras de Datos (2016699), Universidad Nacional de Colombia
**Profesor:** David Alberto Herrera Álvarez
**Estado:** Entrega 2: prototipo funcional con arreglos, listas, colas, pilas y árboles (BST, AVL)

---

## Descripción del problema

En la Universidad Nacional, los programas de pregrado están formados por múltiples asignaturas. Algunas tienen **prerrequisitos**: deben cursarse y aprobarse antes de poder tomar otras. Además, para no sobrecargarse, cada estudiante tiene un **límite máximo de asignaturas por semestre**.

Esto obliga a planificar con cuidado el orden en que se toman los cursos. Un mal orden puede retrasar la graduación uno o varios semestres, por ejemplo si se dejan para después las asignaturas que abren las cadenas de prerrequisitos más largas.

## Objetivo

Diseñar un sistema que determine el **número mínimo de semestres** necesarios para terminar el plan de estudios, cumpliendo:

-  todas las asignaturas obligatorias,
-  las restricciones de prerrequisitos entre asignaturas,
-  el límite máximo de asignaturas por semestre (`k`).

El sistema debe:

- registrar asignaturas con **ID numérico** y **lista de prerrequisitos**,
- acceder a cualquier asignatura por su ID,
- calcular el número mínimo de semestres,
- mostrar qué asignaturas se deben cursar en cada semestre.

## Integrantes

| Nombre | Correo |
| --- | --- |
| Raúl Santiago Bermúdez Camacho | rbermudezc@unal.edu.co |
| Daniel Felipe Rodríguez Rodríguez | danirodriguezrod@unal.edu.co |
| Valeria Aranda Pacheco | Varanda@unal.edu.co |
| José Santiago Quintero Ovalle | jquinteroov@unal.edu.co |
| Brallan Esteban Ardila Osorio | bardilao@unal.edu.co |
| Daniel Felipe Velandia Peña | dvelandiape@unal.edu.co |

## Lenguaje y herramientas

| Herramienta | Uso |
| --- | --- |
| **Java 17+** | Lenguaje de implementación, sin dependencias externas |
| **Git + GitHub** | Control de versiones y trabajo en equipo |
| **HTML, CSS y JavaScript** | Solo en la vista opcional del plan que el programa genera con `--html` |

Todas las estructuras de datos están implementadas **desde cero** en `src/unal/graduacion/estructuras/`. El código no importa nada de `java.util` (ni `ArrayList`, `HashMap`, `PriorityQueue` ni `Scanner`): solo usa `java.io` y `java.nio` para leer y escribir archivos. Las pruebas tampoco usan JUnit, así que el proyecto se clona, se compila y se ejecuta solo con el JDK. Es una aplicación de **consola** con entrada por archivo de texto, sin interfaz gráfica.

## Solución

El plan de estudios se modela como un **grafo dirigido**: cada asignatura es un nodo y cada prerrequisito es una arista `A → B` ("A debe aprobarse antes que B"). Un plan válido no puede tener ciclos; si los tiene, es imposible de terminar y el sistema lo reporta.

```text
Cálculo Diferencial ──► Cálculo Integral ──► Ecuaciones Diferenciales
                                        └──► Probabilidad y Estadística
Fundamentos de Prog. ──► POO ──► Estructuras de Datos ──► Algoritmos
Matemáticas Discretas ─────────────────┘
```

**Flujo:**

1. **Leer** el archivo del plan. La primera línea útil es `k`.
2. **Registrar** cada asignatura en un **árbol AVL por ID**, con su lista de prerrequisitos. La carga hace dos pasadas para que una asignatura pueda citar a otra que aparece más abajo, y rechaza IDs repetidos y prerrequisitos inexistentes.
3. **Validar** que no haya ciclos con el algoritmo de **Kahn** (cola). Si hay uno, lo muestra junto con las asignaturas bloqueadas.
4. **Calcular la altura** de cada asignatura: cuántas asignaturas tiene la cadena más larga que abre. Se recorre el orden topológico al revés con una **pila**.
5. **Armar los semestres** (algoritmo de Hu): en cada semestre se sacan del **AVL de prioridad** las `k` asignaturas disponibles de mayor altura. Las que quedan habilitadas entran para el semestre siguiente, así un prerrequisito nunca comparte semestre con su dependiente. Las que exigen créditos aprobados esperan hasta cumplirlos.
6. **Certificar** el resultado con una **cota inferior**, un número de semestres por debajo del cual ningún plan puede terminar. Si el plan obtenido la alcanza, es **óptimo garantizado**.
   - Cota simple: `max(⌈n/k⌉, cadena más larga)`.
   - Cota reforzada: el máximo, sobre cada altura `h`, de `h − 1 + ⌈N(h)/k⌉`, con `N(h)` el número de asignaturas de altura `≥ h`. Nunca es menor que la simple.

Cuando `k` puede tomar cualquier valor, el problema es NP-difícil (Ullman, 1975): no se conoce un algoritmo exacto y eficiente para todos los planes. Solo los hay en casos particulares, como `k = 2` (Coffman y Graham, 1972); para un `k` fijo mayor o igual a 3, su complejidad sigue siendo un problema abierto. Por eso el sistema combina una heurística rápida con un certificado: el algoritmo de Hu es óptimo cuando cada asignatura es prerrequisito de a lo sumo otra, y la cota inferior dice cuándo el resultado es óptimo en los demás casos. En la malla real lo certifica para `k` = 4, 5, 6 y 7.

## Estructuras de datos

### Entrega 2: implementadas

| Estructura | Clase | Uso en el sistema | Costo |
| --- | --- | --- | --- |
| Árbol AVL por ID | `ArbolAVL<Long, Asignatura>` | Registro y acceso por ID (RF1, RF2); también guarda los códigos de las optativas | O(log n) |
| Arreglo | `RegistroAsignaturas` y algoritmos | Asignatura por índice interno; grado, altura y semestre de cada una | O(1) |
| Lista enlazada | `ListaEnlazada` | Prerrequisitos y dependientes de cada asignatura; asignaturas de cada semestre | O(1) al agregar |
| Cola circular | `Cola` | Kahn: orden topológico y detección de ciclos | O(1) amortizado |
| Pila | `Pila` | Orden topológico al revés para calcular las alturas | O(1) |
| Árbol AVL por (altura, ID) | `DisponiblesAVL` | Asignaturas disponibles; se extraen las `k` de mayor altura | O(log n) |

Para el análisis comparativo hay dos alternativas que el sistema no usa: `ArbolBST`, que con IDs ordenados degenera en una lista (altura `n`), y `DisponiblesLista`, una lista sin orden en la que sacar la mejor asignatura cuesta O(n).

Costo total: O(n + e) para validar y calcular alturas, y O((n + e) log n) para registrar y planificar, con `n` asignaturas y `e` prerrequisitos.

### Entrega 3: planeadas

| Estructura | Reemplaza a | Mejora esperada |
| --- | --- | --- |
| Tabla hash | AVL por ID | Acceso por ID en O(1) promedio |
| Grafo dirigido (listas de adyacencia + DFS) | Listas sueltas | Modelo explícito y detección de ciclos en O(n + e) |
| Heap de máximos | AVL por prioridad | Mismo O(log n) con menor costo constante |
| Conjuntos (bits) | — | Solución exacta para planes pequeños (n ≤ 20) |

Ambas versiones se compararán con mediciones de tiempo de ejecución y gráficas.

## Requisitos funcionales

| ID | Requisito | Dónde |
| --- | --- | --- |
| RF1 | Registrar asignaturas con ID, nombre y prerrequisitos; rechazar IDs duplicados y prerrequisitos inexistentes | `CargadorPlan`, `RegistroAsignaturas` |
| RF2 | Acceder a cualquier asignatura por su ID | `RegistroAsignaturas.buscar`; opción 2 del menú y `--id` |
| RF3 | Detectar ciclos de prerrequisitos y reportar las asignaturas afectadas | `ValidadorCiclos` |
| RF4 | Calcular el número de semestres sin superar `k` y con cada prerrequisito en un semestre anterior | `Planificador`, `VerificadorPlan` |
| RF5 | Mostrar las asignaturas de cada semestre | `ImpresorPlan`, `ExportadorHTML` |
| RF6 | Informar la cota inferior y si el plan es óptimo garantizado | `CotaInferior` |
| RF7 | Medir tiempos de ejecución con planes de tamaño creciente y exportarlos a CSV | `Medidor`, `GeneradorPlanes` |

## Formato de entrada y salida

**Entrada:** archivo de texto UTF-8. Las líneas vacías y las que empiezan por `#` se ignoran. La primera línea útil es `k` y luego va una asignatura por línea:

```text
id;nombre;prerrequisitos;créditos;componente;optativas
```

Solo `id` y `nombre` son obligatorios, así que el formato corto de la Entrega 1 sigue funcionando:

```text
# Primera línea: k = máximo de asignaturas por semestre
3
# id;nombre;prerrequisitos separados por coma
101;Cálculo Diferencial;
102;Fundamentos de Programación;
104;Cálculo Integral;101
105;Programación Orientada a Objetos;102
106;Matemáticas Discretas I;
107;Estructuras de Datos;105,106
```

Los campos opcionales sirven para la malla real (`datos/plan_2A74.txt`):

| Campo | Contenido | Ejemplo |
| --- | --- | --- |
| prerrequisitos | Códigos separados por coma. Admite un requisito de créditos: `cr>=N` (N créditos aprobados) o `crC>=N` (N créditos del componente C) | `2015702,crC>=40` |
| créditos | Entero no negativo | `3` |
| componente | `B` fundamentación, `C` disciplinar, `L` libre elección, `P` nivelación | `C` |
| optativas | `código nombre (créditos)`, separadas por `\|`; su código también encuentra la casilla | `2016377 Cálculo diferencial en una variable (4 cr)` |

**Salida** de `java -cp out unal.graduacion.Main datos/plan_ejemplo.txt`:

```text
Plan: datos/plan_ejemplo.txt
6 asignaturas · 4 prerrequisitos · k = 3

Semestres necesarios: 3
Cota inferior:        3   (por cantidad 6/3 -> 2, por cadena 3)
Resultado:            óptimo garantizado; ningún plan termina en menos de 3 semestres.

h = altura: cuántas asignaturas tiene la cadena más larga que abre.  * = crítica: si se atrasa, se atrasa la graduación.

Semestre 1 · 3 asignaturas
  * h3  [102] Fundamentos de Programación
    h2  [101] Cálculo Diferencial
    h2  [106] Matemáticas Discretas I

Semestre 2 · 2 asignaturas
  * h2  [105] Programación Orientada a Objetos
    h1  [104] Cálculo Integral

Semestre 3 · 1 asignatura
  * h1  [107] Estructuras de Datos
```

## Instalación

**Requisitos:** [JDK 17 o superior](https://adoptium.net/) y [Git](https://git-scm.com/). No hay más dependencias.

```bash
# 1. Clonar el repositorio
git clone https://github.com/Danieelggs/Proyecto_ED.git
cd Proyecto_ED

# 2. Verificar la versión de Java (17 o superior)
java -version

# 3. Compilar
# Linux / macOS
javac -encoding UTF-8 -d out $(find src -name "*.java")

# Windows (PowerShell)
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

`-encoding UTF-8` es necesario en Windows con JDK 17: sin esa opción, las tildes del código fuente se leen mal.

## Uso

Los comandos se ejecutan desde la raíz del repositorio, porque los planes de `datos/` se buscan con rutas relativas.

```bash
java -cp out unal.graduacion.Main
```

Abre el menú interactivo. La opción 1 pide la ruta del plan (con Enter carga `datos/plan_2A74.txt`); las demás permiten consultar una asignatura por código, cambiar `k`, calcular el plan, exportarlo a HTML, generar planes aleatorios y correr las mediciones. Para usar el plan desde la línea de comandos, sin menú:

| Comando | Qué hace |
| --- | --- |
| `java -cp out unal.graduacion.Main datos/plan_2A74.txt` | Plan de la malla real con el `k` del archivo (6) |
| `java -cp out unal.graduacion.Main datos/plan_2A74.txt 5` | La misma malla con `k = 5` |
| `java -cp out unal.graduacion.Main datos/plan_2A74.txt --id 2016699` | Ficha de una asignatura; también acepta el código de una optativa |
| `java -cp out unal.graduacion.Main datos/plan_2A74.txt --html resultados/plan.html` | Además guarda una vista del plan para abrir en el navegador |
| `java -cp out unal.graduacion.Main datos/ciclo.txt` | Plan imposible: muestra el ciclo y las asignaturas bloqueadas |
| `java -cp out unal.graduacion.Main datos/errores.txt` | Muestra las validaciones de la carga |
| `java -cp out unal.graduacion.Main --pruebas` | 81 verificaciones funcionales con respuesta conocida |
| `java -cp out unal.graduacion.Main --medir` | Tiempos de 10⁴ a 10⁶ asignaturas en `resultados/tiempos.csv`; tarda unos minutos |
| `java -cp out unal.graduacion.Main --medir --rapido` | Versión corta, hasta 5·10⁴ asignaturas |
| `java -cp out unal.graduacion.Main --calidad` | Con qué frecuencia la cota certifica el óptimo, en `resultados/calidad.csv` |
| `java -cp out unal.graduacion.Main --generar 100000 5 datos/aleatorio_100000.txt 2026` | Plan aleatorio reproducible: la misma semilla da el mismo plan |
| `java -cp out unal.graduacion.Main --ayuda` | Todas las opciones |

### Desde Visual Studio Code

Abra la carpeta del repositorio en VS Code (si pregunta, confíe en los autores para que se puedan ejecutar las tareas) y use **Terminal > Ejecutar tarea**:

| Tarea | Qué hace |
| --- | --- |
| Graduación UNAL: compilar | Compila todo en `out/` |
| Graduación UNAL: pruebas | Compila y corre las 81 verificaciones |
| Graduación UNAL: menú interactivo | Compila y abre el menú en la terminal |
| Graduación UNAL: mediciones para el reporte | Compila, corre las pruebas, las mediciones y la calidad de la cota, guarda los datos del equipo y deja todo en `resultados/para_el_reporte.zip` |

La última usa `herramientas/mediciones_reporte.ps1` en Windows y `herramientas/mediciones_reporte.sh` en Linux y macOS.

## Resultados con la malla 2A74

Plan 2A74 de Ingeniería de Sistemas y Computación, según el Acuerdo 11 de 2023 de la Facultad de Ingeniería: 49 asignaturas, 51 prerrequisitos y 165 créditos.

| k | Semestres | Cota inferior | Resultado |
| --- | --- | --- | --- |
| 4 | 13 | 13 | óptimo garantizado |
| 5 | 10 | 10 | óptimo garantizado |
| **6** (el del archivo) | **9** | **9** | **óptimo garantizado** |
| 7 | 7 | 7 | óptimo garantizado |

La malla sugiere 10 semestres; con `k = 6` el plan termina en 9. Con nivelación (`datos/plan_2A74_nivelacion.txt`, 55 asignaturas) y `k = 6` son 10 semestres, también óptimo garantizado. El enunciado limita el número de asignaturas por semestre, no los créditos: en el plan de 9 semestres, el octavo suma 26 créditos.

## Mediciones

`--medir` genera planes aleatorios de 10⁴ a 10⁶ asignaturas, con hasta 3 prerrequisitos cada una y una semilla fija, y mide con `System.nanoTime`:

- cada etapa del proceso: registro en el AVL, Kahn con la cola, alturas con la pila, planificación con el AVL de prioridad, cota inferior y proceso total;
- las operaciones de cada estructura: insertar y buscar en el AVL, agregar a la lista, encolar y desencolar, apilar y desapilar;
- dos comparaciones: planificar con una lista sin orden frente al AVL, e insertar IDs ordenados en un BST frente al AVL. Las dos alternativas son O(n²), así que se miden solo hasta n = 20.000.

Antes de medir hace 3 corridas de calentamiento de la JVM que no se registran. Cada medición se repite 5 veces y la consola muestra la mediana, ns/n y ns/(n·log₂ n). El CSV guarda una fila por repetición con las columnas `experimento, estructura, n, e, k, repeticion, tiempo_ns, tiempo_ms, semestres, cota_inferior, optimo_garantizado, altura_arbol`. Junto a él queda `tiempos_resumen.csv`, con la mediana de cada experimento por tamaño; es el que leen las tablas y gráficas del reporte.

Para que la comparación con las alternativas O(n²) tenga más puntos, se corre aparte con tamaños pequeños:

```bash
java -cp out unal.graduacion.Main --medir --tamanos 1000,2000,5000,10000,20000 --csv resultados/cuadraticas.csv
```

`--calidad` cuenta en cuántos planes aleatorios cada cota certifica el óptimo: dos familias (planes generales y planes en forma de árbol), n de 20 a 200, k de 2 a 6 y 300 planes por caso.

## Estructura del proyecto

```text
Proyecto_ED/
├── README.md
├── .vscode/                       # tareas de VS Code (compilar, pruebas, menú, mediciones)
├── datos/
│   ├── plan_2A74.txt              # malla real de Ingeniería de Sistemas y Computación, k = 6
│   ├── plan_2A74_nivelacion.txt   # la misma malla con las 6 asignaturas de nivelación
│   ├── plan_ejemplo.txt           # ejemplo corto de este README
│   ├── ciclo.txt                  # plan imposible: detección de ciclos (RF3)
│   ├── cota_reforzada.txt         # caso en que la cota reforzada supera a la simple
│   └── errores.txt                # errores a propósito: validaciones de la carga (RF1)
├── herramientas/                  # script de mediciones para el reporte (Windows y Linux/macOS)
├── reporte/                       # reporte técnico en LaTeX y sus figuras
├── resultados/                    # CSV de las mediciones y vista HTML
└── src/unal/graduacion/
    ├── Main.java                  # modos de línea de comandos
    ├── MenuConsola.java           # menú interactivo
    ├── estructuras/               # implementaciones propias, sin java.util
    │   ├── ListaEnlazada.java
    │   ├── Cola.java              # arreglo circular que crece al llenarse
    │   ├── Pila.java
    │   ├── ArbolAVL.java
    │   └── ArbolBST.java          # solo para comparar con el AVL
    ├── modelo/                    # Asignatura, RegistroAsignaturas
    ├── io/                        # CargadorPlan, ImpresorPlan, ExportadorHTML
    ├── planificador/              # ValidadorCiclos, CalculadoraAlturas, Planificador,
    │                              # DisponiblesAVL, DisponiblesLista, CotaInferior, VerificadorPlan...
    ├── medicion/                  # GeneradorPlanes, Medidor, ExperimentoCalidad
    └── pruebas/                   # PruebasFuncionales
```

## Decisiones de diseño

El enunciado deja a criterio del equipo el formato de entrada y la representación de los datos. Estas son las decisiones tomadas:

- **Cada línea es una casilla de la malla.** Las asignaturas obligatorias son las 49 casillas del plan 2A74. Las optativas son alternativas que cumplen una casilla, y buscar su código devuelve esa casilla.
- **Formato extendido y compatible.** Créditos, componente y optativas son opcionales, así que un archivo con el formato de la Entrega 1 se lee igual.
- **Requisitos de créditos.** El Acuerdo 11 de 2023 exige 40 créditos del componente disciplinar y Gerencia y gestión de proyectos para el Taller de proyectos interdisciplinarios, y 60 créditos disciplinares para el Trabajo de grado. Se escriben `crC>=40` y `crC>=60`, y el planificador hace esperar a esas asignaturas hasta que se cumplan.
- **Heurística con certificado.** Como no se conoce un algoritmo exacto y eficiente para cualquier `k`, el sistema garantiza el óptimo cuando el plan alcanza la cota inferior y lo informa en cada resultado.
- **Pruebas sin JUnit.** La Entrega 1 preveía JUnit. Se cambió por pruebas propias para que el proyecto compile y corra solo con el JDK, como se hace en el video al clonar y ejecutar.

## Convenciones de trabajo

- **Ramas:** `main` estable; cada integrante trabaja en una rama `feature/<tema>` y une sus cambios con *pull request*.
- **Commits:** mensajes descriptivos en español y en imperativo, un avance concreto por commit. Ejemplo: `Agrega rotaciones al árbol AVL`.
- **Antes de cada commit:** compilar y correr `java -cp out unal.graduacion.Main --pruebas`; deben salir 0 fallidas.
- **Código:** nombres en español, comentarios Javadoc con la complejidad de cada operación.
