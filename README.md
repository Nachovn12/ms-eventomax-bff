# EventoMax BFF

Backend for Frontend de **EventoMax**, responsable de aplicar seguridad y enrutar solicitudes protegidas hacia los microservicios de dominio.

## Estado actual — EMX-43

Proyecto inicializado mediante Spring Initializr, con Maven Wrapper, configuración YAML mínima y prueba de carga de contexto (`contextLoads()`).

Todavía no están implementados:

- Validación JWT e integración con Microsoft Entra ID.
- Autorización mediante roles y claims.
- Respuestas personalizadas `401` y `403`.
- Routing hacia microservicios.
- Integración con AWS API Gateway.

El BFF no contiene lógica de negocio, no tiene base de datos propia ni accede directamente a PostgreSQL.

## Tecnologías

- Java 25 LTS
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security
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

El BFF volverá a validar el token mediante Spring Security antes de permitir el acceso a los servicios internos.

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

La validación JWT utilizará Microsoft Entra ID como proveedor de identidad.

La configuración sensible deberá suministrarse mediante variables de entorno o mecanismos seguros equivalentes.

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

## Proyecto académico

**Asignatura:** DSY1107 – Desarrollo Cloud Native I  
**Caso:** Caso 8 – EventoMax
