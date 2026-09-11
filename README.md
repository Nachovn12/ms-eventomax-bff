# EventoMax BFF

Backend for Frontend de **EventoMax**, responsable de aplicar seguridad y enrutar solicitudes protegidas hacia los microservicios de dominio.

## Tecnologías

- Java 21
- Spring Boot
- Spring Security
- OAuth2 Resource Server
- JWT
- Maven
- OpenAPI / Swagger

## Arquitectura

El BFF forma parte del flujo seguro de EventoMax:

`Angular + MSAL → Microsoft Entra ID → JWT → AWS API Gateway → ms-eventomax-bff → microservicio de dominio`

El API Gateway realiza una primera validación del JWT mediante JWT Authorizer.

El BFF vuelve a validar el token mediante Spring Security antes de permitir el acceso a los servicios internos.

## Responsabilidades

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

Las instrucciones de compilación y ejecución se completarán cuando se inicialice el proyecto Spring Boot.

## Proyecto académico

**Asignatura:** DSY1107 – Desarrollo Cloud Native I  
**Caso:** Caso 8 – EventoMax
