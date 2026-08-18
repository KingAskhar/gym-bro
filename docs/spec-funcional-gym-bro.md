# Gym-Bro — Especificación Funcional

**Versión:** 1.1
**Fecha:** 8 de agosto de 2026
**Base:** Diagrama de arquitectura y flujos "Gym-Bro"

**Cambios v1.1:** se formaliza la comunicación Front ↔ Back por HTTP REST (§4.1), se incorpora el stack complementario (§4.2) y las tablas complementarias pasan de recomendadas a parte definitiva del modelo (§5.3), con sus requisitos funcionales y endpoints asociados.

---

## 1. Resumen ejecutivo

Gym-Bro es una aplicación de gestión integral para gimnasios. Cubre tres frentes:

1. **Administración**: gestión de empleados, roles, permisos y catálogo de productos.
2. **Operación**: entrenadores y personal registran clientes, crean rutinas, asignan entrenadores, gestionan horarios y venden productos.
3. **Cliente final**: el socio consulta su rutina, su entrenador asignado, los horarios del gimnasio y compra productos.

El acceso a funcionalidad se resuelve dinámicamente: tras el login se obtienen **rol + permisos** del usuario, y el backend determina qué vista se devuelve al front.

---

## 2. Alcance

### 2.1 Dentro del alcance (v1)

- Autenticación y autorización basada en roles y permisos (RBAC).
- Gestión de usuarios (socios), empleados, roles y permisos.
- Catálogo de ejercicios y máquinas; creación y asignación de rutinas.
- Asignación de entrenador a socio.
- Gestión de horarios (clases / disponibilidad de entrenadores).
- Catálogo de productos, venta presencial (empleado) y compra en línea (socio).
- Registro de pagos y ventas.

### 2.2 Fuera del alcance (v1) — candidatos a v2

- App móvil nativa (la v1 es web responsive).
- Pasarela de pago real integrada (v1 registra el pago, no lo procesa).
- Control de acceso físico (torniquetes, huella, QR de entrada).
- Reportería avanzada / BI.
- Notificaciones push y correo transaccional masivo.

---

## 3. Actores y roles

| Actor | Descripción | Vista principal |
|---|---|---|
| **Administrador** | Configura el sistema: roles, permisos, empleados, catálogo. | `VistaAdmin` |
| **Empleado / Entrenador** | Opera el día a día: registra socios, crea rutinas, asigna entrenadores, gestiona horarios, vende productos. | `VistaEmpleado` |
| **Usuario / Socio** | Cliente del gimnasio. Consulta su rutina, entrenador, horarios y compra productos. | `VistaUser` |

> **Nota de diseño:** el diagrama muestra `TablaUsuario` y `TablaEmpleado` como entidades separadas. Se recomienda un modelo de **identidad única** (`usuario`) con un perfil de empleado opcional (`empleado`) que la referencia. Evita duplicar credenciales cuando un empleado también es socio del gimnasio.

### 3.1 Matriz de permisos por rol

| Permiso | Admin | Empleado | Socio |
|---|:---:|:---:|:---:|
| `ROLES_GESTIONAR` | ✅ | ❌ | ❌ |
| `PERMISOS_ASIGNAR` | ✅ | ❌ | ❌ |
| `EMPLEADOS_GESTIONAR` | ✅ | ❌ | ❌ |
| `USUARIOS_REGISTRAR` | ✅ | ✅ | ❌ |
| `USUARIOS_VER_TODOS` | ✅ | ✅ | ❌ |
| `PERFIL_PROPIO_EDITAR` | ✅ | ✅ | ✅ |
| `PRODUCTOS_GESTIONAR` | ✅ | ✅ | ❌ |
| `PRODUCTOS_VER` | ✅ | ✅ | ✅ |
| `PRODUCTOS_VENDER` | ✅ | ✅ | ❌ |
| `PRODUCTOS_COMPRAR` | ❌ | ❌ | ✅ |
| `RUTINAS_CREAR` | ✅ | ✅ | ❌ |
| `RUTINAS_VER_PROPIA` | ❌ | ❌ | ✅ |
| `ENTRENADOR_ASIGNAR` | ✅ | ✅ | ❌ |
| `HORARIOS_GESTIONAR` | ✅ | ✅ | ❌ |
| `HORARIOS_VER` | ✅ | ✅ | ✅ |
| `PAGOS_REGISTRAR` | ✅ | ✅ | ❌ |
| `MEMBRESIAS_GESTIONAR` | ✅ | ❌ | ❌ |
| `ASISTENCIA_REGISTRAR` | ✅ | ✅ | ❌ |

Los permisos son **datos**, no código: el admin puede recomponer roles sin desplegar.

---

## 4. Arquitectura

```
┌─────────────────────────────┐
│           App               │
│  ┌───────────────────────┐  │
│  │ Front  → React / Node │  │
│  └───────────┬───────────┘  │
│  ┌───────────▼───────────┐  │
│  │ Back   → Spring Boot  │  │
│  └───────────┬───────────┘  │
│  ┌───────────▼───────────┐  │
│  │ Datos  → PostgreSQL   │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

| Capa | Tecnología | Responsabilidad |
|---|---|---|
| Front | React + Node (build/SSR) | Render de vistas, validación de formularios, consumo de API REST. **No** decide permisos, solo los refleja. |
| Back | Java + Spring Boot (Spring Security, Spring Data JPA) | Reglas de negocio, autenticación JWT, autorización por permiso, transaccionalidad. |
| Datos | PostgreSQL | Persistencia relacional. |

### 4.1 Comunicación Front ↔ Back

El front y el back son **dos aplicaciones desplegadas por separado** que se comunican exclusivamente mediante **endpoints HTTP REST**. No hay renderizado de vistas en servidor ni sesión con estado: el back expone datos en JSON y el front decide cómo pintarlos.

```
  Navegador
      │
      │  HTTPS / JSON
      ▼
┌──────────────┐   fetch / axios    ┌──────────────────┐   JDBC    ┌────────────┐
│  Front       │ ─────────────────► │  Back            │ ────────► │ PostgreSQL │
│  React       │ ◄───────────────── │  Spring Boot     │ ◄──────── │            │
│  (Node)      │   200 / 4xx / 5xx  │  API REST        │           │            │
└──────────────┘                    └──────────────────┘           └────────────┘
```

#### Contrato de comunicación

| Aspecto | Definición |
|---|---|
| **Protocolo** | HTTPS obligatorio en todos los ambientes salvo local. |
| **Formato** | `application/json; charset=utf-8` en petición y respuesta. Excepción: subida de imágenes con `multipart/form-data`. |
| **Base URL** | `/api/v1` — la versión va en la ruta, no en un header. |
| **Autenticación** | Header `Authorization: Bearer <jwt>` en toda petición salvo `/auth/login` y `/auth/recuperar`. |
| **Estado** | Sin estado (*stateless*). El back no guarda sesión; cada petición se autoriza por sí misma. |
| **CORS** | El back habilita CORS solo para el dominio del front (`allowed-origins` por ambiente). Métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`. |
| **Idempotencia** | `GET`, `PUT` y `DELETE` idempotentes. `POST /ventas` y `POST /compras` aceptan header `Idempotency-Key` para evitar cobros duplicados por doble clic. |
| **Trazabilidad** | El front envía `X-Request-Id` (UUID); el back lo devuelve y lo incluye en sus logs. |

#### Verbos y códigos de estado

| Acción | Verbo | Éxito | Errores frecuentes |
|---|---|---|---|
| Listar | `GET /recurso` | 200 | 401, 403 |
| Obtener uno | `GET /recurso/{id}` | 200 | 404 |
| Crear | `POST /recurso` | 201 + header `Location` | 400, 409 |
| Actualizar completo | `PUT /recurso/{id}` | 200 | 400, 404, 409 |
| Actualizar parcial | `PATCH /recurso/{id}` | 200 | 400, 404 |
| Baja lógica | `DELETE /recurso/{id}` | 204 | 404, 409 |

| Código | Cuándo |
|---|---|
| 400 | Fallo de validación de datos de entrada. |
| 401 | Token ausente, inválido o expirado → el front redirige al login. |
| 403 | Autenticado pero sin el permiso requerido → el front muestra "acceso denegado". |
| 404 | Recurso inexistente o fuera del alcance del usuario. |
| 409 | Conflicto de negocio: stock insuficiente, email duplicado, horario solapado. |
| 422 | Petición bien formada pero semánticamente inválida (fecha fin anterior a fecha inicio). |
| 500 | Error no controlado. Nunca expone stacktrace al front. |

#### Formato uniforme de error

Todo error, sin importar el endpoint, responde con la misma estructura:

```json
{
  "timestamp": "2026-08-08T14:32:10Z",
  "codigo": "STOCK_INSUFICIENTE",
  "mensaje": "No hay unidades suficientes del producto solicitado",
  "path": "/api/v1/ventas",
  "requestId": "8f3c1a...",
  "detalles": [
    { "campo": "items[0].cantidad", "error": "Disponible: 3, solicitado: 5" }
  ]
}
```

El front nunca interpreta el texto de `mensaje` para tomar decisiones: usa `codigo`.

#### Paginación, filtro y orden

Los listados devuelven un envoltorio consistente:

```
GET /api/v1/usuarios?page=0&size=20&sort=apellido,asc&q=perez&activo=true
```

```json
{
  "contenido": [ ... ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 137,
  "totalPaginas": 7
}
```

#### Responsabilidades de cada lado

| Front | Back |
|---|---|
| Guarda el token y lo adjunta en cada llamada. | Emite, firma y valida el token. |
| Muestra u oculta botones según `permisos`. | **Vuelve a validar el permiso en cada endpoint.** |
| Valida formatos para dar feedback rápido. | Valida todo de nuevo; es la única fuente de verdad. |
| Traduce `codigo` de error a mensaje al usuario. | Devuelve códigos estables y documentados. |
| Maneja 401 renovando o redirigiendo al login. | Expira tokens a los 60 minutos. |

### 4.2 Stack complementario

Componentes que no aparecen en el diagrama pero que se incorporan al proyecto:

| Componente | Herramienta | Para qué |
|---|---|---|
| **Migraciones de BD** | Flyway | Versionar el esquema en `src/main/resources/db/migration` (`V1__esquema_inicial.sql`, `V2__membresias.sql`...). Evita el `ddl-auto: update` de Hibernate, que en producción corrompe datos. |
| **Documentación de API** | springdoc-openapi (Swagger UI) | Contrato vivo en `/swagger-ui.html`. El equipo de front trabaja contra él sin esperar al back. |
| **Entorno local** | Docker Compose | Levanta PostgreSQL + back + front con un comando. Elimina el "en mi máquina sí funciona". |
| **Seguridad** | Spring Security + jjwt | Filtro JWT, `@PreAuthorize` por permiso, BCrypt. |
| **Validación** | Bean Validation (Jakarta) | Anotaciones en los DTO de entrada, con `@ControllerAdvice` que las traduce al formato de error uniforme. |
| **Mapeo DTO ↔ entidad** | MapStruct | Evita exponer entidades JPA directamente en la API (nunca se devuelve `password_hash`). |
| **Pruebas** | JUnit 5 + Testcontainers | Pruebas de integración contra un PostgreSQL real y efímero. |
| **Cliente HTTP en front** | Axios con interceptores | Un interceptor inyecta el token; otro captura 401 y redirige al login. |
| **Estado en front** | React Query (o similar) | Cachea respuestas GET y evita llamadas repetidas al back. |
| **Logs** | SLF4J + Logback en JSON | Logs estructurados con `requestId` para correlacionar front y back. |
| **CI** | GitHub Actions | Build, pruebas y análisis estático en cada push. |

---

## 5. Modelo de datos

### 5.1 Tablas del diagrama

| Tabla (diagrama) | Tabla propuesta | Campos clave |
|---|---|---|
| TablaUsuario | `usuario` | id, nombre, apellido, documento, email (único), telefono, password_hash, fecha_nacimiento, activo, fecha_registro |
| TablaEmpleado | `empleado` | id, usuario_id (FK), cargo, fecha_contratacion, salario, activo |
| tablaRoles | `rol` | id, nombre, descripcion |
| tablaPermisos | `permiso` | id, codigo, descripcion |
| TablaEjercicio | `ejercicio` | id, nombre, descripcion, grupo_muscular, maquina_id (FK, nullable) |
| TablaEjercicio_Usuario | `rutina_ejercicio` | id, rutina_id (FK), ejercicio_id (FK), series, repeticiones, peso_kg, descanso_seg, dia_semana, orden |
| tablaMaquinas | `maquina` | id, nombre, descripcion, cantidad, estado (OPERATIVA / MANTENIMIENTO / BAJA) |
| tablaProductos | `producto` | id, nombre, descripcion, precio, stock, categoria, activo |
| tablaVentas | `venta` | id, usuario_id (FK, comprador), empleado_id (FK, nullable), fecha, total, canal (MOSTRADOR / ONLINE), estado |
| tablaPagos | `pago` | id, usuario_id (FK), monto, fecha, metodo, concepto (MEMBRESIA / VENTA), referencia_id, estado |

### 5.2 Tablas de relación necesarias

| Tabla | Motivo |
|---|---|
| `usuario_rol` | Un usuario puede tener varios roles. |
| `rol_permiso` | Define qué permisos otorga cada rol. |

### 5.3 Tablas complementarias (incorporadas al modelo)

El diagrama tiene funcionalidades sin respaldo de datos. Estas tablas cierran esos huecos y **forman parte del modelo definitivo**, no son opcionales.

#### `rutina` — cabecera de la rutina
Cierra el hueco entre `CrearRutina` y `TablaEjercicio_Usuario`. Sin ella no se puede versionar ni desactivar una rutina completa.

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT FK → usuario | Socio dueño de la rutina |
| empleado_id | BIGINT FK → empleado | Entrenador que la creó |
| nombre | VARCHAR(100) | "Hipertrofia — fase 1" |
| objetivo | VARCHAR(50) | FUERZA / HIPERTROFIA / RESISTENCIA / PERDIDA_PESO |
| fecha_inicio | DATE | |
| fecha_fin | DATE NULL | |
| activa | BOOLEAN | Índice parcial único: solo una activa por `usuario_id` |
| fecha_creacion | TIMESTAMP | |

#### `horario` — clases y disponibilidad
Da soporte a `RegistroHorarios` y `VistaHorarios`, que en el diagrama no persistían en ningún lado.

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| tipo | VARCHAR(20) | CLASE / DISPONIBILIDAD_ENTRENADOR |
| nombre | VARCHAR(100) | "Spinning", "Funcional" |
| empleado_id | BIGINT FK → empleado | Responsable |
| dia_semana | SMALLINT | 1 = lunes … 7 = domingo |
| hora_inicio / hora_fin | TIME | Restricción: `hora_fin > hora_inicio` |
| cupo_maximo | INT NULL | Solo para tipo CLASE |
| activo | BOOLEAN | |

Restricción de negocio: no se permiten dos horarios solapados para el mismo `empleado_id` y `dia_semana`.

#### `inscripcion_horario` — socios inscritos a una clase
Necesaria para poder validar el `cupo_maximo` (RN-08).

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| horario_id | BIGINT FK → horario | |
| usuario_id | BIGINT FK → usuario | Único junto con `horario_id` |
| fecha_inscripcion | TIMESTAMP | |
| estado | VARCHAR(20) | INSCRITO / CANCELADO |

#### `asignacion_entrenador` — historial socio ↔ entrenador
Da destino a `AsignarEntrenador`.

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT FK → usuario | Socio |
| empleado_id | BIGINT FK → empleado | Entrenador |
| fecha_inicio | DATE | |
| fecha_fin | DATE NULL | Se llena al reasignar |
| activa | BOOLEAN | Índice parcial único por `usuario_id` |

#### `detalle_venta` — líneas de la venta
Sin ella no se puede vender más de un producto por transacción ni conservar el precio histórico (RN-06).

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| venta_id | BIGINT FK → venta | ON DELETE CASCADE |
| producto_id | BIGINT FK → producto | |
| cantidad | INT | > 0 |
| precio_unitario | NUMERIC(10,2) | Congelado al momento de la venta |
| subtotal | NUMERIC(10,2) | cantidad × precio_unitario |

#### `membresia` — planes ofrecidos
El modelo de ingresos del gimnasio, ausente por completo en el diagrama.

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| nombre | VARCHAR(80) | "Mensual", "Trimestral", "Anual" |
| precio | NUMERIC(10,2) | |
| duracion_dias | INT | 30, 90, 365 |
| beneficios | TEXT | Descripción libre |
| activa | BOOLEAN | |

#### `suscripcion` — membresía contratada por un socio

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT FK → usuario | |
| membresia_id | BIGINT FK → membresia | |
| fecha_inicio | DATE | |
| fecha_fin | DATE | Calculada: inicio + duracion_dias |
| estado | VARCHAR(20) | ACTIVA / VENCIDA / CANCELADA |
| pago_id | BIGINT FK → pago NULL | Pago que la originó |

#### `asistencia` — check-in del socio

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT FK → usuario | |
| fecha_hora_entrada | TIMESTAMP | |
| fecha_hora_salida | TIMESTAMP NULL | |
| registrado_por | BIGINT FK → empleado NULL | Null si fue autoservicio |

#### `auditoria` — trazabilidad de operaciones sensibles
Respalda el requisito no funcional de auditoría.

| Campo | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT FK → usuario | Quién ejecutó |
| accion | VARCHAR(60) | ASIGNAR_ROL, REGISTRAR_VENTA, CAMBIAR_PERMISO |
| entidad / entidad_id | VARCHAR(50) / BIGINT | Sobre qué |
| datos_antes / datos_despues | JSONB | Solo campos relevantes |
| fecha | TIMESTAMP | |
| ip | VARCHAR(45) | |

### 5.4 Diagrama relacional (resumen)

```
usuario ──< usuario_rol >── rol ──< rol_permiso >── permiso
   │
   ├──1:1─ empleado ──< horario ──< inscripcion_horario >── usuario (socio)
   │           │
   │           └──< asignacion_entrenador >── usuario (socio)
   │
   ├──< rutina ──< rutina_ejercicio >── ejercicio >── maquina
   │
   ├──< suscripcion >── membresia
   │
   ├──< venta ──< detalle_venta >── producto
   │
   ├──< pago
   ├──< asistencia
   └──< auditoria
```

---

## 6. Flujo de autenticación y resolución de vista

Este es el flujo central del diagrama.

1. El **Actor** envía credenciales a `POST /api/auth/login`.
2. El back valida contra `usuario.password_hash` (BCrypt).
3. Si es válido: **obtiene los roles** del usuario (`usuario_rol`).
4. **Obtiene los permisos** derivados de esos roles (`rol_permiso`).
5. **Dependiendo del rol y los permisos, se determina la vista**.
6. **Se devuelve la vista** al front junto con el token JWT.

### 6.1 Respuesta del login

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiraEn": 3600,
  "usuario": { "id": 42, "nombre": "Ana", "email": "ana@mail.com" },
  "roles": ["EMPLEADO"],
  "permisos": ["USUARIOS_REGISTRAR", "RUTINAS_CREAR", "HORARIOS_GESTIONAR"],
  "vista": "VISTA_EMPLEADO"
}
```

### 6.2 Reglas de resolución de vista

| Condición | Vista |
|---|---|
| Rol contiene `ADMIN` | `VISTA_ADMIN` |
| Rol contiene `EMPLEADO` (y no ADMIN) | `VISTA_EMPLEADO` |
| Rol contiene `USUARIO` únicamente | `VISTA_USER` |
| Múltiples roles | Se devuelve la de mayor privilegio + selector para cambiar de vista |
| Sin roles asignados | Error 403 + mensaje "cuenta sin rol asignado, contacte al administrador" |

### 6.3 Regla de seguridad no negociable

El front usa la lista de permisos **solo para mostrar u ocultar elementos de UI**. Cada endpoint del back valida el permiso de forma independiente (`@PreAuthorize("hasAuthority('RUTINAS_CREAR')")`). Ocultar un botón no es una medida de seguridad.

---

## 7. Requisitos funcionales

### 7.1 Módulo Autenticación

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-AUT-01 | Login con email y contraseña | Credenciales válidas → token + vista. Inválidas → 401 sin revelar si el email existe. |
| RF-AUT-02 | Bloqueo por intentos fallidos | 5 fallos en 15 min → cuenta bloqueada 15 min. |
| RF-AUT-03 | Cierre de sesión | Token invalidado en el cliente; expiración de 1 h. |
| RF-AUT-04 | Recuperación de contraseña | Enlace con token de un solo uso, válido 30 min. *(no está en el diagrama, se agrega)* |
| RF-AUT-05 | Cambio de contraseña | Requiere contraseña actual. Mínimo 8 caracteres, mayúscula, minúscula y número. |

### 7.2 Módulo Administración (VistaAdmin)

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-ADM-01 | Gestionar roles (`Asignarroles`) | CRUD de roles. No se puede eliminar un rol con usuarios asignados. |
| RF-ADM-02 | Asignar permisos a roles (`Asignarpermisos`) | Pantalla de checkboxes por rol. Cambios efectivos en el siguiente login del usuario. |
| RF-ADM-03 | Asignar roles a usuarios | Un usuario puede tener 1..N roles. |
| RF-ADM-04 | Registrar empleados (`registroEmpleados`) | Crea `usuario` + `empleado`. Documento y email únicos. |
| RF-ADM-05 | Registrar usuarios (`RegistroUser`) | Igual que RF-EMP-01. |
| RF-ADM-06 | Registrar productos (`RegistrarProducto`) | Nombre, precio > 0, stock ≥ 0, categoría. |
| RF-ADM-07 | Desactivar empleado | Baja lógica. Sus asignaciones activas se marcan para reasignar. |

### 7.3 Módulo Empleado (VistaEmpleado)

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-EMP-01 | Registrar socio (`RegistroUser`) | Datos personales + membresía inicial opcional. Rol `USUARIO` por defecto. |
| RF-EMP-02 | Crear rutina (`CrearRutina`) | Cabecera + N ejercicios con series, repeticiones, peso y día. Al crear una nueva, la anterior del socio pasa a `activa = false`. |
| RF-EMP-03 | Asignar entrenador (`AsignarEntrenador`) | Un socio tiene máximo un entrenador activo. Reasignar cierra la asignación previa. |
| RF-EMP-04 | Gestionar horarios (`RegistroHorarios` / `VistaHorarios`) | CRUD de horarios. Valida solapamiento para el mismo entrenador. |
| RF-EMP-05 | Registrar producto (`RegistrarProducto`) | Igual que RF-ADM-06. |
| RF-EMP-06 | Vender producto (`VenderProducto`) | Descuenta stock atómicamente, genera `venta` + `detalle_venta` + `pago`. Rechaza si stock insuficiente. |
| RF-EMP-07 | Consultar socios | Búsqueda por nombre, documento o estado de membresía. |

### 7.4 Módulo Socio (VistaUser)

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-USR-01 | Consultar rutina (`ConsultaRutina`) | Muestra solo la rutina activa propia, agrupada por día. |
| RF-USR-02 | Consultar entrenador (`ConsultaEntrenador`) | Nombre, especialidad y horarios de su entrenador asignado. |
| RF-USR-03 | Ver horarios (`VistaHorarios`) | Horarios de clases y disponibilidad, solo lectura. |
| RF-USR-04 | Ver productos (`VistaProducto`) | Catálogo con precio y disponibilidad. |
| RF-USR-05 | Comprar producto (`CompraProducto`) | Carrito → confirmación → `venta` canal ONLINE + `pago` pendiente. |
| RF-USR-06 | Editar perfil (`RegistroUser` en rama usuario) | Solo datos propios: teléfono, email, contraseña. **No** puede cambiar su rol ni su documento. |
| RF-USR-07 | Ver historial de pagos | Lista de pagos propios con fecha, concepto y estado. |
| RF-USR-08 | Inscribirse a una clase | Solo si hay cupo disponible y la suscripción está activa. |
| RF-USR-09 | Ver estado de membresía | Plan, fecha de vencimiento y días restantes. |

### 7.5 Módulo Membresías (agregado)

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-MEM-01 | Gestionar planes de membresía | CRUD de `membresia`. Precio > 0, duración > 0 días. Permiso `MEMBRESIAS_GESTIONAR`. |
| RF-MEM-02 | Contratar / renovar suscripción | Genera `suscripcion` + `pago` con concepto MEMBRESIA. `fecha_fin = fecha_inicio + duracion_dias`. |
| RF-MEM-03 | Vencimiento automático | Un proceso diario marca como `VENCIDA` toda suscripción cuya `fecha_fin` ya pasó. |
| RF-MEM-04 | Alerta de vencimiento próximo | Aviso en la vista del socio cuando faltan ≤ 7 días. |
| RF-MEM-05 | Renovación anticipada | Si renueva antes de vencer, la nueva suscripción arranca el día siguiente al vencimiento vigente. |

### 7.6 Módulo Asistencia (agregado)

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-ASI-01 | Registrar entrada | Busca al socio por documento, valida suscripción activa y crea `asistencia`. Si está vencida, permite el ingreso pero deja constancia. |
| RF-ASI-02 | Registrar salida | Cierra la asistencia abierta más reciente del socio. |
| RF-ASI-03 | Ver historial propio | El socio consulta sus visitas de los últimos 90 días. |

---

## 8. Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | Un socio con suscripción vencida puede iniciar sesión y ver su información, pero se le muestra un aviso de renovación. |
| RN-02 | El stock nunca puede quedar negativo. La venta se ejecuta en transacción con bloqueo de fila sobre el producto. |
| RN-03 | Un usuario solo tiene una rutina activa a la vez. |
| RN-04 | Un usuario solo tiene un entrenador activo a la vez. |
| RN-05 | Las eliminaciones son **lógicas** (`activo = false`), nunca físicas. Preserva la integridad del histórico de ventas y rutinas. |
| RN-06 | Los precios se guardan en `detalle_venta` al momento de la venta; cambiar el precio del producto no altera ventas pasadas. |
| RN-07 | Un empleado no puede asignarse a sí mismo permisos adicionales. |
| RN-08 | Un horario de clase no puede exceder su `cupo_maximo` de inscritos. |

---

## 9. API REST (propuesta)

Base: `/api/v1`. Todas las rutas salvo `/auth/**` requieren `Authorization: Bearer <token>`.

### Autenticación
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/auth/login` | público |
| POST | `/auth/recuperar` | público |
| POST | `/auth/cambiar-password` | autenticado |
| GET | `/auth/me` | autenticado |

### Administración
| Método | Ruta | Permiso |
|---|---|---|
| GET/POST/PUT | `/roles` | `ROLES_GESTIONAR` |
| PUT | `/roles/{id}/permisos` | `PERMISOS_ASIGNAR` |
| GET | `/permisos` | `ROLES_GESTIONAR` |
| PUT | `/usuarios/{id}/roles` | `ROLES_GESTIONAR` |
| GET/POST/PUT | `/empleados` | `EMPLEADOS_GESTIONAR` |

### Usuarios
| Método | Ruta | Permiso |
|---|---|---|
| GET | `/usuarios` | `USUARIOS_VER_TODOS` |
| POST | `/usuarios` | `USUARIOS_REGISTRAR` |
| PUT | `/usuarios/{id}` | `USUARIOS_REGISTRAR` |
| PUT | `/usuarios/me` | `PERFIL_PROPIO_EDITAR` |

### Rutinas y entrenamiento
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/rutinas` | `RUTINAS_CREAR` |
| GET | `/rutinas/usuario/{id}` | `RUTINAS_CREAR` |
| GET | `/rutinas/me` | `RUTINAS_VER_PROPIA` |
| GET | `/ejercicios` | autenticado |
| GET | `/maquinas` | autenticado |
| POST | `/asignaciones-entrenador` | `ENTRENADOR_ASIGNAR` |
| GET | `/asignaciones-entrenador/me` | autenticado |

### Horarios y clases
| Método | Ruta | Permiso |
|---|---|---|
| GET | `/horarios` | `HORARIOS_VER` |
| POST/PUT/DELETE | `/horarios/{id}` | `HORARIOS_GESTIONAR` |
| POST | `/horarios/{id}/inscripciones` | `HORARIOS_VER` (se inscribe a sí mismo) |
| DELETE | `/horarios/{id}/inscripciones/me` | autenticado |
| GET | `/horarios/{id}/inscripciones` | `HORARIOS_GESTIONAR` |

### Comercio
| Método | Ruta | Permiso |
|---|---|---|
| GET | `/productos` | `PRODUCTOS_VER` |
| POST/PUT | `/productos` | `PRODUCTOS_GESTIONAR` |
| POST | `/ventas` | `PRODUCTOS_VENDER` |
| POST | `/compras` | `PRODUCTOS_COMPRAR` |
| GET | `/ventas` | `PRODUCTOS_VENDER` |
| GET | `/pagos/me` | autenticado |
| POST | `/pagos` | `PAGOS_REGISTRAR` |

### Membresías
| Método | Ruta | Permiso |
|---|---|---|
| GET | `/membresias` | autenticado |
| POST/PUT | `/membresias` | `MEMBRESIAS_GESTIONAR` |
| POST | `/suscripciones` | `PAGOS_REGISTRAR` |
| GET | `/suscripciones/me` | autenticado |
| GET | `/suscripciones?estado=VENCIDA` | `USUARIOS_VER_TODOS` |

### Asistencia
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/asistencias/entrada` | `ASISTENCIA_REGISTRAR` |
| POST | `/asistencias/salida` | `ASISTENCIA_REGISTRAR` |
| GET | `/asistencias/me` | autenticado |

> Las convenciones de formato, paginación, errores y códigos de estado están definidas en §4.1 y aplican a **todos** los endpoints de esta tabla.

---

## 10. Requisitos no funcionales

| Categoría | Requisito |
|---|---|
| **Seguridad** | Contraseñas con BCrypt (cost ≥ 10). JWT firmado HS256, expiración 1 h. HTTPS obligatorio. Validación de entrada en back. Protección contra SQL injection vía JPA parametrizado. |
| **Rendimiento** | Respuesta < 500 ms en el percentil 95 para consultas simples. Soporte para 200 usuarios concurrentes. |
| **Disponibilidad** | 99% mensual en horario de operación del gimnasio. |
| **Usabilidad** | Interfaz responsive (móvil, tablet, escritorio). Flujos de máximo 3 clics desde la vista principal. |
| **Auditoría** | Registro de quién y cuándo en operaciones sensibles: cambios de rol, permisos, ventas y pagos. |
| **Respaldo** | Backup diario automático de PostgreSQL con retención de 30 días. |
| **Trazabilidad** | Logs estructurados con correlación por request-id. |
| **Compatibilidad** | Navegadores: últimas 2 versiones de Chrome, Firefox, Safari y Edge. |

---

## 11. Observaciones sobre el diagrama

Puntos que conviene resolver antes de construir:

1. **`RegistroUser` aparece en las tres vistas** con significados distintos. En Admin y Empleado es *crear un socio*; en la vista de usuario debería ser *editar mi perfil*. Se recomienda renombrar a `RegistroUser` (admin/empleado) y `MiPerfil` (socio) para evitar que se implemente el mismo endpoint sin restricción de alcance — sería una escalada de privilegios.

2. **`RegistrarProducto` está en Admin y en Empleado.** Definir si un empleado realmente puede crear productos o solo venderlos. Recomendación: solo Admin crea, el empleado vende.

3. ~~**`VistaHorarios` no tiene tabla asociada**~~ → **Resuelto**: tablas `horario` e `inscripcion_horario` (§5.3).

4. ~~**`CrearRutina` conecta directamente con `TablaEjercicio_Usuario`**~~ → **Resuelto**: se agrega la cabecera `rutina` (§5.3).

5. **`tablaPagos` es ambigua.** Aparece colgando tanto de compras de producto como del flujo de usuario. Se resuelve con el campo `concepto` (MEMBRESIA / VENTA) + `referencia_id`, pero conviene confirmarlo con el negocio.

6. ~~**No existe el concepto de membresía/suscripción**~~ → **Resuelto**: tablas `membresia` y `suscripcion`, módulo RF-MEM y fase 4 del plan.

7. **`tablaMaquinas` está desconectada de `TablaEjercicio`** en algunas ramas. Se propone `ejercicio.maquina_id` opcional (hay ejercicios de peso libre sin máquina).

8. **`TablaEmpleado` y `TablaUsuario` separadas** duplican identidad y credenciales. Ver §3.

---

## 12. Plan de implementación sugerido

| Fase | Contenido | Entregable |
|---|---|---|
| **Fase 1 — Base** | Modelo de datos, migraciones, autenticación JWT, RBAC, CRUD de usuarios/roles/permisos, VistaAdmin. | Login funcional con resolución de vista. |
| **Fase 2 — Operación** | Empleados, ejercicios, máquinas, rutinas, asignación de entrenador, horarios. VistaEmpleado y consultas de VistaUser. | Núcleo del gimnasio operativo. |
| **Fase 3 — Comercio** | Productos, inventario, ventas de mostrador, compra en línea, pagos. | Módulo comercial completo. |
| **Fase 4 — Membresías** | Membresías, suscripciones, vencimientos, avisos de renovación, asistencia. | Modelo de ingresos cerrado. |
| **Fase 5 — Refinamiento** | Auditoría, reportes básicos, notificaciones, mejoras de UX. | Producto listo para producción. |

---

## 13. Glosario

| Término | Definición |
|---|---|
| **Socio / Usuario** | Cliente del gimnasio con membresía. |
| **Rol** | Agrupación nombrada de permisos (ADMIN, EMPLEADO, USUARIO). |
| **Permiso** | Autorización atómica para ejecutar una acción concreta. |
| **Rutina** | Plan de entrenamiento con vigencia, compuesto por ejercicios distribuidos por día. |
| **Vista** | Conjunto de pantallas y opciones que el back determina según rol y permisos. |
| **Baja lógica** | Marcar un registro como inactivo en lugar de borrarlo. |
