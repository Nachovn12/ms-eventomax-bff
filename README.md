# EventoMax BFF

Backend for Frontend de **EventoMax**, responsable de aplicar seguridad y enrutar solicitudes protegidas hacia los microservicios de dominio.

## Estado actual — EP1

Proyecto implementado con Spring Boot 4.1.1. La validación JWT está implementada como OAuth2 Resource Server para los access tokens v2.0 de Microsoft Entra ID: firma, issuer, vigencia (`exp` y `nbf`) y audience.

El flujo completo EP1 está validado en cloud:

```
Angular + MSAL → Microsoft Entra ID → JWT → AWS API Gateway HTTP API JWT Authorizer → ALB → ms-eventomax-bff → Productions/Catalog
```

El BFF no contiene lógica de negocio, no tiene base de datos propia ni accede directamente a PostgreSQL.

## Tecnologías

- Java 25 LTS
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security 7 (versión gestionada por Spring Boot)
- OAuth2 Resource Server
- Spring Boot Actuator
- SpringDoc OpenAPI 3.1.1 (Swagger UI + Bearer JWT)
- Maven Wrapper
- YAML
- Docker (multi-stage build, usuario no-root)

## Arquitectura

El BFF forma parte del flujo seguro de EventoMax:

`Angular + MSAL → Microsoft Entra ID → JWT → AWS API Gateway HTTP API JWT Authorizer → ALB → ms-eventomax-bff → microservicio de dominio`

El API Gateway realiza una primera validación del JWT mediante JWT Authorizer.

El BFF valida nuevamente el token mediante Spring Security, aunque API Gateway también lo valide, y enruta las solicitudes autorizadas a Productions o Catalog.

## Responsabilidades

`ms-eventomax-bff` debe:

- Validar firma, issuer, audience y vigencia del JWT.
- Aplicar autorización según roles y claims.
- Responder `401 Unauthorized` cuando no exista autenticación válida.
- Responder `403 Forbidden` cuando el usuario esté autenticado pero no autorizado.
- Enrutar solicitudes hacia los microservicios de dominio correspondientes.
- Mantener separada la lógica de seguridad y orquestación de la lógica de negocio.

## Microservicios de dominio

Durante EP1 el BFF se integra con:

- `ms-eventomax-productions`
- `ms-eventomax-catalog`

La arquitectura semestral contempla además:

- `ms-eventomax-notify`
- `ms-eventomax-report`
- `ms-eventomax-audit`

## Seguridad

El BFF valida access tokens de la API `eventomax-api`. No utiliza Client Secret: verifica la firma con las claves públicas obtenidas mediante el descubrimiento del issuer de Microsoft Entra ID.

`SecurityConfiguration` combina `JwtValidators.createDefaultWithIssuer(...)` con `AudienceValidator`; así conserva los validadores estándar y añade la comprobación explícita de `aud`. La audience esperada debe aparecer exactamente en el claim; una audience ausente o incorrecta se rechaza. Se conserva la tolerancia temporal estándar de Spring Security de 60 segundos.

El decoder usa `SupplierJwtDecoder` para diferir el descubrimiento y la carga de claves hasta la primera solicitud con Bearer token. El arranque no requiere conectarse a Entra ID; la validación de tokens sí necesita acceso a sus metadatos y claves públicas cuando no están en caché. Este diseño sigue la [documentación oficial de Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

La cadena de seguridad permite `GET /actuator/health` y `GET /actuator/info` sin autenticación. Permitir una ruta no expone el endpoint: se conserva la exposición predeterminada de Actuator, que incluye `health`.

La autenticación usa `Authorization: Bearer`, sin sesiones ni cookies de autenticación. CSRF y el logout de sesión están deshabilitados para este flujo. Las validaciones JWT se conservan.

### Scopes, roles y autorización

`EntraJwtAuthoritiesConverter` combina dos conversores mediante `DelegatingJwtGrantedAuthoritiesConverter`:

- El `JwtGrantedAuthoritiesConverter` estándar conserva la conversión de scopes: `scp: "access_as_user"` produce `SCOPE_access_as_user`.
- Un segundo `JwtGrantedAuthoritiesConverter` lee `roles` y añade el prefijo `ROLE_`: `["Admin", "Productor"]` produce `ROLE_Admin` y `ROLE_Productor`, sin eliminar las authorities `SCOPE_*`.

Los roles funcionales son `Admin`, `Productor`, `Organizador` y `Auditor`, con coincidencia exacta de mayúsculas/minúsculas. No existe una jerarquía implícita: `Admin` no obtiene permisos reservados a otros roles, como crear productions.

Cada regla de dominio requiere **simultáneamente** `SCOPE_access_as_user` y al menos uno de los roles indicados, mediante `AuthorizationManagers.allOf(...)`:

| Método | Ruta | Roles permitidos |
| --- | --- | --- |
| GET | `/api/productions/**` | Admin, Productor, Organizador |
| POST | `/api/productions` | Productor, Organizador |
| PUT | `/api/productions/{id}/status` | Admin, Productor |
| GET | `/api/catalog/**` | Admin, Productor |
| POST | `/api/catalog/**` | Admin |
| PUT | `/api/catalog/**` | Admin |
| GET | `/api/report/**` | Admin |
| GET | `/api/audit/**` | Admin, Auditor |

Las demás combinaciones de método y ruta requieren únicamente autenticación válida. La matriz configura seguridad; solo los contratos de routing descritos abajo tienen controllers productivos. Una ruta autorizada sin contrato implementado devuelve `404` o `405`, sin llamar a un downstream.

### Respuestas 401 y 403

- Sin Bearer token o con JWT inválido: `401 Unauthorized`, mediante `BearerTokenAuthenticationEntryPoint`.
- Con JWT válido pero sin `access_as_user` o sin rol suficiente: `403 Forbidden`, mediante `BearerTokenAccessDeniedHandler`.
- Con JWT válido, scope requerido y rol permitido: la solicitud supera la autorización.

Se configura explícitamente el comportamiento estándar, con cabecera `WWW-Authenticate` y sin cuerpos JSON personalizados. El handler estándar informa `insufficient_scope` también cuando falta un rol permitido.

### OpenAPI / Swagger

SpringDoc OpenAPI 3.1.1 está integrado con esquema de seguridad Bearer JWT (`@SecurityScheme`). Los endpoints de documentación (`/v3/api-docs`, `/swagger-ui.html`) están protegidos por Spring Security y requieren un JWT válido.

### Configuración externalizada

Antes de ejecutar la aplicación, define estas variables en el entorno del proceso o en la configuración de ejecución del IDE:

| Variable | Configuración | Valor requerido |
| --- | --- | --- |
| `ENTRA_ISSUER_URI` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Issuer v2.0 confirmado del tenant de `eventomax-api`, con coincidencia exacta con `iss`. |
| `ENTRA_AUDIENCE` | `eventomax.security.jwt.audience` | Application (client) ID de `eventomax-api` para los access tokens v2.0. No es el scope delegado ni la URI `api://.../access_as_user`. |

Ambas variables son obligatorias y no tienen valores por defecto. No se incluyen identificadores del entorno ni tokens en el código. Un archivo `.env` no se carga automáticamente.

## Routing a Productions y Catalog

`ProductionsRoutingController` y `CatalogRoutingController` declaran los contratos explícitos. Ambos delegan el transporte HTTP a `DomainRoutingClient`, que usa `RestClient`. `DomainRoutingConfiguration` configura destinos y cliente HTTP; aprovecha el `RestClient.Builder` de Boot cuando está disponible y utiliza el builder de Spring Framework en caso contrario, sin añadir un starter.

| Servicio | Método | Contrato |
| --- | --- | --- |
| Productions | POST | `/api/productions` |
| Productions | GET | `/api/productions/{id}` |
| Productions | GET | `/api/productions` |
| Productions | PUT | `/api/productions/{id}/status` |
| Catalog | GET | `/api/catalog/services` |
| Catalog | POST | `/api/catalog/services` |
| Catalog | PUT | `/api/catalog/services/{id}` |

El listado de productions conserva la query original, por ejemplo `?status=CONFIRMADO&from=2026-09-01&to=2026-09-30`, incluidos orden, parámetros repetidos y escapes. El path y la query ya codificados se envían mediante una `URI`, sin expandir plantillas ni recodificarlos.

Se reenvían únicamente `Authorization`, `Content-Type` y `Accept` cuando existen. El token Bearer original se conserva y no se registra. No hay un header de correlación definido en el proyecto. `Host`, `Connection`, `Content-Length` y `Transfer-Encoding` no se copian: el cliente HTTP genera los headers de transporte que necesita.

Los bodies se manejan como bytes opacos, sin DTOs de dominio ni validación de JSON, estados o inventario. Se deshabilita `FormContentFilter` para evitar que MVC consuma el body de formularios PUT antes del routing. Los payloads se almacenan en memoria durante el transporte; habrá que revisar límites/streaming si se incorporan cargas grandes. Las solicitudes HEAD implícitas de MVC no se reenvían, porque no forman parte de los contratos aprobados.

El BFF conserva el status, body, `Content-Type` y `Location` del downstream. Se usa `RestClient.exchange(...)` para propagar también `4xx` y `5xx`, sin convertirlos en `200`; no se siguen redirecciones. Un fallo de conexión o timeout produce `502 Bad Gateway` sin cuerpo. No se aplican reintentos ni circuit breaker. El uso de `URI`, body opaco y `exchange` sigue la [documentación de RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

### Variables de downstream

| Variable | Propiedad | Requisito / valor predeterminado |
| --- | --- | --- |
| `PRODUCTIONS_BASE_URL` | `eventomax.downstream.productions-base-url` | URL HTTP(S) de Productions; obligatoria, sin valor predeterminado. |
| `CATALOG_BASE_URL` | `eventomax.downstream.catalog-base-url` | URL HTTP(S) de Catalog; obligatoria, sin valor predeterminado. |
| `DOWNSTREAM_CONNECT_TIMEOUT` | `eventomax.downstream.connect-timeout` | Opcional; `3s`. |
| `DOWNSTREAM_READ_TIMEOUT` | `eventomax.downstream.read-timeout` | Opcional; `10s`. |

Las URLs base no deben incluir credenciales, query ni fragmento. A la base se añade el path completo del contrato; no se debe repetir `/api/productions` ni `/api/catalog/services` en la base.

## Despliegue cloud — EC2

El BFF se despliega en una instancia EC2 de AWS Academy usando Docker Compose.

### Requisitos

- Docker y Docker Compose instalados en la EC2.
- Red Docker externa `eventomax-net` creada previamente:
  ```bash
  docker network create eventomax-net
  ```
- Los microservicios `ms-eventomax-productions` y `ms-eventomax-catalog` deben estar corriendo y conectados a `eventomax-net`.
- Archivo `.env` con las variables reales (no versionado).

### Despliegue

```bash
cp .env.example .env
# Editar .env con los valores reales de Entra ID

docker compose -f docker-compose.prod.yml up -d --build
```

### Configuración de producción

- **Puerto:** host `8080` → container `8080` (el ALB accede al BFF por este puerto).
- **Restart policy:** `unless-stopped` — el contenedor se reinicia automáticamente tras un reinicio de la EC2 o un crash, evitando el incidente donde el BFF quedaba `Exited` y el ALB devolvía `503`.
- **Red:** `eventomax-net` (externa) — permite comunicación por nombre de servicio con Productions y Catalog.
- **Health check:** El ALB verifica `/actuator/health` (accesible sin JWT). No se agrega `HEALTHCHECK` Docker porque la imagen runtime no incluye `curl`/`wget`.
- **Sin DB:** El BFF no tiene base de datos propia ni accede a RDS.

### Variables de entorno

Copiar `.env.example` a `.env` y completar con valores reales. El archivo `.env` está excluido de Git por `.gitignore`.

## Credenciales

No se deben almacenar en este repositorio:

- Client Secrets
- Access Tokens
- credenciales AWS
- credenciales PostgreSQL
- archivos `.env` reales
- passwords o claves privadas

## Estrategia de ramas

- `main`: versión estable y preparada para entrega.
- `develop`: rama de integración.
- `feature/*`: desarrollo de historias de usuario.
- `fix/*`: correcciones.
- `chore/*`: configuración e infraestructura.

Flujo de integración:

`feature/* → Pull Request → develop → pruebas → Pull Request → main`

## Ejecución local

Requisito: **JDK 25**, con `JAVA_HOME` apuntando al JDK. No se requiere instalar Maven por separado; se utiliza el Wrapper incluido.

`spring-boot:run` requiere las variables anteriores. Los tests suministran su propia configuración y no requieren variables reales de Entra ID.

### Windows

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw clean test
./mvnw spring-boot:run
```

### Pruebas automatizadas

Las pruebas unitarias cubren la coincidencia exacta de audience, múltiples audiences, claims ausentes/vacíos y configuración de audience inválida.

También verifican el mapeo `ROLE_*`, múltiples roles, conservación de `SCOPE_*`, comportamiento estándar de claims de scopes, duplicados y sensibilidad a mayúsculas/minúsculas.

Las pruebas de integración ejercitan el decoder, la cadena de seguridad y los controllers reales con JWT sintéticos firmados mediante claves RSA efímeras. `LocalJwtIssuer` proporciona metadatos/JWKS, y dos instancias de `LocalDownstreamServer` capturan las solicitudes a Productions y Catalog. Todos utilizan loopback y puertos efímeros; no se realizan llamadas a Internet ni a Microsoft Entra ID, ni se necesita Docker.

Las pruebas de routing verifican los siete contratos, body/path/query/headers, estados `201`/`202`/`204`, errores downstream `400`/`401`/`403`/`404`/`409`/`500`, redirecciones y fallo de conexión `502`. Las pruebas de seguridad comprueban que una solicitud rechazada no llama al downstream. Los dumps de MockMvc están deshabilitados para no imprimir Bearer tokens.

Las pruebas de OpenAPI runtime verifican que `/v3/api-docs` requiere JWT (`401` sin token, `200` con token válido) y que Swagger UI redirige correctamente bajo autenticación.

## Proyecto académico

**Asignatura:** DSY1107 – Desarrollo Cloud Native I
**Caso:** Caso 8 – EventoMax
