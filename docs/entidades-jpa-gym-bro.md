# Gym-Bro — Capa de Entidades (JPA / Hibernate)

**Versión:** 1.0 · 8 de agosto de 2026
**Origen:** temas vistos en clase sobre entidades en Spring Boot JPA / Hibernate
**Complementa:** Especificación Técnica §3 y §4

> Este documento traduce cada tema de la clase a una decisión concreta dentro de Gym-Bro. Sirve para dos cosas: guiar la implementación de las tareas `BE-01` y siguientes, y tener a mano las respuestas cuando el profesor pregunte "¿dónde aplicaste esto?".

---

## 1. Checklist: tema de clase → dónde vive en Gym-Bro

| Tema de la clase | Dónde se aplica en Gym-Bro | Archivo |
|---|---|---|
| `@Entity` | Las 20 entidades del modelo | todas |
| `@Table` | Todas, con nombre explícito en snake_case | todas |
| `@Id` + `@GeneratedValue` | Heredado de `BaseEntity` | `common/BaseEntity.java` |
| Estrategia `IDENTITY` | Elegida por usar `BIGSERIAL` en PostgreSQL | `BaseEntity` |
| `@Column` | Longitudes, `nullable`, `unique`, `precision` | todas |
| `@Transient` | `Usuario.nombreCompleto`, `Suscripcion.diasRestantes` | `Usuario`, `Suscripcion` |
| `LocalDate` / `LocalDateTime` | Fechas de nacimiento, contratación, vigencias, timestamps | varias |
| Texto largo | `Membresia.beneficios`, `Ejercicio.instrucciones` | ver §4 |
| `@Enumerated(STRING)` | 10 enumeraciones del dominio | `enums/` |
| `@Embeddable` / `@Embedded` | `Direccion` en `Usuario`, `RangoHorario` en `Horario` | `common/embeddable/` |
| `@MappedSuperclass` | `BaseEntity` con id, auditoría y versión | `common/BaseEntity.java` |
| Lombok correcto | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` | todas |
| **No usar `@Data`** | Regla del proyecto, documentada en §7 | — |
| `@Builder.Default` | `activo = true`, `stock = 0`, estados iniciales | varias |
| `@EqualsAndHashCode` seguro | Resuelto en `BaseEntity` por `id` | `BaseEntity` |
| `@ToString` sin relaciones | Excluye colecciones y `@ManyToOne` | todas |

**Cobertura: 100% de los temas de la clase.** No hay que forzar nada — el modelo de Gym-Bro los pedía de forma natural.

---

## 2. El choque con el material de clase (y cómo resolverlo)

El material de clase muestra el DDL **generado automáticamente por Hibernate** a partir de las entidades. Nuestra especificación técnica usa Flyway con `ddl-auto: validate`, o sea justo lo contrario: las tablas se crean con SQL escrito por nosotros y Hibernate solo verifica que coincidan.

Ambos enfoques son correctos, pero para cosas distintas:

| | Hibernate genera el DDL | Flyway con SQL propio |
|---|---|---|
| Para aprender el mapeo | Excelente: ves qué produce cada anotación | No lo muestra |
| Para un proyecto que evoluciona | Malo: no versiona, no borra columnas viejas | Es exactamente su propósito |
| Índices parciales, `EXCLUDE`, `CHECK` | **No los puede generar** | Sí |
| Reproducir la base desde cero | No garantizado | Garantizado |

### La solución: usar los dos, cada uno para lo suyo

Hibernate puede **escribir el DDL en un archivo sin tocar la base de datos**. Así obtienen el artefacto de la clase y conservan Flyway:

```properties
# application-dev.properties — solo en desarrollo
spring.jpa.hibernate.ddl-auto=validate

# Hibernate escribe el DDL que ÉL generaría, en un archivo, sin ejecutarlo
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/ddl-generado-por-hibernate.sql
```

Al arrancar, `target/ddl-generado-por-hibernate.sql` contiene el `CREATE TABLE` que Hibernate deduce de las anotaciones. Ese archivo se usa de tres formas:

1. **Como punto de partida** para escribir la migración Flyway: se copia, se le agregan los índices parciales y restricciones que Hibernate no sabe generar, y queda `V1__esquema_base.sql`.
2. **Como verificación**: si el DDL generado y la migración difieren en algo inesperado, hay un error de mapeo.
3. **Como material de sustentación** (ver §9): comparar los dos archivos es la mejor forma de explicar por qué usan Flyway.

> **Sobre `application.properties` vs `application.yml`:** el material de clase usa `.properties`. Nuestro spec usa `.yml`. Son equivalentes; usen el que el profesor espere ver. Si es `.properties`, no hay que cambiar nada más del diseño.

---

## 3. Estructura de paquetes de la capa de modelo

```
com.gymbro
├── common/
│   ├── BaseEntity.java              ← @MappedSuperclass
│   └── embeddable/
│       ├── Direccion.java           ← @Embeddable
│       └── RangoHorario.java        ← @Embeddable
├── usuario/
│   ├── Usuario.java
│   └── UsuarioRepository.java
├── rol/
│   ├── Rol.java
│   └── Permiso.java
├── producto/
│   └── Producto.java
├── horario/
│   └── Horario.java
├── membresia/
│   ├── Membresia.java
│   └── Suscripcion.java
└── enums/
    ├── EstadoMaquina.java · ObjetivoRutina.java · DiaSemana.java
    ├── TipoHorario.java   · CanalVenta.java    · EstadoVenta.java
    ├── MetodoPago.java    · ConceptoPago.java  · EstadoSuscripcion.java
    └── EstadoInscripcion.java
```

Las entidades viven **dentro del paquete de su módulo**, no en un paquete `model/` global. Es coherente con la organización por funcionalidad del spec técnico §3.1. Si el profesor pide la estructura clásica (`model/`, `repository/`, `service/`, `controller/`), es un cambio de carpetas sin impacto en el código.

---

## 4. Paso 1 — La clase base (`BaseEntity`)

Todas las entidades comparten `id`, fechas de auditoría y control de versión. Poner eso en una clase base evita repetirlo 20 veces.

```java
package com.gymbro.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Clase base de dominio. NO es una entidad: no genera tabla propia.
 * Sus columnas se copian a la tabla de cada entidad que la herede.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /** Bloqueo optimista: evita que dos ediciones simultáneas se pisen. */
    @Version
    private Long version;

    // equals y hashCode escritos a mano, NO con Lombok. Ver explicación abajo.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity otra = (BaseEntity) o;
        // Dos entidades sin id todavía guardado nunca son iguales,
        // salvo que sean literalmente el mismo objeto (caso de arriba).
        return id != null && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        // Constante por clase: el hash NO puede cambiar cuando la entidad
        // pasa de no-guardada (id null) a guardada (id asignado).
        return getClass().hashCode();
    }
}
```

Para que la auditoría funcione hay que habilitarla una sola vez:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig { }
```

### Por qué `equals` y `hashCode` van a mano

El material de clase advierte que `@EqualsAndHashCode` de Lombok es peligroso en entidades, y tiene razón. El detalle de por qué:

1. **Por defecto compara todos los campos.** Si `Usuario` tiene una lista de rutinas y `Rutina` apunta al usuario, comparar dos usuarios entra en recursión infinita → `StackOverflowError`.
2. **Toca las colecciones perezosas.** Comparar dispara la carga de relaciones `LAZY` fuera de la transacción → `LazyInitializationException`.
3. **El hash cambia al guardar.** Si el `hashCode` depende del `id` y el `id` es `null` antes de guardar, una entidad metida en un `HashSet` antes del `save()` queda irrecuperable después: quedó archivada bajo un hash que ya no le corresponde. Por eso el `hashCode` de arriba usa la clase, no el id.

Si prefieren usar Lombok igual, la única forma segura es:

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// y sobre el campo id:
@EqualsAndHashCode.Include
private Long id;
```

Pero con `@MappedSuperclass` eso obliga a `callSuper = true` en cada hija y se enreda. Escribirlo una vez en la clase base es más simple y más fácil de defender.

### Sobre `GenerationType`

| Estrategia | Cuándo | En Gym-Bro |
|---|---|---|
| `IDENTITY` | La columna es `SERIAL`/`BIGSERIAL`; la base asigna el id al insertar | ✅ **La que usamos**: nuestro DDL define `BIGSERIAL` |
| `SEQUENCE` | Hibernate pide números a una secuencia; permite inserciones en lote | Mejor rendimiento en inserciones masivas, innecesario aquí |
| `AUTO` | Hibernate elige | Se evita: hace el comportamiento impredecible entre bases |
| `TABLE` | Simula secuencias con una tabla | Obsoleta, lenta. No usar |

**Respuesta corta para la sustentación:** `IDENTITY`, porque las tablas usan `BIGSERIAL` y así hay una sola fuente de verdad para los ids, que es PostgreSQL. La contra conocida es que deshabilita el `batch insert` de Hibernate, algo irrelevante en este proyecto.

---

## 5. Paso 2 — Las enumeraciones

Todas con `EnumType.STRING`, sin excepción.

```java
package com.gymbro.enums;

public enum EstadoMaquina { OPERATIVA, MANTENIMIENTO, BAJA }

public enum ObjetivoRutina { FUERZA, HIPERTROFIA, RESISTENCIA, PERDIDA_PESO }

public enum DiaSemana {
    LUNES(1), MARTES(2), MIERCOLES(3), JUEVES(4),
    VIERNES(5), SABADO(6), DOMINGO(7);

    private final int numero;
    DiaSemana(int numero) { this.numero = numero; }
    public int getNumero() { return numero; }
}

public enum TipoHorario { CLASE, DISPONIBILIDAD_ENTRENADOR }
public enum CanalVenta { MOSTRADOR, ONLINE }
public enum EstadoVenta { COMPLETADA, ANULADA }
public enum MetodoPago { EFECTIVO, TARJETA, TRANSFERENCIA }
public enum ConceptoPago { MEMBRESIA, VENTA }
public enum EstadoSuscripcion { ACTIVA, VENCIDA, CANCELADA }
public enum EstadoInscripcion { INSCRITO, CANCELADO }
```

### Por qué nunca `ORDINAL`

`ORDINAL` guarda la **posición** del valor en el enum: `OPERATIVA` → 0, `MANTENIMIENTO` → 1, `BAJA` → 2.

El desastre ocurre el día que alguien agrega un valor en el medio:

```java
public enum EstadoMaquina { OPERATIVA, ALQUILADA, MANTENIMIENTO, BAJA }
```

Ahora el 1 significa `ALQUILADA`, pero en la base de datos hay cientos de filas con un 1 que se grabaron cuando 1 era `MANTENIMIENTO`. **Todas las máquinas en mantenimiento pasaron a estar alquiladas**, silenciosamente, sin ningún error. No hay forma de detectarlo ni de revertirlo sin saber cuándo se hizo el cambio.

Con `STRING` la columna dice `MANTENIMIENTO` y reordenar el enum no rompe nada. Cuesta unos bytes más por fila; es el mejor negocio del proyecto.

En Gym-Bro hay una razón adicional: el DDL del spec técnico ya define esas columnas como `VARCHAR` con nombres legibles. Con `ORDINAL` el `ddl-auto: validate` fallaría al arrancar, porque Hibernate esperaría un entero.

---

## 6. Paso 3 — Las clases embebidas

Un objeto embebido agrupa varias columnas que siempre viajan juntas y no merecen tabla propia. Dos casos reales en Gym-Bro:

### `Direccion` — dentro de `Usuario`

```java
package com.gymbro.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Direccion {

    @Column(name = "direccion_calle", length = 120)
    private String calle;

    @Column(name = "direccion_ciudad", length = 60)
    private String ciudad;

    @Column(name = "direccion_departamento", length = 60)
    private String departamento;

    @Column(name = "direccion_codigo_postal", length = 10)
    private String codigoPostal;
}
```

### `RangoHorario` — dentro de `Horario`

```java
@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RangoHorario {

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Lógica de dominio dentro del embebido: no necesita la base de datos. */
    public boolean seSolapaCon(RangoHorario otro) {
        return horaInicio.isBefore(otro.horaFin) && otro.horaInicio.isBefore(horaFin);
    }

    public int duracionMinutos() {
        return (int) java.time.Duration.between(horaInicio, horaFin).toMinutes();
    }
}
```

**Por qué es un buen ejemplo y no relleno académico:** `seSolapaCon` es la validación de RF-EMP-04. Vive dentro del objeto que representa el concepto, es fácil de probar sin base de datos, y si mañana aparece un rango horario en otra entidad, se reutiliza.

**Detalle importante:** un `@Embeddable` **no genera tabla**. Sus columnas se agregan a la tabla de la entidad que lo contiene. `usuario` tendrá `direccion_calle`, `direccion_ciudad`, etc. Si dos embebidos de la misma clase van en una entidad, hay que renombrar columnas con `@AttributeOverride`.

---

## 7. Paso 4 — Una entidad completa

`Producto` es el análogo directo del ejemplo de clase, así que sirve de referencia.

```java
package com.gymbro.producto;

import com.gymbro.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor                       // OBLIGATORIO para JPA
@AllArgsConstructor
@Builder
@ToString(callSuper = true)              // sin relaciones: esta entidad no tiene
public class Producto extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** NUNCA double ni float para dinero. BigDecimal + NUMERIC(10,2). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** Lógica de dominio: RN-02, el stock nunca queda negativo. */
    public void descontarStock(int cantidad) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        if (this.stock < cantidad)
            throw new IllegalStateException(
                "Stock insuficiente de " + nombre + ": hay " + stock + ", se piden " + cantidad);
        this.stock -= cantidad;
    }
}
```

### `Usuario`, con `@Transient` y embebido

```java
@Entity
@Table(name = "usuario")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(callSuper = true, exclude = {"passwordHash", "roles"})   // ← clave
public class Usuario extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Embedded
    private Direccion direccion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_rol",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"))
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    /** Calculado en memoria: NO existe columna en la tabla. */
    @Transient
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Transient
    public Integer getEdad() {
        return fechaNacimiento == null ? null
             : Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}
```

Dos detalles que valen nota:

- **`@ToString` excluye `passwordHash`.** Si no, el hash de la contraseña acaba impreso en los logs el primer día. Es un error de seguridad real, no un tecnicismo.
- **`@ToString` excluye `roles`.** Imprimir una colección `LAZY` fuera de la transacción lanza `LazyInitializationException`; y si `Rol` tuviera de vuelta una lista de usuarios, sería recursión infinita.

### `Suscripcion`, con campo calculado

```java
@Entity
@Table(name = "suscripcion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(callSuper = true, exclude = {"usuario", "membresia"})
public class Suscripcion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membresia_id", nullable = false)
    private Membresia membresia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoSuscripcion estado = EstadoSuscripcion.ACTIVA;

    /** RF-USR-09 y RF-MEM-04: se calcula, no se guarda. */
    @Transient
    public long getDiasRestantes() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
    }

    @Transient
    public boolean isPorVencer() {
        long dias = getDiasRestantes();
        return dias >= 0 && dias <= 7;
    }
}
```

**Por qué `diasRestantes` es `@Transient` y no una columna:** una columna guardada quedaría desactualizada mañana. El dato se deriva de `fechaFin`, y guardar datos derivados es cómo se corrompen las bases.

---

## 8. Textos largos: el detalle de `@Lob` con PostgreSQL

El material de clase presenta `@Lob` para textos largos. Cuidado con esto en PostgreSQL, porque es una trampa clásica:

```java
@Lob
private String beneficios;   // ⚠️ Hibernate lo mapea a una columna OID, no a TEXT
```

Hibernate mapea `@Lob String` a un **`oid`** en PostgreSQL: un puntero a un objeto grande almacenado aparte. El resultado es que un `SELECT` directo devuelve un número en lugar del texto, y el `ddl-auto: validate` falla contra una columna declarada como `TEXT`.

En Gym-Bro no hay archivos binarios, solo texto largo. La forma correcta:

```java
@Column(columnDefinition = "TEXT")
private String beneficios;              // ✅ TEXT de verdad en PostgreSQL
```

`@Lob` queda reservado para si en el futuro se guardan imágenes de productos como `byte[]`, y en ese caso lo recomendable sería guardar la imagen en disco o en almacenamiento externo y en la base solo la ruta.

Vale la pena que este detalle esté en el documento final: muestra que probaron el mapeo contra PostgreSQL real y no solo copiaron la anotación.

---

## 9. Las reglas de oro (versión Gym-Bro)

| # | Regla | Por qué |
|---|---|---|
| 1 | **Nunca `@Data` en una entidad** | Ver §10 |
| 2 | `@NoArgsConstructor` siempre | Hibernate crea las instancias por reflexión: sin constructor vacío no puede |
| 3 | `@Enumerated(EnumType.STRING)` siempre | `ORDINAL` corrompe datos en silencio |
| 4 | `BigDecimal` para dinero, nunca `double` | `0.1 + 0.2 != 0.3` en punto flotante |
| 5 | `LocalDate` / `LocalDateTime`, nunca `java.util.Date` | La API vieja es mutable y ambigua |
| 6 | `fetch = FetchType.LAZY` explícito en `@ManyToOne` | El **default es EAGER**, y trae media base de datos en cada consulta |
| 7 | `@ToString` excluyendo relaciones y datos sensibles | Recursión infinita y contraseñas en los logs |
| 8 | `equals`/`hashCode` solo por `id`, en `BaseEntity` | Ver §4 |
| 9 | `@Builder.Default` en todo campo con valor inicial | Sin esto, el `builder` deja `null` y se pierde el default |
| 10 | La entidad nunca sale del controlador | Se mapea a DTO. Spec técnico §3.2 |
| 11 | Lógica de dominio dentro de la entidad, no solo getters | `descontarStock()` es mejor que `setStock()` desde el servicio |

La regla 6 no está en el glosario de la clase y es la que más problemas de rendimiento causa en proyectos reales. Vale la pena mencionarla en la sustentación.

---

## 10. Por qué `@Data` es un antipatrón en entidades

`@Data` es un atajo que activa cinco cosas a la vez: `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode` y `@RequiredArgsConstructor`. Tres de esas cinco son un problema:

| Lo que incluye | Por qué rompe |
|---|---|
| `@EqualsAndHashCode` sobre **todos** los campos | Recursión infinita en relaciones bidireccionales → `StackOverflowError`. Y el hash cambia al guardar. |
| `@ToString` sobre **todos** los campos | Imprime el `passwordHash` en los logs, y carga colecciones `LAZY` → `LazyInitializationException`. |
| `@RequiredArgsConstructor` | Genera un constructor con los campos `final`/`@NonNull`, pero **no** el constructor sin argumentos que JPA necesita. |

Lo grave es que nada de esto falla al compilar. Falla en tiempo de ejecución, semanas después, con un error que no menciona a Lombok en ninguna parte.

**La alternativa es escribir las cinco anotaciones que sí quieren.** Son tres líneas más y se sabe exactamente qué hace cada una:

```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(callSuper = true, exclude = {"passwordHash", "roles"})
```

---

## 11. Cómo probarlo (lo que hicieron en clase, adaptado)

El ejemplo de clase probaba las entidades desde la clase principal. Para Gym-Bro conviene una prueba de verdad, que además cuenta como tarea `QA` del backlog:

```java
@DataJpaTest
class UsuarioEntityTest {

    @Autowired TestEntityManager em;

    @Test
    void guardaYRecuperaUsuarioConDireccionEmbebida() {
        var usuario = Usuario.builder()
            .nombre("Ana").apellido("Pérez")
            .documento("1001").email("ana@mail.com")
            .passwordHash("$2a$12$fakehash")
            .direccion(Direccion.builder()
                .calle("Cra 50 #10-20").ciudad("Bello")
                .departamento("Antioquia").build())
            .build();

        var guardado = em.persistFlushFind(usuario);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getActivo()).isTrue();               // @Builder.Default funcionó
        assertThat(guardado.getFechaCreacion()).isNotNull();     // auditoría funcionó
        assertThat(guardado.getNombreCompleto()).isEqualTo("Ana Pérez");  // @Transient
        assertThat(guardado.getDireccion().getCiudad()).isEqualTo("Bello");
    }

    @Test
    void elToStringNoExponeElHashDeLaPassword() {
        var usuario = Usuario.builder()
            .nombre("Ana").apellido("Pérez").documento("1002")
            .email("ana2@mail.com").passwordHash("$2a$12$secreto").build();

        assertThat(usuario.toString()).doesNotContain("secreto");
    }
}
```

Ese segundo test es corto y demuestra criterio de seguridad. Vale mostrarlo.

---

## 12. Preguntas probables del profesor

Ensayen estas respuestas en voz alta antes de la sustentación.

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué `@MappedSuperclass` y no `@Entity` en `BaseEntity`? | Porque no queremos una tabla `base_entity`. `@MappedSuperclass` copia las columnas a cada tabla hija; `@Entity` con herencia crearía una tabla y estrategias de join innecesarias. |
| ¿Por qué `IDENTITY` y no `SEQUENCE`? | Las tablas usan `BIGSERIAL`, así que PostgreSQL es la única fuente de ids. La contra es que deshabilita inserciones en lote, irrelevante en este proyecto. |
| ¿Qué pasa si uso `ORDINAL`? | Se guarda la posición. Al agregar un valor en medio del enum, todas las filas existentes cambian de significado sin error. Ejemplo concreto: máquinas en mantenimiento pasarían a otro estado. |
| ¿Por qué no `@Data`? | Trae `equals`, `hashCode` y `toString` sobre todos los campos: recursión infinita en relaciones bidireccionales, hash que cambia al guardar, y contraseñas en los logs. |
| ¿Por qué `@NoArgsConstructor` es obligatorio? | Hibernate instancia las entidades por reflexión al leer de la base; necesita un constructor sin argumentos para hacerlo. |
| ¿Dónde está la lógica de negocio? | La que depende solo de la entidad, dentro de ella (`descontarStock`, `seSolapaCon`). La que coordina varias entidades, en el servicio. |
| ¿Por qué Flyway si Hibernate puede generar las tablas? | Porque Hibernate no genera índices parciales ni restricciones `EXCLUDE`, que en este proyecto son las que garantizan "una sola rutina activa" y "horarios sin solape". Y no versiona los cambios. Podemos mostrar los dos archivos y las diferencias. |
| ¿`@Transient` no desperdicia procesamiento? | Calcular `diasRestantes` es una resta de fechas. Guardarlo sería un dato que queda desactualizado al día siguiente. |

**El momento fuerte de la demo:** abrir `target/ddl-generado-por-hibernate.sql` al lado de `V1__esquema_base.sql` y señalar qué falta en el primero. Demuestra que entienden las dos herramientas y por qué eligieron una.
