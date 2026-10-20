# ms-eventomax-bff — Backend for Frontend, Enrutamiento de Dominio y Seguridad OAuth2/JWT (EP2)

**Asignatura:** Desarrollo Cloud Native I (`DSY1107`)
**Caso de Estudio:** Caso 8 — Plataforma de Producción y Montaje de Eventos (**EventoMax**)
**Equipo:** Ignacio Valeria, Benjamín Flores, Benjamín Espinoza

---

## 1. Propósito del Repositorio y Rol en EventoMax

`ms-eventomax-bff` actúa como la puerta de enlace de aplicación (*Backend for Frontend*) entre **AWS API Gateway** (y el frontend Angular 22 SPA) y los microservicios internos de **EventoMax EP2** desplegados sobre la red privada Docker `eventomax-net`.

Sus responsabilidades principales son:
1. **Validación de Seguridad en Segunda Capa (Defense-in-Depth):** Valida criptográficamente el JWT emitido por **Microsoft Entra ID** (`issuer`, `audience`, `exp`, `scope = access_as_user` y claim `roles` de aplicación) aplicando una matriz RBAC granular por ruta y verbo HTTP.
2. **Enrutamiento y Propagación de Contexto (`DomainRoutingClient`):** Reenvía encabezados de autorización (`Authorization`), correlación distribuida (`X-Trace-Id`, `X-Correlation-Id`) y descarga documental (`Content-Disposition`) hacia `ms-eventomax-productions`, `ms-eventomax-catalog`, `ms-eventomax-notify`, `ms-eventomax-report`, `ms-eventomax-audit` y `rabbit-admin`.
3. **Verificación Previa de Propiedad (Anti-IDOR en Artefactos):** Antes de servir el ticket de cuadrilla (`GET /api/productions/{id}/crew-ticket`) o el Acta PDF de Cierre (`GET /api/productions/{id}/quote-pdf`) desde `ms-eventomax-notify`, el BFF verifica en `ms-eventomax-productions` que el solicitante tenga acceso autorizado a la producción `{id}`.

---

## 2. Arquitectura del Módulo y Diagrama

```mermaid
flowchart LR
    SPA["Angular 22 SPA\n(MSAL OAuth2 PKCE)"] -->|HTTPS + Bearer JWT| APIGW["AWS API Gateway\n(JWT Authorizer)"]
    APIGW --> BFF["ms-eventomax-bff (:8080 / :8081)"]

    subgraph Red Interna Docker eventomax-net
        PROD["ms-eventomax-productions"]
        CAT["ms-eventomax-catalog"]
        NOTIFY["ms-eventomax-notify"]
        REP["ms-eventomax-report"]
        AUD["ms-eventomax-audit"]
        RADMIN["ms-eventomax-rabbit-admin"]
    end

    BFF -->|/api/productions/**\n/api/organizers/**| PROD
    BFF -->|/api/catalog/**| CAT
    BFF -->|/api/productions/{id}/quote-pdf\n/api/productions/{id}/crew-ticket| NOTIFY
    BFF -->|/api/report/**| REP
    BFF -->|/api/audit/**| AUD
    BFF -->|/api/rabbit-admin/**| RADMIN
```

---

## 3. Stack Tecnológico y Versiones Reales (`pom.xml`)

| Componente | Tecnología / Dependencia | Versión |
|---|---|---|
| Lenguaje | Java (Eclipse Temurin) | `25` |
| Framework Base | Spring Boot (`spring-boot-starter-parent`) | `4.1.1` |
| Web / Cliente HTTP | `spring-boot-starter-webmvc` (`RestClient` con timeouts explícitos) | Boot `4.1.1` |
| Seguridad | `spring-boot-starter-security` + `oauth2-resource-server` | Boot `4.1.1` |
| Observabilidad | `spring-boot-starter-actuator` + OpenAPI (`springdoc`) | `3.0.3` |
| Testing | JUnit 5 (`@ParameterizedTest` matriz RBAC de 230 casos), MockMvc | Boot `4.1.1` |

---

## 4. Cumplimiento de Rúbrica EP2 (Matriz Caso 8)

| Criterio de Evaluación EP2 | Implementación en `ms-eventomax-bff` | Estado |
|---|---|---|
| **Autenticación Microsoft Entra ID y Autorización RBAC** | Validación de `issuer-uri`, `AudienceValidator`, scope `access_as_user` y roles `Admin`, `Productor`, `Organizador`, `Auditor` (`EntraJwtAuthoritiesConverter`). | **CUMPLIDO** |
| **Integración con Artefactos RabbitMQ / Notify** | Enrutamiento seguro de `GET /api/productions/{id}/quote-pdf` y `GET /api/productions/{id}/crew-ticket` hacia `ms-eventomax-notify` previa validación de propiedad de la producción. | **CUMPLIDO** |
| **Enrutamiento hacia Rabbit Admin (`AmqpAdmin`)** | `RabbitAdminRoutingController` expone `/api/rabbit-admin/**` exclusivamente al rol `Admin` (`scopeAndAnyRole(ADMIN)`). | **CUMPLIDO** |
| **Trazabilidad y Manejo de Errores Downstream** | Propagación de `X-Trace-Id`, `X-Correlation-Id`, `Content-Disposition` y traducción de fallos de red/timeout a `502 Bad Gateway` / `504 Gateway Timeout`. | **CUMPLIDO** |

---

## 5. Matriz Completa de Endpoints y Roles Autorizados

Todas las rutas `/api/**` exigen `SCOPE_access_as_user`:

| Método | Ruta Expuesta en BFF | Destino Downstream | Roles Permitidos |
|---|---|---|---|
| `GET` | `/api/productions/**` | `ms-eventomax-productions` | `Admin`, `Productor`, `Organizador` |
| `POST` | `/api/productions` | `ms-eventomax-productions` | `Admin`, `Organizador` |
| `PUT` | `/api/productions/*/status` | `ms-eventomax-productions` | `Admin`, `Productor` |
| `POST`, `DELETE` | `/api/productions/*/services/**` | `ms-eventomax-productions` | `Admin`, `Productor`, `Organizador` |
| `GET` | `/api/productions/{id}/crew-ticket` | `ms-eventomax-productions` (check) → `ms-eventomax-notify` | `Admin`, `Productor` |
| `GET` | `/api/productions/{id}/quote-pdf` | `ms-eventomax-productions` (check) → `ms-eventomax-notify` | `Admin`, `Productor`, `Organizador` |
| `GET` | `/api/organizers/{organizerId}/contact` | `ms-eventomax-productions` | `Admin`, `Productor`, `Organizador` |
| `PUT` | `/api/organizers/{organizerId}/contact` | `ms-eventomax-productions` | `Admin`, `Organizador` |
| `GET` | `/api/catalog/services/**`, `/api/catalog/equipment/**` | `ms-eventomax-catalog` | `Admin`, `Productor`, `Organizador` |
| `POST`, `PUT` | `/api/catalog/services/**`, `/api/catalog/equipment/**` | `ms-eventomax-catalog` | `Admin` |
| `GET` | `/api/catalog/reservations/productions/*` | `ms-eventomax-productions` (check) → `ms-eventomax-catalog` | `Admin`, `Productor`, `Organizador` |
| `POST` | `/api/catalog/reservations/**` | `ms-eventomax-catalog` | `Admin`, `Productor` |
| `GET` | `/api/report/kpis`, `/api/report/top-services` | `ms-eventomax-report` | `Admin`, `Auditor` |
| `GET` | `/api/audit/timeline` | `ms-eventomax-audit` | `Admin`, `Auditor` |
| `GET`, `POST`, `DELETE` | `/api/rabbit-admin/**` | `ms-eventomax-rabbit-admin` | `Admin` |

---

## 6. Seguridad y Autenticación/Autorización JWT

- **Issuer:** `https://login.microsoftonline.com/f191bdc8-7926-4722-9ec0-cba663701940/v2.0` (`ENTRA_ISSUER_URI`).
- **Audience:** `api://a1a87bcd-fed7-4d65-83c3-338389b80093` (`ENTRA_AUDIENCE`).
- **Regla Deny-by-Default:** Cualquier ruta no declarada explícitamente en `SecurityConfiguration` es rechazada con `.anyRequest().denyAll()`.
- **Respuestas Determinísticas:** `401 Unauthorized` para tokens ausentes/inválidos y `403 Forbidden` (`application/problem+json`) para roles o scopes insuficientes.

---

## 7. Integración con RabbitMQ y Kafka (Rol de Pasarela)

El BFF no se conecta directamente a los brokers AMQP/Kafka, sino que expone de forma autenticada:
- La descarga de los artefactos generados asíncronamente por RabbitMQ en `ms-eventomax-notify` (`/crew-ticket` y `/quote-pdf`).
- La administración programática de colas, exchanges y bindings (`demo.*`) hacia `rabbit-admin` (`/api/rabbit-admin/**`).
- Las consultas sobre las proyecciones Kafka de `ms-eventomax-report` y `ms-eventomax-audit`.

---

## 8. Base de Datos

`ms-eventomax-bff` es un servicio **100% stateless**; no mantiene base de datos propia ni migraciones Flyway.

---

## 9. Variables de Entorno Requeridas

| Variable | Ejemplo Placeholder | Descripción |
|---|---|---|
| `ENTRA_ISSUER_URI` | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` | Issuer URI de Microsoft Entra ID |
| `ENTRA_AUDIENCE` | `api://a1a87bcd-fed7-4d65-83c3-338389b80093` | Audience del API en Entra ID |
| `PRODUCTIONS_BASE_URL` | `http://ms-eventomax-productions:8080` | URL interna de Productions |
| `CATALOG_BASE_URL` | `http://ms-eventomax-catalog:8080` | URL interna de Catalog |
| `REPORT_BASE_URL` | `http://ms-eventomax-report:8080` | URL interna de Report |
| `AUDIT_BASE_URL` | `http://ms-eventomax-audit:8080` | URL interna de Audit |
| `NOTIFY_BASE_URL` | `http://ms-eventomax-notify:8080` | URL interna de Notify |
| `RABBIT_ADMIN_BASE_URL` | `http://ms-eventomax-rabbit-admin:8080` | URL interna de Rabbit Admin |
| `DOWNSTREAM_CONNECT_TIMEOUT` | `3s` | Timeout de conexión hacia microservicios |
| `DOWNSTREAM_READ_TIMEOUT` | `10s` | Timeout de lectura hacia microservicios |

---

## 10. Ejecución Local Paso a Paso

```bash
export ENTRA_ISSUER_URI="https://login.microsoftonline.com/f191bdc8-7926-4722-9ec0-cba663701940/v2.0"
export ENTRA_AUDIENCE="api://a1a87bcd-fed7-4d65-83c3-338389b80093"
export PRODUCTIONS_BASE_URL="http://localhost:8082"
export CATALOG_BASE_URL="http://localhost:8080"
export REPORT_BASE_URL="http://localhost:8084"
export AUDIT_BASE_URL="http://localhost:8085"
export NOTIFY_BASE_URL="http://localhost:8083"
export RABBIT_ADMIN_BASE_URL="http://localhost:8086"

mvn spring-boot:run
```

---

## 11. Ejecución de Pruebas Automatizadas

```bash
mvn clean test
```

**Resultado certificado en EP2:** `Tests run: 230, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`.

---

## 12. Construcción y Despliegue Docker / Preparación Cloud

```bash
docker build -t ms-eventomax-bff:ep2 .
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 13. Estructura del Proyecto

```text
ms-eventomax-bff/
├── src/main/java/cl/duoc/eventomax/bff/
│   ├── routing/
│   │   ├── AuditRoutingController.java
│   │   ├── CatalogEquipmentRoutingController.java
│   │   ├── CatalogReservationsRoutingController.java
│   │   ├── CatalogRoutingController.java
│   │   ├── DomainRoutingClient.java
│   │   ├── DomainRoutingConfiguration.java
│   │   ├── OrganizersRoutingController.java
│   │   ├── ProductionsRoutingController.java
│   │   ├── RabbitAdminRoutingController.java
│   │   └── ReportRoutingController.java
│   └── security/
│       ├── AudienceValidator.java
│       ├── EntraJwtAuthoritiesConverter.java
│       └── SecurityConfiguration.java
├── src/main/resources/application.yaml
├── Dockerfile
└── docker-compose.prod.yml
```

---

## 14. Estado Actual de Certificación EP2 y Trazabilidad Jira

- **Pruebas Automatizadas:** `230/230` aprobadas.
- **Historias Jira:** `EMX-72` (Rutas Audit/Report), `EMX-Bloque-1A` (Equipos y Reservas), `EP2-FIX-01` (Rutas Notify Quote PDF, Crew Ticket, Contactos de Organizador y Rabbit Admin).
