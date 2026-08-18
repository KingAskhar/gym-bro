# Gym-Bro

Sistema de gestión para gimnasios. Proyecto final de carrera.

## Qué es

Aplicación web que permite administrar un gimnasio: socios, membresías,
rutinas de entrenamiento, horarios de clases, control de asistencia,
inventario y ventas de productos.

El sistema distingue tres tipos de usuario, y cada uno ve una interfaz
distinta según los permisos que tenga asignados:

| Rol | Qué puede hacer |
|-----|-----------------|
| **Administrador** | Gestión completa: usuarios, roles, productos, membresías, reportes |
| **Empleado** | Atención diaria: registrar asistencia, vender productos, asignar rutinas |
| **Socio** | Consultar su rutina, su membresía, inscribirse a horarios |

## Tecnologías

**Backend**
- Java 21
- Spring Boot 4.1.0
- Spring Data JPA / Hibernate 7
- Spring Security 7 + JWT
- Flyway (migraciones de base de datos)
- PostgreSQL 18

**Frontend**
- React 18 + TypeScript
- Vite
- React Router
- TanStack Query
- Tailwind CSS

## Estructura del repositorio

```
gym-bro/
├── backend/     Aplicación Java (Spring Boot)
├── frontend/    Aplicación React
├── docker/      Configuración de contenedores
└── docs/        Documentación del proyecto
```

## Documentación

Toda la documentación está en la carpeta `docs/`:

| Documento | Contenido |
|-----------|-----------|
| `spec-funcional-gym-bro.md` | Qué hace el sistema: actores, permisos, requisitos, reglas de negocio |
| `spec-tecnico-gym-bro.md` | Cómo está construido: arquitectura, seguridad, migraciones, convenciones |
| `backlog-tareas-gym-bro.md` | Plan de trabajo: tareas, estimaciones en horas, calendario de 16 semanas |
| `entidades-jpa-gym-bro.md` | Diseño de las entidades JPA y su mapeo a la base de datos |
| `guia-trabajo-con-ia.md` | Metodología de trabajo del equipo y uso responsable de asistentes de IA |

## Cómo levantar el proyecto

### Requisitos previos

- JDK 21
- PostgreSQL 18
- Node.js LTS (solo para el frontend)

### Base de datos

Crear una base de datos vacía llamada `gymbro`. Flyway se encarga de
crear las tablas automáticamente la primera vez que arranca el backend.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Queda disponible en `http://localhost:8080`.
Verificación rápida: `http://localhost:8080/actuator/health`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda disponible en `http://localhost:5173`.

## Estado del proyecto

En desarrollo. Ver `docs/backlog-tareas-gym-bro.md` para el avance detallado.

## Equipo

Proyecto desarrollado por un equipo de dos estudiantes.