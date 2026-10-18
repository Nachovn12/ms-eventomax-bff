# Handoff Técnico - EventoMax EP2: Bloque 1A (BFF)

- **Repositorio:** `ms-eventomax-bff`
- **Rama:** `feature/EMX-bloque-1a-bff`
- **Fecha:** Octubre 2026
- **Estado del bloque:** Implementado y verificado localmente.

---

## 1. Alcance Implementado

En este bloque se expuso y securizó el acceso a los endpoints de equipamiento y reservas de catálogo a través del Backend For Frontend (BFF):

1. **Enrutador de Equipos (`CatalogEquipmentRoutingController`):**
   - `GET /api/catalog/equipment`: Enruta hacia el microservicio `ms-eventomax-catalog`.
     - Permisos: Permitido para roles `ADMIN` y `PRODUCTOR`. Rechazado (`403 Forbidden`) para `ORGANIZADOR` y `AUDITOR`.
   - `POST /api/catalog/equipment`: Enruta la creación de equipamiento.
     - Permisos: Exclusivo para rol `ADMIN`. Rechazado (`403 Forbidden`) para `PRODUCTOR`, `ORGANIZADOR` y `AUDITOR`.

2. **Enrutador de Reservas (`CatalogReservationsRoutingController`):**
   - `GET /api/catalog/reservations/production/{productionId}`: Enruta la consulta de reservas asociadas a una producción.
     - Permisos: Permitido para roles `ADMIN` y `PRODUCTOR`. Rechazado (`403 Forbidden`) para otros roles.

---

## 2. Evidencia de Pruebas Locales

- **Pruebas unitarias e integración de seguridad:** 216/216 PASS (`mvn clean test`).
- Se incorporaron pruebas específicas en `MsEventomaxBffApplicationTests.java` validando la matriz completa de autorización RBAC con simulación de roles `ADMIN`, `PRODUCTOR`, `ORGANIZADOR` y `AUDITOR`.

---

## 3. Limitaciones Conocidas y Trabajo Pendiente

> [!WARNING]
> No se declara 100% verificado ni listo para producción. Quedan pendientes pruebas de integración distribuida (E2E) con los demás microservicios en red compartida y despliegue real en infraestructura cloud/AWS.

- Pruebas con tokens JWT reales de Microsoft Entra ID en entorno Cloud.
- Pruebas E2E de llamadas completas Angular -> BFF -> Catalog en entorno integrado.
- Enrutamiento de bloques posteriores (1B, 2, 3, 4).

---

## 4. Instrucciones para Continuar en el Laboratorio

### Requisitos previos:
- JDK 21 o superior.
- Maven 3.9+ o script `./mvnw`.

### Comandos de clonado y ejecución:
```bash
git clone https://github.com/Nachovn12/ms-eventomax-bff.git
cd ms-eventomax-bff
git checkout feature/EMX-bloque-1a-bff

# Compilar y ejecutar pruebas de enrutamiento y seguridad
./mvnw clean test
```
