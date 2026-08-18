# Gym-Bro — Plan de Trabajo y Tareas

**Versión:** 2.0 · 8 de agosto de 2026
**Contexto:** proyecto final de carrera · **2 estudiantes**
**Documentos base:** Especificación Funcional v1.1 · Especificación Técnica v1.0

---

## Antes de empezar: qué significan las palabras raras

Los documentos anteriores usaban vocabulario de la industria sin explicarlo. Va la traducción.

| Palabra | Qué es en realidad |
|---|---|
| **Backlog** | La lista completa de tareas pendientes del proyecto. Nada más. |
| **Épica** | Un grupo grande de tareas que persiguen un mismo objetivo. "Todo lo relacionado con el login" es una épica. |
| **Tarea** | Una unidad de trabajo con principio y fin claros, que una persona puede terminar. |
| **Dependencia** | Otra tarea que tiene que estar lista antes. No podés hacer la pantalla de login si el endpoint de login no existe. |
| **Criterios de aceptación** | Cómo sabés que la tarea quedó bien. Si no los podés verificar, la tarea está mal escrita. |
| **Sprint** | Un bloque de tiempo fijo (aquí: 2 semanas) en el que se trabaja un conjunto de tareas y al final se muestra algo funcionando. |
| **Camino crítico** | La cadena de tareas que no se puede acortar. Si una se atrasa, todo el proyecto se atrasa. |
| **PR** (*pull request*) | Pedirle a tu compañero que revise tu código antes de integrarlo. |

### Sobre los "puntos" que aparecían antes

En la industria se estima con **puntos de historia**: en vez de decir "esto toma 8 horas", se dice "esto es un 5", comparando tareas entre sí. Se hace así porque los humanos somos malísimos estimando horas, pero razonablemente buenos diciendo "esto es más grande que aquello".

Para lo que ustedes necesitan, eso es ruido innecesario. **Este documento estima en horas de trabajo real.** Es más fácil de planear contra un calendario académico y más fácil de explicarle a un profesor.

> **Advertencia honesta sobre las horas:** estas estimaciones asumen que ya sabés lo que estás haciendo. Si es tu primera vez con Spring Security, con React o con JPA, multiplicá por dos las tareas de esa tecnología. No es pesimismo: es lo que pasa siempre, y planear sin contarlo es la causa número uno de proyectos finales entregados a medias.

---

## Cómo leer las tablas

| Columna | Significado |
|---|---|
| **ID** | `INF` infraestructura · `BE` backend (Java) · `FE` frontend (React) · `QA` pruebas |
| **Dep.** | Qué debe estar terminado antes |
| **Horas** | Estimación de trabajo efectivo |
| **Criterios de aceptación** | Cómo verificar que quedó bien |

### Prioridades

| Marca | Significado |
|---|---|
| 🔴 | **Imprescindible.** Sin esto no hay proyecto que exponer. |
| 🟡 | **Recomendado.** Suma calidad visible. Háganlo si el tiempo alcanza. |
| ⚪ | **Opcional.** Preséntenlo como "trabajo futuro" en la sustentación. |

### Los números de este proyecto

| Alcance | Horas totales | Por persona | Semanas a 15 h/sem c/u |
|---|---|---|---|
| **Solo 🔴** | 460 h | 230 h | ~15 semanas |
| 🔴 + 🟡 | 736 h | 368 h | ~25 semanas |
| Todo | 1 244 h | 622 h | ~41 semanas |

> **Estas cifras son sin asistencia de IA.** Con IA usada bien, se reducen entre un 40% y un 50% en promedio — pero no de forma pareja: el código repetitivo se acelera muchísimo y la depuración casi nada. La recalibración completa está en `guia-trabajo-con-ia.md`, junto con el margen que hay que **sumar** para poder defender el código en la sustentación.

**Lean esa tabla con cuidado.** El proyecto completo es un año de trabajo para dos personas a medio tiempo — es un sistema comercial real, no un ejercicio académico. Lo marcado en 🔴 es el núcleo irrenunciable; con IA, 🔴 + membresías es un objetivo realista para un semestre.

Un sistema pequeño que **funciona completo** se defiende mucho mejor que uno grande con siete módulos a medias. Los módulos que dejen fuera no son un fracaso: son la sección "trabajo futuro" de su documento, y demuestran que entendieron el alcance.

---

## EPIC 0 — Fundaciones (bloqueante para todo)

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| 🔴 | INF-01 | Crear monorepo, `.gitignore`, README, estructura de carpetas | — | 4 h | Estructura del spec técnico §2 creada y en `main` |
| 🔴 | INF-02 | Bootstrap del backend Spring Boot 3.3 + Java 21 + Maven | INF-01 | 8 h | `mvn spring-boot:run` levanta en 8080; `/actuator/health` responde 200 |
| 🔴 | INF-03 | Bootstrap del frontend Vite + React 18 + TS + Tailwind | INF-01 | 8 h | `npm run dev` sirve en 5173; build sin errores de TS |
| 🟡 | INF-04 | Docker Compose: PostgreSQL 16 + back + front | INF-02, INF-03 | 12 h | `docker compose up` deja todo funcionando en máquina limpia |
| 🔴 | INF-05 | Configurar Flyway y migración `V1__esquema_base.sql` | INF-04 | 12 h | Migración corre sola al arrancar; `ddl-auto=validate`; tablas usuario/rol/permiso/empleado creadas |
| 🟡 | INF-06 | Configurar springdoc-openapi (Swagger UI) | INF-02 | 4 h | `/swagger-ui.html` accesible sin token |
| 🔴 | INF-07 | Configurar CORS por ambiente + variables de entorno + `.env.example` | INF-02 | 8 h | El front llama al back sin error de CORS; ningún secreto en el repo |
| 🔴 | INF-08 | `GlobalExceptionHandler` + `ErrorResponse` + enum `CodigoError` | INF-02 | 12 h | Todo error sale en el formato de §6.1; ningún stacktrace llega al cliente |
| 🔴 | INF-09 | Espejo de `codigosError.ts` + cliente Axios con interceptores en el front | INF-03, INF-08 | 8 h | Token se inyecta solo; 401 redirige a login; `X-Request-Id` presente |
| ⚪ | INF-10 | Pipeline GitHub Actions (build + test back y front) | INF-02, INF-03 | 12 h | PR con test roto no se puede mergear |
| ⚪ | INF-11 | Logs JSON con `requestId` en MDC | INF-08 | 8 h | Un mismo `requestId` correlaciona la petición del front con el log del back |

**Total épica: 96 h** · de las cuales imprescindibles: **60 h**

---

## EPIC 1 — Autenticación y RBAC

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| 🔴 | BE-01a | `BaseEntity` (`@MappedSuperclass`) con id, auditoría y `@Version` + `@EnableJpaAuditing` | INF-05 | 8 h | `equals`/`hashCode` por id escritos a mano; las fechas se llenan solas al guardar. Ver `entidades-jpa-gym-bro.md` §4 |
| 🔴 | BE-01b | Las 10 enumeraciones del dominio con `@Enumerated(STRING)` | BE-01a | 4 h | Ninguna usa `ORDINAL`; los valores coinciden con los `VARCHAR` de la migración |
| 🔴 | BE-01c | Embebidos `Direccion` y `RangoHorario` (`@Embeddable`) | BE-01a | 6 h | `RangoHorario.seSolapaCon()` probado con test unitario sin base de datos |
| 🔴 | BE-01 | Entidades `Usuario`, `Rol`, `Permiso` + repositorios | BE-01a, BE-01b, BE-01c | 12 h | Heredan de `BaseEntity`; `@ToString` excluye `passwordHash` y colecciones; sin `@Data`; la app arranca con `validate` sin errores |
| 🟡 | BE-01d | Configurar generación del DDL de Hibernate a archivo (no a la base) | BE-01 | 2 h | `target/ddl-generado-por-hibernate.sql` se genera al arrancar; comparado contra `V1__esquema_base.sql` y las diferencias documentadas |
| 🟡 | BE-01e | Pruebas de la capa de entidades con `@DataJpaTest` | BE-01 | 6 h | Verifican `@Builder.Default`, `@Transient`, embebidos y que `toString()` no expone la contraseña |
| 🔴 | BE-02 | Migración `V7__datos_iniciales.sql`: roles, permisos, rol_permiso, admin semilla | BE-01 | 8 h | Los 18 permisos de la matriz funcional §3.1 insertados; admin puede iniciar sesión. Sin esto, una base nueva no deja entrar a nadie |
| 🔴 | BE-03 | `JwtService`: generar, firmar y validar token | INF-02 | 12 h | Token con sub, roles, permisos, exp; secreto desde `JWT_SECRET`; test de token expirado |
| 🔴 | BE-04 | `JwtAuthenticationFilter` + `SecurityConfig` + entry point y access denied handler | BE-03, INF-08 | 20 h | Ruta protegida sin token → 401 uniforme; token inválido → 401; `@EnableMethodSecurity` activo |
| 🔴 | BE-05 | `POST /auth/login` + `ResolvedorVista` | BE-02, BE-04 | 20 h | Devuelve token, usuario, roles, permisos y vista; credenciales malas → 401 `CREDENCIALES_INVALIDAS` sin revelar si el email existe |
| 🟡 | BE-06 | Bloqueo por intentos fallidos (RF-AUT-02) | BE-05 | 12 h | 5 fallos en 15 min → `CUENTA_BLOQUEADA`; se libera a los 15 min; contador se resetea al acertar |
| 🔴 | BE-07 | `GET /auth/me` y `POST /auth/cambiar-password` (RF-AUT-05) | BE-05 | 8 h | Cambio exige contraseña actual; valida política de 8+ con mayúscula, minúscula y número |
| ⚪ | BE-08 | Recuperación de contraseña con token de un solo uso (RF-AUT-04) | BE-07 | 20 h | Token válido 30 min, un solo uso; responde 200 aunque el email no exista (no enumera cuentas) |
| 🔴 | FE-01 | `AuthContext` + `useAuth` + persistencia en sessionStorage | INF-09 | 12 h | Estado de sesión sobrevive al refresco de página |
| 🔴 | FE-02 | Pantalla de login + manejo de errores por código | FE-01, BE-05 | 12 h | Mensajes distintos para credenciales inválidas y cuenta bloqueada |
| 🔴 | FE-03 | `RutaProtegida` + componente `<Permiso>` + redirección por vista | FE-01 | 12 h | Sin permiso → `/sin-acceso`; cada vista aterriza en su ruta inicial |
| 🔴 | FE-04 | Layouts base (Admin, Empleado, Usuario) con navegación filtrada por permiso | FE-03 | 20 h | El menú solo muestra lo que el permiso habilita |
| 🟡 | QA-01 | Suite de integración de auth con Testcontainers | BE-05, BE-06 | 12 h | Cubre login OK, login fallido, bloqueo, token expirado, sin rol asignado |

**Total épica: 206 h** · de las cuales imprescindibles: **166 h**

> La capa de entidades (`BE-01a` a `BE-01e`) implementa los temas vistos en clase sobre JPA/Hibernate. El detalle completo, con código y respuestas para la sustentación, está en `entidades-jpa-gym-bro.md`.

---

## EPIC 2 — Administración: usuarios, roles, permisos, empleados

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| 🔴 | BE-09 | CRUD de roles (RF-ADM-01) | BE-04 | 12 h | No permite borrar rol con usuarios → 409 `ROL_EN_USO` |
| 🔴 | BE-10 | `PUT /roles/{id}/permisos` (RF-ADM-02) | BE-09 | 12 h | Reemplaza el conjunto completo de permisos del rol; queda en auditoría |
| 🔴 | BE-11 | `PUT /usuarios/{id}/roles` (RF-ADM-03) | BE-09 | 8 h | Un usuario admite 1..N roles; no puede quedarse sin ninguno |
| 🔴 | BE-12 | CRUD de usuarios/socios (RF-EMP-01, RF-ADM-05) | BE-01 | 20 h | Email y documento únicos → 409; rol `USUARIO` por defecto; baja lógica |
| 🔴 | BE-13 | `PUT /usuarios/me` — perfil propio (RF-USR-06) | BE-12 | 12 h | **Solo** teléfono, email y contraseña. Rechaza cualquier intento de cambiar rol o documento. Id tomado del token |
| 🔴 | BE-14 | CRUD de empleados (RF-ADM-04, RF-ADM-07) | BE-12 | 20 h | Crea `usuario` + `empleado`; al desactivar, marca asignaciones activas para reasignar |
| 🟡 | BE-15 | Listado paginado y filtrable de socios (RF-EMP-07) | BE-12 | 12 h | Filtra por nombre, documento y estado de membresía; formato `PaginaResponse` |
| 🔴 | FE-05 | Pantalla admin: gestión de roles y matriz de permisos | FE-04, BE-10 | 20 h | Matriz de checkboxes rol × permiso; guarda en una sola llamada |
| 🟡 | FE-06 | Pantalla admin: alta y listado de empleados | FE-04, BE-14 | 20 h | Formulario validado con Zod; tabla paginada |
| 🔴 | FE-07 | Pantalla empleado: registro y búsqueda de socios | FE-04, BE-15 | 20 h | Búsqueda con debounce; paginación server-side |
| 🟡 | FE-08 | Pantalla usuario: mi perfil | FE-04, BE-13 | 12 h | Campos de rol y documento en solo lectura |
| 🟡 | QA-02 | Pruebas de IDOR sobre endpoints `/me` | BE-13 | 12 h | Un socio no puede leer ni editar datos de otro id, ni enviando el id en el body |

**Total épica: 180 h** · de las cuales imprescindibles: **124 h**

---

## EPIC 3 — Entrenamiento: catálogos, rutinas y entrenadores

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| 🔴 | BE-16 | Migración `V2__catalogo_entrenamiento.sql` con índices parciales únicos | INF-05 | 12 h | `uq_rutina_activa_por_usuario` y `uq_entrenador_activo_por_usuario` creados |
| 🟡 | BE-17 | CRUD de máquinas | BE-16 | 8 h | Estado OPERATIVA/MANTENIMIENTO/BAJA |
| 🔴 | BE-18 | CRUD de ejercicios (con `maquina_id` opcional) | BE-17 | 12 h | Permite ejercicios de peso libre sin máquina |
| 🔴 | BE-19 | `POST /rutinas` con cabecera + ejercicios (RF-EMP-02, RN-03) | BE-18 | 32 h | Transaccional; desactiva la rutina previa del socio; valida series/reps > 0 |
| 🔴 | BE-20 | `GET /rutinas/me` y `GET /rutinas/usuario/{id}` (RF-USR-01) | BE-19 | 12 h | Agrupada por día; el socio solo ve la suya |
| 🔴 | BE-21 | Asignación de entrenador (RF-EMP-03, RN-04) | BE-14 | 20 h | Reasignar cierra la anterior con `fecha_fin`; segundo intento simultáneo → 409 |
| 🟡 | BE-22 | `GET /asignaciones-entrenador/me` (RF-USR-02) | BE-21 | 8 h | Devuelve nombre, especialidad y horarios del entrenador |
| 🔴 | FE-09 | Pantalla empleado: constructor de rutinas | FE-07, BE-19 | 32 h | Arrastrar ejercicios por día; validación antes de enviar; una sola llamada al guardar |
| 🔴 | FE-10 | Pantalla usuario: mi rutina | FE-08, BE-20 | 20 h | Vista por día con series, reps, peso y descanso; responsive para usar en el gimnasio |
| 🟡 | FE-11 | Pantalla empleado: asignar entrenador | FE-07, BE-21 | 12 h | Muestra entrenador actual y permite reasignar con confirmación |
| 🟡 | FE-12 | Pantalla admin: catálogo de ejercicios y máquinas | FE-05, BE-18 | 20 h | CRUD completo con tabla y modal |
| ⚪ | QA-03 | Prueba de concurrencia: dos rutinas activas simultáneas | BE-19 | 8 h | El índice parcial fuerza que una falle con 409 |

**Total épica: 196 h** · de las cuales imprescindibles: **140 h**

---

## EPIC 4 — Horarios y clases

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| ⚪ | BE-23 | Migración `V3__horarios.sql` con `EXCLUDE` anti-solape | BE-16 | 12 h | La extensión `btree_gist` se instala; la restricción rechaza solapes |
| ⚪ | BE-24 | CRUD de horarios (RF-EMP-04) | BE-23 | 20 h | Solape del mismo entrenador → 409 `HORARIO_SOLAPADO`; `hora_fin > hora_inicio` |
| ⚪ | BE-25 | Inscripción y cancelación a clase (RF-USR-08, RN-08) | BE-24 | 20 h | Cupo lleno → 409 `CUPO_AGOTADO`; suscripción vencida → 409; no permite inscripción duplicada |
| ⚪ | BE-26 | `GET /horarios` público para autenticados (RF-USR-03) | BE-24 | 8 h | Devuelve la semana completa; solo lectura para el socio |
| ⚪ | FE-13 | Pantalla empleado: gestión de horarios (vista semanal) | FE-07, BE-24 | 32 h | Grilla semanal; feedback claro al detectar solape |
| ⚪ | FE-14 | Pantalla usuario: horarios e inscripción | FE-10, BE-25 | 20 h | Muestra cupos disponibles y estado de mi inscripción |

**Total épica: 112 h** · de las cuales imprescindibles: **0 h**

---

## EPIC 5 — Comercio: productos, ventas, compras y pagos

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| ⚪ | BE-27 | Migración `V4__comercio.sql` con `CHECK (stock >= 0)` | BE-16 | 12 h | Restricciones creadas |
| ⚪ | BE-28 | CRUD de productos (RF-ADM-06) | BE-27 | 12 h | Precio > 0, stock ≥ 0; baja lógica |
| ⚪ | BE-29 | Venta en mostrador con bloqueo pesimista (RF-EMP-06, RN-02, RN-06) | BE-28 | 32 h | Descuenta stock atómicamente; congela precio en `detalle_venta`; stock insuficiente → 409 |
| ⚪ | BE-30 | Compra en línea del socio (RF-USR-05) | BE-29 | 20 h | Canal ONLINE; pago en estado PENDIENTE; acepta `Idempotency-Key` |
| ⚪ | BE-31 | Registro y consulta de pagos (RF-USR-07) | BE-29 | 12 h | `concepto` distingue MEMBRESIA de VENTA; `/pagos/me` solo devuelve los propios |
| ⚪ | FE-15 | Pantalla admin/empleado: catálogo de productos | FE-06, BE-28 | 20 h | CRUD con control de stock visible |
| ⚪ | FE-16 | Pantalla empleado: punto de venta | FE-15, BE-29 | 32 h | Carrito, búsqueda rápida por nombre, total en vivo, botón deshabilitado tras enviar |
| ⚪ | FE-17 | Pantalla usuario: tienda y carrito | FE-14, BE-30 | 32 h | Catálogo, carrito persistente en sesión, confirmación de compra |
| ⚪ | FE-18 | Pantalla usuario: historial de pagos | FE-17, BE-31 | 12 h | Tabla con fecha, concepto, monto y estado |
| ⚪ | QA-04 | Prueba de concurrencia: dos ventas del último producto | BE-29 | 12 h | Una tiene éxito, la otra 409; el stock nunca queda negativo |

**Total épica: 196 h** · de las cuales imprescindibles: **0 h**

---

## EPIC 6 — Membresías y asistencia

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| 🟡 | BE-32 | Migración `V5__membresias.sql` | BE-27 | 8 h | Tablas `membresia` y `suscripcion` creadas |
| 🟡 | BE-33 | CRUD de planes de membresía (RF-MEM-01) | BE-32 | 12 h | Precio > 0, duración > 0 |
| 🟡 | BE-34 | Contratar y renovar suscripción (RF-MEM-02, RF-MEM-05) | BE-33, BE-31 | 20 h | Genera pago con concepto MEMBRESIA; renovación anticipada arranca al día siguiente del vencimiento vigente |
| 🟡 | BE-35 | Job diario de vencimiento (RF-MEM-03) | BE-34 | 12 h | `@Scheduled` marca VENCIDA lo que ya expiró; idempotente si corre dos veces |
| 🟡 | BE-36 | `GET /suscripciones/me` + días restantes (RF-USR-09, RF-MEM-04) | BE-34 | 8 h | Devuelve plan, vencimiento y bandera de alerta si faltan ≤ 7 días |
| ⚪ | BE-37 | Migración `V6__asistencia_auditoria.sql` | BE-32 | 8 h | Tablas `asistencia` y `auditoria` creadas |
| ⚪ | BE-38 | Check-in y check-out (RF-ASI-01, RF-ASI-02) | BE-37 | 20 h | Busca por documento; permite entrar con membresía vencida pero deja constancia; check-out cierra la asistencia abierta |
| ⚪ | BE-39 | `GET /asistencias/me` últimos 90 días (RF-ASI-03) | BE-38 | 8 h | Solo las propias |
| 🟡 | FE-19 | Pantalla admin: planes de membresía | FE-15, BE-33 | 12 h | CRUD simple |
| 🟡 | FE-20 | Pantalla empleado: contratar/renovar membresía | FE-19, BE-34 | 20 h | Selector de plan, cálculo de vencimiento en pantalla antes de confirmar |
| ⚪ | FE-21 | Pantalla empleado: check-in rápido | FE-20, BE-38 | 20 h | Un solo campo de documento; respuesta en menos de 2 segundos; alerta visible si la membresía está vencida |
| 🟡 | FE-22 | Banner de vencimiento próximo en la vista del socio | FE-14, BE-36 | 8 h | Aparece con ≤ 7 días restantes y al estar vencida (RN-01) |

**Total épica: 156 h** · de las cuales imprescindibles: **0 h**

---

## EPIC 7 — Auditoría, calidad y despliegue

| | ID | Tarea | Dep. | Horas | Criterios de aceptación |
|---|---|---|---|---|---|
| ⚪ | BE-40 | Aspecto `@Auditable` con AOP sobre la tabla `auditoria` | BE-37, INF-11 | 20 h | Registra cambios de rol, permisos, ventas y pagos con datos antes/después en JSONB |
| ⚪ | QA-05 | Cobertura ≥ 70% en la capa de servicios | Todas las BE | 20 h | Reportado en CI; el pipeline falla por debajo del umbral |
| ⚪ | QA-06 | E2E con Playwright: 3 logins + una venta completa | FE-16 | 32 h | Corre en CI contra el Compose |
| 🟡 | QA-07 | Revisión de seguridad: checklist OWASP básico | Todas | 20 h | Sin secretos en repo, sin IDOR, headers de seguridad, rate limit en login |
| ⚪ | INF-12 | Dockerfiles de producción multi-stage | INF-04 | 12 h | Imagen del back < 250 MB; front servido por Nginx |
| ⚪ | INF-13 | Backup automático diario de PostgreSQL | INF-12 | 12 h | Retención de 30 días; restauración probada al menos una vez |
| 🟡 | INF-14 | Manual de despliegue y runbook | INF-13 | 12 h | Otra persona puede desplegar siguiendo solo el documento |

**Total épica: 128 h** · de las cuales imprescindibles: **0 h**

---
## Plan de 16 semanas para 2 estudiantes

Asume ~15 horas semanales por persona (30 h/semana entre los dos). Ajusten según su carga académica real.

### Reparto

| | **Persona A** | **Persona B** |
|---|---|---|
| Se especializa en | Backend: Java, Spring, base de datos | Frontend: React, pantallas, integración |
| Pero además | Revisa el código de B | Revisa el código de A |

**Las primeras 4 semanas trabajan juntos, no en paralelo.** Las fundaciones y el login son demasiado críticos y están demasiado entrelazados para repartirlos, y es donde un error de diseño se paga durante todo el semestre. Además, así los dos entienden la base del sistema — importante cuando en la sustentación el profesor le pregunte a cualquiera de los dos.

### Calendario

| Semanas | Foco | Persona A | Persona B | Al terminar deberían poder mostrar |
|---|---|---|---|---|
| **1–2** | Preparar el terreno | INF-02, INF-05, INF-06, INF-08 | INF-01, INF-03, INF-04, INF-07, INF-09 | El proyecto arranca, la base de datos se crea sola, Swagger se ve |
| **3–4** | Login *(en pareja)* | BE-01, BE-02, BE-03, BE-04 | BE-05, FE-01, FE-02 | Iniciar sesión de verdad y recibir un token con permisos |
| **5–6** | Roles y permisos | BE-07, BE-09, BE-10, BE-11 | FE-03, FE-04, FE-05 | Tres usuarios distintos entran y ven tres menús distintos |
| **7–8** | Usuarios y empleados | BE-12, BE-13, BE-14 | FE-06, FE-07, FE-08 | Registrar socios y empleados; cada uno edita su perfil |
| **9–10** | Rutinas (motor) | BE-16, BE-18, BE-19, BE-20 | FE-09 *(pantalla de armar rutina)* | Un entrenador arma una rutina completa y la guarda |
| **11–12** | Rutinas (vista del socio) | BE-21, BE-22, QA-01, QA-02 | FE-10, FE-11 | El socio abre el celular en el gimnasio y ve su rutina del día |
| **13–14** | Cerrar y pulir | BE-06, BE-15, BE-17 | FE-12, corrección de bugs, responsive | Sistema estable, sin errores en los flujos principales |
| **15** | Documentación | Manual técnico, diagramas actualizados, README | Capturas, guion de la demo | Documento de sustentación listo |
| **16** | Ensayo | Ensayar la demo completa **dos veces**, con datos de prueba cargados | | Presentación cronometrada |

**No borren la semana 16.** La causa más común de una mala sustentación no es un mal proyecto: es una demo que se rompe en vivo porque nadie la ensayó con datos reales.

### Si van más rápido de lo previsto

Agreguen en este orden: **membresías** (BE-32 a BE-36, FE-19, FE-20, FE-22 — ~110 h). Es lo que convierte el sistema en algo que un gimnasio realmente usaría, y da una historia redonda: alguien se inscribe, paga, entrena y su membresía vence.

Después, si sobra tiempo: comercio (EPIC 5) u horarios (EPIC 4). Nunca los dos.

### Si van más lento

Recorten en este orden, sin culpa:

1. Todo lo ⚪ (ya está fuera del plan).
2. Las pruebas automatizadas (QA-01, QA-02) — pero entonces prueben a mano y **déjenlo dicho en el documento**. Un profesor valora más "sabemos que faltan pruebas y sabemos por qué" que el silencio.
3. Las pantallas de catálogos (FE-12): carguen ejercicios y máquinas directamente por SQL.
4. **Nunca recorten el login ni los permisos.** Es el corazón del diagrama que presentaron y lo primero que van a preguntar.

---

## Camino crítico

```
INF-01 → INF-02 → INF-05 → BE-01 → BE-02 → BE-04 → BE-05 → FE-03 → todo lo demás
```

**BE-04 y BE-05 son el cuello de botella.** Hasta que el login no devuelva un token con permisos, ninguna pantalla protegida se puede probar de verdad. Háganlas **sentados frente a la misma pantalla**, turnándose el teclado. Es la parte donde un error cuesta más caro y donde más conviene que los dos entiendan cada línea.

Si en la semana 4 el login no funciona, no sigan adelante: paren y resuélvanlo. Avanzar con la base rota solo acumula trabajo que después habrá que rehacer.

---

## Riesgos de ser dos

| Riesgo | Qué hacer |
|---|---|
| Uno se enferma o tiene parciales y su carril se detiene | Revisión cruzada de todo el código. Si solo uno entiende la seguridad, el proyecto tiene un único punto de falla con nombre y apellido. |
| Se traban una semana en un error y no avisan | Regla de las 2 horas: si algo lleva más de 2 h sin avanzar, se le cuenta al otro. Siempre. |
| Llegan a la sustentación sin haber ensayado | Semana 16 bloqueada para eso. Es innegociable. |
| El alcance crece durante el semestre | La lista de "fuera del alcance" de abajo es un compromiso, no una sugerencia. |

---

## Qué mostrar en la sustentación

Cinco cosas que se ven bien y que ya están construidas si siguen este plan:

1. **Los tres roles en vivo.** Entrar como admin, como empleado y como socio, y mostrar que cada uno ve algo distinto. Es el diagrama original hecho realidad.
2. **La matriz de permisos.** Cambiar un permiso desde la pantalla de admin y mostrar que el menú del otro usuario cambia. Demuestra que los permisos son datos, no código escrito a mano — eso impresiona.
3. **Swagger.** Abrir `/swagger-ui.html` y mostrar la API documentada sola. Da imagen de proyecto profesional con cero esfuerzo extra.
4. **Una regla de negocio defendida por la base de datos.** Intentar crear dos rutinas activas para el mismo socio y mostrar que PostgreSQL lo rechaza por el índice parcial único. Explicar por qué está en dos lugares (servicio y base) demuestra criterio, no paranoia.
5. **Las migraciones.** Borrar la base, levantar el proyecto y mostrar cómo se reconstruye sola desde cero. Es un momento de demo muy efectivo y muy poco común en proyectos estudiantiles.

Y una cosa que **no** hay que hacer: pedir disculpas por lo que falta. Preséntenlo como decisiones de alcance con justificación. "Priorizamos el núcleo de gestión de entrenamiento y dejamos el módulo comercial documentado como trabajo futuro" suena a criterio profesional. "No nos dio el tiempo" suena a otra cosa, aunque sea lo mismo.

---

## Fuera del alcance

Declarado en el spec funcional §2.2, se repite para que no se cuele por la puerta de atrás a mitad de semestre:

- Procesamiento real de pagos con pasarela.
- App móvil nativa.
- Control de acceso físico (torniquete, QR).
- Reportería y BI.
- Notificaciones push y correo masivo.

Si alguno entra, se estima aparte y se saca otra cosa a cambio.
