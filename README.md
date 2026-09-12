# EventoMax BFF

Backend for Frontend de **EventoMax**, responsable de aplicar seguridad y enrutar solicitudes protegidas hacia los microservicios de dominio.

## Estado actual — EMX-47

Proyecto inicializado mediante Spring Initializr, con Maven Wrapper y configuración YAML. La validación JWT está implementada como OAuth2 Resource Server para los access tokens v2.0 de Microsoft Entra ID: firma, issuer, vigencia (`exp` y `nbf`) y audience.

EMX-47 añade autorización por scope y App Roles de Entra ID, con respuestas estándar `401` y `403`.

Todavía no están implementados:

- Routing hacia microservicios (EMX-48).
- Integración con AWS API Gateway.

El BFF no contiene lógica de negocio, no tiene base de datos propia ni accede directamente a PostgreSQL.

## Tecnologías

- Java 25 LTS
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security 7 (versión gestionada por Spring Boot)
- OAuth2 Resource Server
- Spring Boot Actuator
- Maven Wrapper
- YAML

### Tecnología planificada

- OpenAPI / Swagger

## Arquitectura

El BFF formará parte del flujo seguro previsto de EventoMax:

`Angular + MSAL → Microsoft Entra ID → JWT → AWS API Gateway → ms-eventomax-bff → microservicio de dominio`

El API Gateway realizará una primera validación del JWT mediante JWT Authorizer.

El BFF valida nuevamente el token mediante Spring Security, aunque API Gateway también lo valide. El routing a los servicios internos queda pendiente.

## Responsabilidades previstas

`ms-eventomax-bff` debe:

- Validar firma, issuer, audience y vigencia del JWT.
- Aplicar autorización según roles y claims.
- Responder `401 Unauthorized` cuando no exista autenticación válida.
- Responder `403 Forbidden` cuando el usuario esté autenticado pero no autorizado.
- Enrutar solicitudes hacia los microservicios de dominio correspondientes.
- Mantener separada la lógica de seguridad y orquestación de la lógica de negocio.

## Microservicios de dominio

Durante EP1 el BFF se integrará inicialmente con:

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

La autenticación usa `Authorization: Bearer`, sin sesiones ni cookies de autenticación. CSRF y el logout de sesión están deshabilitados para este flujo. Las validaciones JWT de EMX-46 se conservan.

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

Las demás combinaciones de método y ruta requieren únicamente autenticación válida. Esta matriz configura seguridad; todavía no implementa endpoints de dominio ni routing.

### Respuestas 401 y 403

- Sin Bearer token o con JWT inválido: `401 Unauthorized`, mediante `BearerTokenAuthenticationEntryPoint`.
- Con JWT válido pero sin `access_as_user` o sin rol suficiente: `403 Forbidden`, mediante `BearerTokenAccessDeniedHandler`.
- Con JWT válido, scope requerido y rol permitido: la solicitud supera la autorización.

Se configura explícitamente el comportamiento estándar, con cabecera `WWW-Authenticate` y sin cuerpos JSON personalizados. El handler estándar informa `insufficient_scope` también cuando falta un rol permitido.

### Configuración externalizada

Antes de ejecutar la aplicación, define estas variables en el entorno del proceso o en la configuración de ejecución del IDE:

| Variable | Configuración | Valor requerido |
| --- | --- | --- |
| `ENTRA_ISSUER_URI` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Issuer v2.0 confirmado del tenant de `eventomax-api`, con coincidencia exacta con `iss`. |
| `ENTRA_AUDIENCE` | `eventomax.security.jwt.audience` | Application (client) ID de `eventomax-api` para los access tokens v2.0. No es el scope delegado ni la URI `api://.../access_as_user`. |

Ambas variables son obligatorias y no tienen valores por defecto. No se incluyen identificadores del entorno ni tokens en el código. Un archivo `.env` no se carga automáticamente.

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

Las pruebas de integración ejercitan el decoder y la cadena de seguridad reales con JWT sintéticos firmados mediante claves RSA efímeras. Un servidor HTTP en loopback, con puerto asignado dinámicamente, proporciona metadatos y JWKS exclusivamente de prueba; no se realizan llamadas a Internet ni a Microsoft Entra ID.

Se conservan las pruebas de EMX-46: token válido, audience/issuer incorrectos, expiración, `nbf`, firma inválida, audience ausente, token malformado y flujo sin sesión. EMX-47 añade la matriz completa de los cuatro roles, scope ausente/incorrecto, rol ausente/incorrecto, respuestas `401`/`403` y límites de métodos/patrones de rutas.

El controller que responde `200` en `/test/protected` y `/api/**` existe únicamente en `src/test/java`; permite comprobar la autorización sin implementar routing ni endpoints ficticios en producción.

## Proyecto académico

**Asignatura:** DSY1107 – Desarrollo Cloud Native I  
**Caso:** Caso 8 – EventoMax
