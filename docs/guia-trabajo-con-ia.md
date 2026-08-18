# Gym-Bro — Guía de trabajo con IA

**Versión:** 1.0 · 8 de agosto de 2026
**Para:** los 2 estudiantes del proyecto
**Complementa:** `backlog-tareas-gym-bro.md`

---

## 1. Qué se acelera y qué no

Tener asistencia de IA es una ventaja real, pero desigual. Repartida por tipo de trabajo:

| Tipo de trabajo | Ahorro | Por qué |
|---|---|---|
| Configuración inicial, `pom.xml`, Docker Compose | **70%** | Es texto repetitivo y conocido |
| Migraciones SQL, entidades JPA, DTOs, mappers | **70%** | Se derivan mecánicamente del modelo, que ya está diseñado |
| CRUD de backend (controlador + servicio + repositorio) | **60%** | Mismo patrón nueve veces |
| Pantallas de React con formularios y tablas | **55%** | Estructura repetitiva |
| Configuración de Spring Security y JWT | **40%** | Se genera rápido, pero cuando falla, falla oscuro |
| Escribir pruebas | **60%** | Ideal para delegar: tedioso y mecánico |
| Documentación y diagramas | **70%** | |
| **Depurar un error que solo pasa en tu máquina** | **20%** | No veo tu pantalla, ni tu consola, ni tu base de datos |
| **Integrar front y back por primera vez** | **25%** | CORS, puertos, tokens: son problemas de entorno |
| **Entender el código lo suficiente para defenderlo** | **0%** | Este tiempo hay que **sumarlo**, no restarlo |

### La recalibración

| Alcance | Sin IA | Con IA | Margen de comprensión | **Real** | Por persona |
|---|---|---|---|---|---|
| Solo 🔴 | 460 h | ~250 h | +40 h | **290 h** | 145 h |
| 🔴 + membresías | 570 h | ~310 h | +50 h | **360 h** | 180 h |
| 🔴 + 🟡 completo | 736 h | ~400 h | +60 h | **460 h** | 230 h |

**Recomendación actualizada:** apunten a **🔴 + membresías** (~180 h por persona, ~13 semanas a 14 h/semana). Les da un sistema con una historia completa que contar: alguien se inscribe, paga su membresía, un entrenador le arma una rutina, la consulta desde el celular, y el sistema le avisa cuando está por vencer.

Eso es un proyecto final sólido. Si además les sobra tiempo, agregan comercio.

---

## 2. La regla que no se rompe

> **No integren código que no puedan explicar línea por línea.**

Es la única regla realmente importante de este documento, y es específica de su situación: ustedes no van a entregar código, van a **defenderlo frente a profesores que van a preguntar**.

Las preguntas que caen siempre:

- "¿Por qué usaste esto y no aquello?"
- "¿Qué pasa si dos personas hacen esto al mismo tiempo?"
- "Explicame qué hace esta línea."
- "¿Dónde validás que el usuario tenga permiso?"

Si la respuesta es "la IA lo puso ahí", la nota se cae — y con razón, porque el objetivo académico del proyecto es que ustedes aprendan, no que el código exista.

### El test de los 2 minutos

Antes de hacer merge de cualquier PR: **explicale el código a tu compañero en voz alta, sin leerlo.** Si no podés, todavía no está listo. Volvé a preguntarme hasta entenderlo.

Este test tiene un efecto secundario valioso: es exactamente el ejercicio de la sustentación, repetido cuarenta veces durante el semestre. Van a llegar entrenados.

---

## 3. Cómo pedirme las cosas

### Traeme contexto

No tengo memoria de las conversaciones anteriores salvo lo que esté en este proyecto. Al empezar una sesión, lo más eficiente es:

1. Decime en qué tarea estás (`BE-19`, por ejemplo). Los documentos ya definen qué hace y sus criterios de aceptación.
2. Pegame el código actual relevante, no lo describas.
3. Si algo falla, pegame **el error completo**, no un resumen. El stacktrace de Java es feo pero tiene la respuesta adentro.

### El orden que funciona

| Momento | Qué pedir |
|---|---|
| Antes de escribir | "Explicame cómo funciona X antes de generar código" |
| Al generar | "Generá `BE-12` según los criterios del backlog" |
| Después de generar | **"Explicame qué hace cada parte y por qué"** ← el paso que casi todos se saltan |
| Al fallar | "Este es el error completo, este es el código, ¿qué está pasando?" |
| Antes del merge | "¿Qué preguntaría un profesor sobre este código?" |

Ese último es sorprendentemente útil como preparación.

### Cosas concretas que me pueden pedir

- Generar las migraciones SQL completas desde el modelo del spec.
- Escribir un CRUD entero (controlador, servicio, repositorio, DTOs, mapper) siguiendo las convenciones ya definidas.
- Revisar código y buscar problemas de seguridad, especialmente IDOR en los endpoints `/me`.
- Explicarme un error de Spring Security, que son notoriamente crípticos.
- Escribir las pruebas de una regla de negocio.
- Simular una sustentación: que les haga preguntas difíciles sobre su propio código.
- Redactar el documento final y los diagramas actualizados.

---

## 4. Mis límites, sin adornos

| Límite | Qué significa para ustedes |
|---|---|
| **No veo su pantalla ni ejecuto su código** | Todo lo que les doy es una hipótesis hasta que ustedes lo corren. Yo no sé si funcionó. |
| **Me equivoco con seguridad aparente** | A veces invento un método o un parámetro que no existe en la versión de la librería que usan. Suena convincente igual. Verifiquen contra la documentación oficial cuando algo no compile. |
| **Mi conocimiento tiene fecha de corte** | Las versiones exactas de Spring Boot o React pueden haber cambiado. Si una API que les doy no existe, probablemente sea eso. |
| **No sé el estado real de su proyecto** | Si no me lo dicen, asumo lo que dicen los documentos. |
| **Puedo darles código que funciona pero que no entienden** | Y ese es el riesgo específico de su situación. Ver sección 2. |

Cuando algo no cuadre entre lo que digo yo y lo que dice la documentación oficial o el compilador: **ganan ellos**.

---

## 5. Honestidad académica

Esto conviene resolverlo al principio, no la semana antes de entregar.

- **Averigüen la política de su universidad sobre uso de IA.** Varía muchísimo: algunas la prohíben, muchas la permiten con declaración explícita, otras la fomentan. Pregúntenle directamente al profesor de la materia, por escrito si se puede.
- **Si se permite con declaración, documéntenlo bien.** Una sección corta del tipo "usamos asistencia de IA para generación de código repetitivo, migraciones y pruebas; el diseño de arquitectura, las decisiones de modelo de datos y la integración fueron nuestras" es honesta y suele verse mejor que el silencio.
- **Prepárense para que les pregunten.** Un profesor que sospecha uso de IA no lo confirma con un detector: lo confirma pidiéndoles que expliquen una función. Volvemos a la sección 2.

Usar IA como herramienta de desarrollo es lo que hace la industria hoy. Presentarlo como parte del proceso, con criterio, es defendible. Ocultarlo no.

---

## 6. Rutina semanal sugerida

| Momento | Qué hacer |
|---|---|
| **Lunes, 20 min, juntos** | Elegir las tareas de la semana del backlog. Revisar dependencias. |
| **Durante la semana** | Cada uno en su carril. Regla de las 2 horas: si algo lleva más de dos horas trabado, se le cuenta al otro y se me pregunta. |
| **Miércoles, 30 min** | Revisión cruzada de PR. Aplicar el test de los 2 minutos. |
| **Viernes, 30 min** | Correr la aplicación completa y verificar que lo de la semana funciona de punta a punta. Anotar lo que quedó pendiente. |
| **Fin de cada bloque de 2 semanas** | Grabar un video corto de la demo del bloque. Al final del semestre tendrán el registro de la evolución, que se ve muy bien en la sustentación. |

---

## 7. Lo que sigue siendo suyo

La IA acelera la construcción. No reemplaza cuatro cosas, y son justamente las que evalúan:

1. **Las decisiones.** Por qué separaron `usuario` de `empleado`, por qué los permisos son datos y no código, por qué el stock se protege en dos capas. Están argumentadas en los specs, pero las tienen que hacer propias.
2. **La integración.** Juntar las piezas y que funcionen en una máquina real es donde se va la mitad del tiempo y donde menos ayuda tengo para darles.
3. **El criterio de alcance.** Decidir qué queda fuera y saber sostenerlo.
4. **La defensa.** Ahí están solos, y es lo único que el profesor ve directamente.

Yo puedo ser un compañero de trabajo bastante útil durante el semestre. Pero el proyecto es de ustedes, y conviene que se note.
