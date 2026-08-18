# Gym-Bro — Especificación Técnica

**Versión:** 1.0
**Fecha:** 8 de agosto de 2026
**Documento padre:** Especificación Funcional v1.1
**Backlog asociado:** `backlog-tareas-gym-bro.md`

> Este documento define **cómo** se construye lo que el spec funcional define **qué** hace. Toda referencia tipo `RF-XXX-nn` o `RN-nn` apunta al spec funcional.

---

## 1. Stack y versiones

| Capa | Tecnología | Versión objetivo |
|---|---|---|
| Lenguaje back | Java | 21 (LTS) |
| Framework back | Spring Boot | 3.3.x |
| Seguridad | Spring Security | 6.3.x |
| Persistencia | Spring Data JPA + Hibernate | 6.5.x |
| Migraciones | Flyway | 10.x |
| Base de datos | PostgreSQL | 16 |
| Docs API | springdoc-openapi | 2.5.x |
| Mapeo | MapStruct | 1.5.5 |
| JWT | jjwt | 0.12.x |
| Build back | Maven | 3.9.x |
| Lenguaje front | TypeScript | 5.4+ |
| Framework front | React | 18.3 |
| Build front | Vite | 5.x |
| Router | React Router | 6.x |
| Datos remotos | TanStack Query (React Query) | 5.x |
| HTTP | Axios | 1.7.x |
| Formularios | React Hook Form + Zod | 7.x / 3.x |
| Estilos | Tailwind CSS | 3.4 |
| Pruebas back | JUnit 5 + Testcontainers | 5.10 / 1.19 |
| Pruebas front | Vitest + Testing Library | 1.x |
| Contenedores | Docker + Docker Compose | 24+ |
| CI | GitHub Actions | — |

**Regla:** las versiones se fijan en `pom.xml` y `package.json`. Nada de rangos abiertos (`^`, `~`) en dependencias críticas de seguridad.

---

## 2. Estructura de repositorios

Monorepo con dos aplicaciones independientes:

```
gym-bro/
├── backend/
│   ├── src/main/java/com/gymbro/...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/       ← Flyway
│   ├── src/test/java/...
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── public/
│   ├── vite.config.ts
│   └── package.json
├── docker/
│   ├── docker-compose.yml
│   ├── backend.Dockerfile
│   └── frontend.Dockerfile
├── docs/
│   ├── spec-funcional-gym-bro.md
│   └── spec-tecnico-gym-bro.md
├── .github/workflows/ci.yml
└── README.md
```

---

## 3. Arquitectura del backend

### 3.1 Estructura de paquetes (por funcionalidad, no por capa técnica)

```
com.gymbro
├── GymBroApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── CorsConfig.java
│   └── JacksonConfig.java
├── common/
│   ├── exception/          → RecursoNoEncontradoException, ReglaNegocioException, ...
│   ├── error/              → GlobalExceptionHandler, ErrorResponse, CodigoError (enum)
│   ├── dto/                → PaginaResponse<T>
│   ├── audit/              → @Auditable, AuditAspect
│   └── util/
├── auth/
│   ├── AuthController · AuthService · JwtService
│   ├── JwtAuthenticationFilter
│   ├── dto/                → LoginRequest, LoginResponse, CambiarPasswordRequest
│   └── ResolvedorVista.java
├── usuario/
│   ├── Usuario.java (entity) · UsuarioRepository · UsuarioService · UsuarioController
│   ├── UsuarioMapper.java
│   └── dto/
├── empleado/
├── rol/                    → Rol, Permiso, RolService, PermisoService
├── ejercicio/              → Ejercicio, Maquina
├── rutina/                 → Rutina, RutinaEjercicio
├── entrenador/             → AsignacionEntrenador
├── horario/                → Horario, InscripcionHorario
├── producto/               → Producto (incluye control de stock)
├── venta/                  → Venta, DetalleVenta
├── pago/                   → Pago
├── membresia/              → Membresia, Suscripcion, VencimientoScheduler
└── asistencia/             → Asistencia
```

Cada módulo expone su `Service` como única puerta de entrada. **Un servicio no accede al repositorio de otro módulo**: pide los datos al servicio dueño. Esto evita el grafo de dependencias enredado que aparece en el diagrama original.

### 3.2 Capas por petición

```
Controller  → recibe DTO, valida (@Valid), no tiene lógica
    ↓
Service     → reglas de negocio, @Transactional, orquestación
    ↓
Repository  → Spring Data JPA, queries
    ↓
Entity      → mapeo JPA, sin lógica de negocio
```

**Regla dura:** una entidad JPA **nunca** sale del controller. Siempre se mapea a DTO con MapStruct. `Usuario` tiene `passwordHash`; si se serializa por accidente, se filtra.

> Las convenciones detalladas de la capa de entidades — `@MappedSuperclass`, enumeraciones, embebidos, uso correcto de Lombok y la relación entre el DDL generado por Hibernate y las migraciones Flyway — están en el documento aparte `entidades-jpa-gym-bro.md`.

### 3.3 Convenciones de código

| Elemento | Convención | Ejemplo |
|---|---|---|
| Tabla y columna | snake_case, singular | `asignacion_entrenador`, `fecha_inicio` |
| Clase Java | PascalCase | `AsignacionEntrenador` |
| DTO de entrada | `<Accion><Entidad>Request` | `CrearRutinaRequest` |
| DTO de salida | `<Entidad>Response` | `RutinaResponse` |
| Endpoint | plural, kebab-case | `/api/v1/asignaciones-entrenador` |
| Excepción | `<Motivo>Exception` | `StockInsuficienteException` |
| Migración | `V<n>__<descripcion>.sql` | `V4__membresias.sql` |
| Rama Git | `tipo/TASK-ID-descripcion` | `feat/BE-12-crear-rutina` |
| Commit | Conventional Commits | `feat(rutina): endpoint de creación` |

---

## 4. Modelo físico de datos

### 4.1 Plan de migraciones Flyway

Una **migración** es un archivo `.sql` numerado que describe un cambio en la base de datos. Flyway los ejecuta en orden y lleva registro de cuáles ya corrió. Eso significa que el esquema vive en el repositorio, no solo dentro de una base de datos: cualquiera puede clonar el proyecto y obtener exactamente la misma estructura.

| Archivo | Contenido |
|---|---|
| `V1__esquema_base.sql` | usuario, rol, permiso, usuario_rol, rol_permiso, empleado |
| `V2__catalogo_entrenamiento.sql` | maquina, ejercicio, rutina, rutina_ejercicio, asignacion_entrenador |
| `V3__horarios.sql` | horario, inscripcion_horario |
| `V4__comercio.sql` | producto, venta, detalle_venta, pago |
| `V5__membresias.sql` | membresia, suscripcion |
| `V6__asistencia_auditoria.sql` | asistencia, auditoria |
| `V7__datos_iniciales.sql` | roles, permisos, rol_permiso y usuario admin semilla |

**Reglas de uso:**

1. Un archivo de migración ya mergeado a `main` **no se edita jamás**. Si hay que corregir algo, se crea una migración nueva. Flyway guarda una firma de cada archivo ejecutado; si cambia, falla el arranque.
2. Las migraciones se numeran de forma secuencial. Si dos ramas crean un `V5__`, hay conflicto: se resuelve renumerando la que se mergee después.
3. `V7__datos_iniciales.sql` es obligatorio. Sin roles, permisos y usuario admin, una base recién creada no permite iniciar sesión a nadie.

**Configuración de Hibernate:**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # nunca 'update', nunca 'create'
```

`validate` compara las entidades Java contra las tablas reales al arrancar y falla de inmediato si no coinciden. Es una red de seguridad: avisa que alguien cambió una entidad y olvidó la migración.

**Está prohibido `ddl-auto: update`** en cualquier ambiente. Parece cómodo porque crea las tablas solo, pero: agrega columnas nuevas sin eliminar las viejas, no crea índices parciales ni restricciones `EXCLUDE` ni `CHECK` (que en este proyecto son los que garantizan reglas de negocio como "una sola rutina activa" o "stock nunca negativo"), y deja el esquema en un estado que nadie diseñó ni puede reproducir. `validate` avisa cuando algo no cuadra; `update` improvisa en silencio.

### 4.2 DDL de referencia (extracto)

```sql
-- V1__esquema_base.sql
CREATE TABLE usuario (
    id                BIGSERIAL PRIMARY KEY,
    nombre            VARCHAR(60)  NOT NULL,
    apellido          VARCHAR(60)  NOT NULL,
    documento         VARCHAR(20)  NOT NULL UNIQUE,
    email             VARCHAR(120) NOT NULL UNIQUE,
    telefono          VARCHAR(20),
    password_hash     VARCHAR(72)  NOT NULL,
    fecha_nacimiento  DATE,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMPTZ,
    fecha_registro    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_usuario_email ON usuario(email) WHERE activo;

CREATE TABLE rol (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE permiso (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id     BIGINT NOT NULL REFERENCES rol(id),
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE rol_permiso (
    rol_id     BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permiso(id),
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE empleado (
    id                 BIGSERIAL PRIMARY KEY,
    usuario_id         BIGINT NOT NULL UNIQUE REFERENCES usuario(id),
    cargo              VARCHAR(50) NOT NULL,
    especialidad       VARCHAR(80),
    fecha_contratacion DATE NOT NULL,
    activo             BOOLEAN NOT NULL DEFAULT TRUE
);
```

```sql
-- V2 (extracto): una sola rutina activa por socio — RN-03
CREATE UNIQUE INDEX uq_rutina_activa_por_usuario
    ON rutina(usuario_id) WHERE activa;

-- V2 (extracto): un solo entrenador activo por socio — RN-04
CREATE UNIQUE INDEX uq_entrenador_activo_por_usuario
    ON asignacion_entrenador(usuario_id) WHERE activa;
```

```sql
-- V3 (extracto): no solapar horarios del mismo empleado
ALTER TABLE horario ADD CONSTRAINT ck_horario_rango
    CHECK (hora_fin > hora_inicio);

CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE horario ADD CONSTRAINT ex_horario_sin_solape
    EXCLUDE USING gist (
        empleado_id WITH =,
        dia_semana  WITH =,
        timerange(hora_inicio, hora_fin) WITH &&
    ) WHERE (activo);
```

```sql
-- V4 (extracto): el stock nunca negativo — RN-02
ALTER TABLE producto ADD CONSTRAINT ck_stock_no_negativo CHECK (stock >= 0);
ALTER TABLE detalle_venta ADD CONSTRAINT ck_cantidad_positiva CHECK (cantidad > 0);
```

> Las restricciones de negocio se implementan **en dos lugares**: en el servicio (para dar un error 409 legible) y en la base (como red de seguridad ante condiciones de carrera). No es duplicación innecesaria: es defensa en profundidad.

---

## 5. Seguridad

### 5.1 Cadena de filtros

```
Request
  → CorsFilter
  → JwtAuthenticationFilter   (extrae y valida el token, puebla SecurityContext)
  → AuthorizationFilter       (@PreAuthorize por permiso)
  → Controller
```

### 5.2 Configuración

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)      // API stateless, sin cookies
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login",
                                 "/api/v1/auth/recuperar",
                                 "/api/v1/auth/restablecer").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new JwtEntryPoint())   // 401 en formato uniforme
                .accessDeniedHandler(new JwtAccessDeniedHandler())) // 403 en formato uniforme
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### 5.3 Contenido del JWT

```json
{
  "sub": "42",
  "email": "ana@mail.com",
  "roles": ["EMPLEADO"],
  "permisos": ["USUARIOS_REGISTRAR", "RUTINAS_CREAR"],
  "iat": 1754661130,
  "exp": 1754664730
}
```

- Algoritmo HS256, secreto de 256 bits desde variable de entorno `JWT_SECRET`. **Nunca** en el repositorio.
- Expiración: 60 min. Refresh token opcional (v2), guardado en tabla `refresh_token` con rotación.
- Los permisos viajan en el token para evitar una consulta a BD por request. **Consecuencia documentada:** un cambio de permisos surte efecto en el siguiente login (RF-ADM-02). Si el negocio exige efecto inmediato, se agrega una tabla `token_revocado` y se invalida por `usuario_id`.

### 5.4 Autorización por endpoint

```java
@PostMapping("/rutinas")
@PreAuthorize("hasAuthority('RUTINAS_CREAR')")
public ResponseEntity<RutinaResponse> crear(@Valid @RequestBody CrearRutinaRequest req) { ... }
```

Para recursos propios se combinan permiso y propiedad:

```java
@GetMapping("/rutinas/me")
@PreAuthorize("hasAuthority('RUTINAS_VER_PROPIA')")
public RutinaResponse miRutina(@AuthenticationPrincipal UsuarioAutenticado auth) {
    return rutinaService.obtenerActivaDe(auth.id());   // el id viene del token, jamás del body
}
```

> **Vulnerabilidad a evitar (IDOR):** ningún endpoint acepta `usuarioId` en el body para operar sobre "lo mío". El id siempre sale del token. Este es exactamente el riesgo del `RegistroUser` compartido entre las tres vistas que señaló el spec funcional (§11.1).

### 5.5 Resolución de vista

```java
public enum Vista { VISTA_ADMIN, VISTA_EMPLEADO, VISTA_USER }

@Component
public class ResolvedorVista {
    public Vista resolver(Set<String> roles) {
        if (roles.contains("ADMIN"))    return Vista.VISTA_ADMIN;
        if (roles.contains("EMPLEADO")) return Vista.VISTA_EMPLEADO;
        if (roles.contains("USUARIO"))  return Vista.VISTA_USER;
        throw new SinRolAsignadoException();
    }
}
```

---

## 6. Capa API

### 6.1 Manejo global de errores

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex,
                                                    HttpServletRequest req) {
        var detalles = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> new DetalleError(f.getField(), f.getDefaultMessage()))
            .toList();
        return build(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION,
                     "Datos de entrada inválidos", req, detalles);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(ReglaNegocioException ex,
                                                 HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(...) { /* 404 */ }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado", ex);          // el stacktrace va al log
        return build(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO,
                     "Ocurrió un error inesperado", req, List.of());  // no al cliente
    }
}
```

### 6.2 Catálogo de códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | Falla Bean Validation |
| `CREDENCIALES_INVALIDAS` | 401 | Login incorrecto |
| `TOKEN_EXPIRADO` | 401 | JWT vencido |
| `CUENTA_BLOQUEADA` | 401 | 5 intentos fallidos (RF-AUT-02) |
| `SIN_PERMISO` | 403 | Falta la autoridad requerida |
| `SIN_ROL_ASIGNADO` | 403 | Usuario sin roles (§6.2 funcional) |
| `RECURSO_NO_ENCONTRADO` | 404 | Id inexistente |
| `EMAIL_DUPLICADO` | 409 | Email ya registrado |
| `DOCUMENTO_DUPLICADO` | 409 | Documento ya registrado |
| `STOCK_INSUFICIENTE` | 409 | RN-02 |
| `HORARIO_SOLAPADO` | 409 | RF-EMP-04 |
| `CUPO_AGOTADO` | 409 | RN-08 |
| `SUSCRIPCION_VENCIDA` | 409 | Inscripción a clase con membresía vencida |
| `ROL_EN_USO` | 409 | Eliminar rol con usuarios (RF-ADM-01) |
| `ERROR_INTERNO` | 500 | No controlado |

Este enum es **contrato compartido**. El front tiene su copia en `src/api/codigosError.ts` y la usa para decidir mensajes. Si el back agrega un código, es tarea de la misma historia actualizar el front.

### 6.3 Ejemplo de contrato completo — crear rutina

`POST /api/v1/rutinas` · permiso `RUTINAS_CREAR`

```json
// Request
{
  "usuarioId": 42,
  "nombre": "Hipertrofia — fase 1",
  "objetivo": "HIPERTROFIA",
  "fechaInicio": "2026-08-10",
  "fechaFin": "2026-11-10",
  "ejercicios": [
    { "ejercicioId": 7, "diaSemana": 1, "orden": 1,
      "series": 4, "repeticiones": 10, "pesoKg": 60.0, "descansoSeg": 90 }
  ]
}
```

```json
// 201 Created · Location: /api/v1/rutinas/88
{
  "id": 88,
  "usuario": { "id": 42, "nombreCompleto": "Ana Pérez" },
  "entrenador": { "id": 5, "nombreCompleto": "Luis Gómez" },
  "nombre": "Hipertrofia — fase 1",
  "objetivo": "HIPERTROFIA",
  "fechaInicio": "2026-08-10",
  "fechaFin": "2026-11-10",
  "activa": true,
  "dias": [
    { "diaSemana": 1, "ejercicios": [ { "orden": 1, "ejercicio": {...}, "series": 4, ... } ] }
  ]
}
```

Efecto colateral transaccional: la rutina activa anterior del socio pasa a `activa = false` (RF-EMP-02).

### 6.4 Transacciones críticas

**Venta de producto** (RF-EMP-06, RN-02, RN-06) — una sola transacción:

```java
@Transactional
public VentaResponse vender(CrearVentaRequest req, Long empleadoId) {
    var venta = new Venta(...);
    for (var item : req.items()) {
        // bloqueo pesimista: evita que dos cajas vendan la última unidad
        Producto p = productoRepo.findByIdForUpdate(item.productoId())
            .orElseThrow(() -> new RecursoNoEncontradoException("producto", item.productoId()));
        if (p.getStock() < item.cantidad())
            throw new StockInsuficienteException(p.getNombre(), p.getStock(), item.cantidad());
        p.descontarStock(item.cantidad());
        venta.agregarDetalle(p, item.cantidad(), p.getPrecio()); // precio congelado — RN-06
    }
    ventaRepo.save(venta);
    pagoService.registrar(venta);   // concepto = VENTA
    return mapper.toResponse(venta);
}
```

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Producto p where p.id = :id")
Optional<Producto> findByIdForUpdate(@Param("id") Long id);
```

---

## 7. Arquitectura del frontend

### 7.1 Estructura

```
frontend/src/
├── main.tsx
├── App.tsx
├── api/
│   ├── client.ts            → instancia de Axios + interceptores
│   ├── codigosError.ts      → espejo del enum del back
│   └── endpoints/           → un archivo por módulo (usuarios.ts, rutinas.ts, ...)
├── auth/
│   ├── AuthContext.tsx      → token, usuario, roles, permisos
│   ├── useAuth.ts
│   ├── RutaProtegida.tsx    → guard por permiso
│   └── Permiso.tsx          → <Permiso codigo="RUTINAS_CREAR">{...}</Permiso>
├── layouts/
│   ├── LayoutAdmin.tsx · LayoutEmpleado.tsx · LayoutUser.tsx
├── paginas/
│   ├── Login.tsx
│   ├── admin/    (Roles, Permisos, Empleados, Productos)
│   ├── empleado/ (Socios, Rutinas, Entrenadores, Horarios, Venta, Asistencia)
│   └── usuario/  (MiRutina, MiEntrenador, Horarios, Tienda, MisPagos, MiPerfil)
├── componentes/  → Tabla, Modal, Formulario, Paginador, Toast
├── hooks/        → useUsuarios, useRutinas, ... (envuelven React Query)
└── tipos/        → interfaces TS espejo de los DTO
```

### 7.2 Cliente HTTP

```ts
// api/client.ts
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,   // http://localhost:8080/api/v1
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('gymbro_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-Request-Id'] = crypto.randomUUID();
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const codigo = error.response?.data?.codigo;
    if (error.response?.status === 401 && codigo !== 'CREDENCIALES_INVALIDAS') {
      sessionStorage.clear();
      window.location.href = '/login';       // token expirado
    }
    return Promise.reject(error);
  }
);
```

> El token va en `sessionStorage`, no en `localStorage`: reduce la ventana de exposición ante XSS y muere al cerrar la pestaña. La alternativa robusta (cookie `HttpOnly` + `SameSite=Strict`) queda planteada para v2, ya que exige cambiar el manejo de CORS y CSRF.

### 7.3 Guard de rutas

```tsx
export function RutaProtegida({ permiso, children }: Props) {
  const { autenticado, permisos, cargando } = useAuth();
  if (cargando) return <Spinner />;
  if (!autenticado) return <Navigate to="/login" replace />;
  if (permiso && !permisos.includes(permiso)) return <Navigate to="/sin-acceso" replace />;
  return children;
}
```

Y el enrutado según la vista devuelta por el login:

```tsx
const RUTA_INICIAL: Record<Vista, string> = {
  VISTA_ADMIN:    '/admin',
  VISTA_EMPLEADO: '/empleado',
  VISTA_USER:     '/mi-cuenta',
};
```

### 7.4 Convención de claves de React Query

```ts
export const claves = {
  usuarios:  (filtros?: object) => ['usuarios', filtros ?? {}] as const,
  usuario:   (id: number)       => ['usuarios', id] as const,
  miRutina:  ()                 => ['rutinas', 'me'] as const,
  productos: (filtros?: object) => ['productos', filtros ?? {}] as const,
};
```

Tras una mutación se invalida la clave correspondiente. Nada de recargar la página entera.

---

## 8. Pruebas

| Nivel | Herramienta | Alcance mínimo |
|---|---|---|
| Unitario back | JUnit 5 + Mockito | Toda regla de negocio (RN-01 a RN-08). Cobertura ≥ 70% en `service/`. |
| Integración back | Testcontainers (PostgreSQL real) | Cada endpoint: happy path + 401 + 403 + un 409 de negocio. |
| Migraciones | Testcontainers | Flyway corre limpio desde cero en cada build. |
| Unitario front | Vitest + Testing Library | Componentes con lógica: guards, formularios, carrito. |
| E2E | Playwright (fase 5) | Los tres flujos de login y una venta completa. |

**Casos de prueba obligatorios de concurrencia:** dos ventas simultáneas del último producto en stock; dos asignaciones de entrenador simultáneas al mismo socio. Ambos deben terminar en un 409, nunca en datos corruptos.

---

## 9. Observabilidad

- Logs JSON con Logback, campo `requestId` propagado desde el header `X-Request-Id` vía filtro MDC.
- `spring-boot-starter-actuator`: `/actuator/health` público, el resto protegido.
- Auditoría con AOP: anotación `@Auditable(accion = "ASIGNAR_ROL")` que inserta en la tabla `auditoria` con `datos_antes`/`datos_despues` en JSONB.
- **Nunca** se loguean contraseñas, tokens completos ni datos de pago. El logger enmascara: `Bearer ***`.

---

## 10. Entorno y despliegue

### 10.1 Variables de entorno

| Variable | Ejemplo | Obligatoria |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://db:5432/gymbro` | Sí |
| `DB_USER` / `DB_PASSWORD` | — | Sí |
| `JWT_SECRET` | cadena de ≥ 32 bytes | Sí |
| `JWT_EXPIRACION_MIN` | `60` | No (default 60) |
| `CORS_ORIGINS` | `http://localhost:5173` | Sí |
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` | Sí |
| `VITE_API_URL` (front) | `http://localhost:8080/api/v1` | Sí |

Hay un `.env.example` versionado; el `.env` real está en `.gitignore`.

### 10.2 Docker Compose (desarrollo)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: gymbro
      POSTGRES_USER: gymbro
      POSTGRES_PASSWORD: gymbro
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gymbro"]
      interval: 5s
      retries: 10

  backend:
    build: { context: ../backend, dockerfile: ../docker/backend.Dockerfile }
    environment:
      DB_URL: jdbc:postgresql://db:5432/gymbro
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: dev
    ports: ["8080:8080"]
    depends_on:
      db: { condition: service_healthy }

  frontend:
    build: { context: ../frontend, dockerfile: ../docker/frontend.Dockerfile }
    environment:
      VITE_API_URL: http://localhost:8080/api/v1
    ports: ["5173:5173"]
    depends_on: [backend]

volumes:
  pgdata:
```

Meta: `docker compose up` deja el sistema completo funcionando en una máquina limpia.

### 10.3 Pipeline CI

En cada push y PR: `mvn verify` (incluye Testcontainers) → `npm run lint` → `npm run test` → `npm run build` → build de imágenes Docker. Un PR no se puede mergear con el pipeline en rojo.

---

## 11. Definition of Ready / Definition of Done

**Una tarea está lista para tomarse (DoR) si:**
- Tiene criterios de aceptación escritos y verificables.
- Sus dependencias están cerradas.
- El contrato de API (request/response) está definido o se define en la misma tarea.

**Una tarea está terminada (DoD) si:**
1. El código está en una rama con PR aprobado por otra persona.
2. Tiene pruebas: unitarias de la regla de negocio + integración del endpoint.
3. Los tres casos de seguridad están cubiertos: sin token → 401; con token sin permiso → 403; con permiso → 200.
4. El endpoint aparece documentado en Swagger con ejemplos.
5. Si tocó la BD, hay una migración Flyway nueva (jamás se edita una migración ya mergeada).
6. El pipeline de CI está verde.
7. Los errores devuelven el formato uniforme de §6.1.

---

## 12. Riesgos técnicos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Permisos en el JWT no reflejan cambios inmediatos | Medio | Documentado y aceptado; expiración corta (60 min). Tabla de revocación si el negocio lo exige. |
| Condición de carrera en stock | Alto | Bloqueo pesimista + `CHECK (stock >= 0)` + prueba de concurrencia. |
| `usuario`/`empleado` mal modelados generan credenciales duplicadas | Alto | Decisión tomada: identidad única con perfil de empleado 1:1. Migrar después es caro. |
| Endpoints "propios" que aceptan id ajeno (IDOR) | Alto | Regla §5.4 + revisión obligatoria en PR de todo endpoint `/me`. |
| Scope creep del módulo de pagos | Medio | v1 solo **registra** el pago, no lo procesa. Está en el fuera de alcance. |
| Front y back avanzando desacoplados | Medio | El contrato OpenAPI se define antes de implementar; el front usa mocks contra ese contrato. |
| Equipo de 2 personas: una baja detiene su carril completo | **Alto** | Revisión cruzada de PR obligatoria, para que ambos conozcan el código del otro. Nada de "esa parte solo la entiende él". |

---

## 13. Glosario técnico

Términos usados en este documento y en el backlog. Sirven también para la sustentación: son el vocabulario que un profesor espera escuchar.

### Arquitectura y desarrollo

| Término | Qué significa |
|---|---|
| **Backend / back** | La parte que no se ve: recibe peticiones, aplica las reglas y habla con la base de datos. Aquí es Java con Spring Boot. |
| **Frontend / front** | La parte que se ve en el navegador. Aquí es React. |
| **API REST** | El conjunto de direcciones (`/api/v1/rutinas`) por las que el front le pide cosas al back usando HTTP. Es el "contrato" entre ambos. |
| **Endpoint** | Una dirección concreta de esa API, con su verbo. Ejemplo: `POST /api/v1/rutinas`. |
| **DTO** (*Data Transfer Object*) | Un objeto simple que solo transporta datos entre el front y el back. Se usa para no exponer las entidades de la base directamente. |
| **Entidad** | Una clase Java que representa una tabla de la base de datos. |
| **Repositorio** | La clase que hace las consultas SQL. En Spring casi no se escribe: se declara y él la implementa. |
| **Servicio** | Donde vive la lógica de negocio ("si no hay stock, rechazar la venta"). |
| **Controlador** | Recibe la petición HTTP y llama al servicio. No debe tener lógica. |
| **Monorepo** | Un solo repositorio de Git que contiene el front y el back. |

### Base de datos

| Término | Qué significa |
|---|---|
| **Migración** | Archivo SQL numerado que aplica un cambio al esquema. Ver §4.1. |
| **Esquema** | La estructura de la base: qué tablas hay, con qué columnas y qué relaciones. |
| **DDL** | *Data Definition Language*: el SQL que crea o modifica estructura (`CREATE TABLE`, `ALTER TABLE`). |
| **Llave foránea (FK)** | Columna que apunta al `id` de otra tabla. Garantiza que no exista una rutina de un usuario inexistente. |
| **Índice parcial único** | Índice que solo aplica a las filas que cumplen una condición. Se usa para forzar "una sola rutina activa por usuario". |
| **Transacción** | Bloque de operaciones que ocurren todas o ninguna. Si falla el descuento de stock, tampoco se guarda la venta. |
| **Bloqueo pesimista** | Reservar una fila mientras se trabaja con ella, para que dos ventas simultáneas no vendan la misma última unidad. |

### Seguridad

| Término | Qué significa |
|---|---|
| **Autenticación** | Verificar quién sos (el login). |
| **Autorización** | Verificar qué podés hacer (los permisos). Son cosas distintas. |
| **RBAC** | *Role-Based Access Control*: dar permisos a roles y roles a usuarios, en lugar de permisos sueltos a cada persona. |
| **JWT** (*JSON Web Token*) | Una cadena firmada que el back entrega al hacer login. El front la manda en cada petición para probar quién es. |
| **BCrypt** | Algoritmo para guardar contraseñas de forma que no se puedan revertir. Nunca se guarda una contraseña en texto plano. |
| **Stateless** | El servidor no recuerda nada entre peticiones; cada una llega con su token y se basta a sí misma. |
| **CORS** | Regla del navegador que decide si una página de un dominio puede llamar a una API de otro. Hay que habilitarlo explícitamente. |
| **IDOR** | Vulnerabilidad en la que alguien cambia un `id` en la petición y accede a datos ajenos. Se evita tomando el id del token, no del cuerpo. |

### Proceso de trabajo

| Término | Qué significa |
|---|---|
| **Rama** (*branch*) | Copia paralela del código donde se trabaja una tarea sin romper lo que ya funciona. |
| **PR** (*Pull Request*) | Solicitud para integrar una rama al código principal. Es donde la otra persona revisa antes de aceptar. |
| **Merge** | Integrar una rama con la principal. |
| **CI** (*Integración Continua*) | Un robot que, en cada push, compila el proyecto y corre las pruebas. Si algo se rompe, avisa de inmediato. |
| **Criterios de aceptación** | La lista de condiciones que deben cumplirse para decir que una tarea está hecha. |
| **DoD** (*Definition of Done*) | Lo mismo pero general: aplica a todas las tareas del proyecto. Ver §11. |
| **Prueba unitaria** | Verifica una función aislada. |
| **Prueba de integración** | Verifica un endpoint completo, con base de datos real incluida. |
| **Testcontainers** | Librería que levanta un PostgreSQL de verdad en Docker durante las pruebas y lo destruye al terminar. |
| **Mock** | Una respuesta falsa que simula al back para que el front pueda avanzar sin esperarlo. |
