#  Graduación UNAL

> Planificador del **mínimo número de semestres** para terminar un plan de estudios, respetando prerrequisitos y el límite de asignaturas por semestre.

**Curso:** Estructuras de Datos (2016699), Universidad Nacional de Colombia
**Profesor:** David Alberto Herrera Álvarez
**Estado:** Entrega 1 (planteamiento y diseño)

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
| **Java 17+** | Lenguaje de implementación |
| **Git + GitHub** | Control de versiones y trabajo en equipo |
| **JUnit 5** | Pruebas unitarias (entregas 2 y 3) |

Todas las estructuras de datos se implementarán **desde cero**, sin usar las colecciones de `java.util` (`ArrayList`, `HashMap`, `PriorityQueue`, etc.), para poder analizar y comparar su rendimiento. El proyecto es técnico: aplicación de **consola** con entrada por archivo de texto, sin interfaz gráfica.

## Solución propuesta

El plan de estudios se modela como un **grafo dirigido**: cada asignatura es un nodo y cada prerrequisito es una arista `A → B` ("A debe aprobarse antes que B"). Un plan válido no puede tener ciclos; si los tiene, es imposible de terminar y el sistema lo reporta.

```text
Cálculo Diferencial ──► Cálculo Integral ──► Ecuaciones Diferenciales
                                        └──► Probabilidad y Estadística
Fundamentos de Prog. ──► POO ──► Estructuras de Datos ──► Algoritmos
Matemáticas Discretas ─────────────────┘
```

**Flujo general:**

1. **Leer** el archivo con el plan y el límite `k`.
2. **Registrar** cada asignatura y enlazar sus prerrequisitos.
3. **Validar** que no haya ciclos (orden topológico, algoritmo de Kahn).
4. **Calcular la prioridad** de cada asignatura: la longitud de la cadena más larga de asignaturas que dependen de ella.
5. **Armar los semestres**: en cada uno se toman las `k` asignaturas disponibles de mayor prioridad. Las que quedan habilitadas pasan al semestre siguiente.
6. **Mostrar** el plan y una **cota inferior** `LB = max(⌈n / k⌉, cadena más larga)`. Si el plan usa `LB` semestres, es óptimo con certeza.

## Estructuras de datos

### Entrega 2: estructuras vistas hasta árboles AVL

| Estructura | Uso en el sistema | Costo | Justificación |
| --- | --- | --- | --- |
| Árbol AVL por ID | Registro y acceso a asignaturas | O(log n) | Los IDs son dispersos y suelen venir ordenados; un ABB simple degeneraría a O(n) |
| Lista enlazada | Prerrequisitos y dependientes de cada asignatura | O(1) al agregar | Cantidad variable y solo se recorren en orden |
| Cola | Algoritmo de Kahn: detecta ciclos y libera asignaturas | O(1) | Procesa las asignaturas por niveles de disponibilidad |
| Pila | Recorrer el orden topológico al revés para calcular prioridades | O(1) | La prioridad de una asignatura depende de sus dependientes |
| Árbol AVL por prioridad | Asignaturas disponibles; se extraen las `k` mayores | O(log n) | Mantiene el orden sin reordenar cada semestre |

### Entrega 3: heaps, conjuntos, hash y grafos

| Estructura | Reemplaza a | Mejora esperada |
| --- | --- | --- |
| Tabla hash | AVL por ID | Acceso por ID en O(1) promedio |
| Grafo dirigido (listas de adyacencia + DFS) | Listas sueltas | Modelo explícito y detección de ciclos en O(n + e) |
| Heap de máximos | AVL por prioridad | Mismo O(log n) con menor costo constante |
| Conjuntos (bits) | — | Solución exacta para planes pequeños (n ≤ 20) |

Ambas versiones se compararán con mediciones de tiempo de ejecución y gráficas.
