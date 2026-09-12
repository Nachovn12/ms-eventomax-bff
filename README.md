# EventoMax BFF

Backend for Frontend de **EventoMax**, responsable de aplicar seguridad y enrutar solicitudes protegidas hacia los microservicios de dominio.

## Estado actual — EMX-46

Proyecto inicializado mediante Spring Initializr, con Maven Wrapper y configuración YAML. La validación JWT está implementada como OAuth2 Resource Server para los access tokens v2.0 de Microsoft Entra ID: firma, issuer, vigencia (`exp` y `nbf`) y audience.

Todavía no están implementados:

- Mapping de roles/claims y autorización por rol (EMX-47).
- Reglas de autorización por scopes.
- Respuestas personalizadas `401` y `403` (EMX-15).
- Routing hacia microservicios.
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

La cadena de seguridad permite `/actuator/health` y `/actuator/info` sin autenticación; el resto requiere un JWT válido. Permitir una ruta no expone el endpoint: se conserva la exposición predeterminada de Actuator, que incluye `health`.

La autenticación usa `Authorization: Bearer`, sin sesiones ni cookies de autenticación. CSRF y el logout de sesión están deshabilitados para este flujo. Se mantienen las respuestas estándar de Spring Security, sin conversores personalizados de authorities ni reglas por roles/scopes.

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

Las pruebas de integración ejercitan el decoder y la cadena de seguridad reales con JWT sintéticos firmados mediante claves RSA efímeras. Un servidor HTTP en loopback, con puerto asignado dinámicamente, proporciona metadatos y JWKS exclusivamente de prueba; no se realizan llamadas a Internet ni a Microsoft Entra ID.

Se verifica la aceptación de un token válido y el rechazo de audience incorrecta, issuer incorrecto, token expirado, `nbf` futuro, firma inválida, audience ausente y token malformado. También se comprueban los endpoints públicos, la autenticación obligatoria y el flujo Bearer sin sesión. El endpoint `/test/protected` existe únicamente en el código de tests.

## Proyecto académico

**Asignatura:** DSY1107 – Desarrollo Cloud Native I  
**Caso:** Caso 8 – EventoMax
