# Click&Eat · Backend

API REST de **Click&Eat**, la plataforma de pedidos de comida a domicilio y para recoger.

Este documento explica **cómo arrancar, configurar y probar** el backend en local, cómo se gestiona **la base de datos**, y **por qué** está montado así.

---

## Índice

1. [Stack y versiones](#1-stack-y-versiones)
2. [Estructura del módulo](#2-estructura-del-módulo)
3. [Requisitos previos](#3-requisitos-previos)
4. [Configuración](#4-configuración)
5. [Cómo arrancar en local](#5-cómo-arrancar-en-local)
6. [Comprobar que funciona (Actuator)](#6-comprobar-que-funciona-actuator)
7. [Tests](#7-tests)
8. [Comandos útiles](#8-comandos-útiles)
9. [Problemas frecuentes](#9-problemas-frecuentes)
10. [Decisiones técnicas](#10-decisiones-técnicas)
11. [Base de datos y migraciones](#11-base-de-datos-y-migraciones)


---

## 1. Stack y versiones

| Pieza | Versión | Para qué |
|---|---|---|
| Java | 25 (LTS) | Lenguaje |
| Spring Boot | 4.1.2 | Framework (Spring Framework 7) |
| Maven | Maven Wrapper (`./mvnw`) | Construcción y dependencias. No hace falta instalar Maven |
| PostgreSQL | 18 | Base de datos, en Docker Compose (`infra/`) |
| Flyway | 12.x | Migraciones versionadas del esquema de la base de datos |
| Hibernate | 7.x | Implementación de JPA (traduce objetos Java a tablas) |
| HikariCP | 7.x | Pool de conexiones a la base de datos |
| Spring Boot Actuator | 4.1.2 | Endpoints de estado de la aplicación |
| JUnit 5 + Testcontainers 2 | — | Tests con un PostgreSQL real y temporal |

Proyecto generado con Spring Initializr:
https://start.spring.io/#!type=maven-project&language=java&platformVersion=4.1.2&packaging=jar&configurationFileFormat=yaml&jvmVersion=25&groupId=io.github.eladiolramos&artifactId=clickandeat&packageName=io.github.eladiolramos.clickandeat&dependencies=web,data-jpa,postgresql,validation,actuator,testcontainers

---

## 2. Estructura del módulo

```
backend/
├── .mvn/wrapper/                        ← versión de Maven que usa el wrapper
├── mvnw, mvnw.cmd                       ← Maven Wrapper (Linux / Windows)
├── pom.xml                              ← dependencias y plugins
├── README.md                            ← este documento
└── src/
    ├── main/
    │   ├── java/io/github/eladiolramos/clickandeat/
    │   │   └── ClickandeatApplication.java      ← punto de entrada
    │   └── resources/
    │       ├── application.yaml                 ← configuración COMÚN (todos los entornos)
    │       ├── application-dev.yaml             ← configuración solo de DESARROLLO
    │       └── db/
    │           ├── migration/                   ← migraciones de esquema (todos los entornos)
    │           │   ├── V1__create_table_category.sql
    │           │   └── V2__create_table_product.sql
    │           └── seed/                        ← datos de ejemplo (solo perfil dev)
    │               └── R__catalog_sample_data.sql
    └── test/
        └── java/io/github/eladiolramos/clickandeat/
            ├── TestcontainersConfiguration.java ← PostgreSQL temporal para los tests
            ├── ClickandeatApplicationTests.java ← comprueba que la app arranca (Surefire)
            ├── ActuatorHealthIT.java            ← comprueba /actuator/health (Failsafe)
            └── TestClickandeatApplication.java  ← arrancar la app con BD de Testcontainers
```

El código se organizará **por funcionalidad** (`catalogo/`, `usuario/`, `pedido/`, `shared/`) a partir del paso 1.3.

---

## 3. Requisitos previos

| Requisito | Comprobación |
|---|---|
| **Java 25** (JDK) | `java -version` y `./mvnw -v` → Java 25 |
| **Docker** con Docker Compose | `docker info` sin errores |
| En Windows: proyecto **dentro de WSL** (Ubuntu), no en `C:\` | `pwd` → `/home/<usuario>/…` |
| `infra/.env` creado | ver [sección 5.1](#51-crear-el-env-solo-la-primera-vez) |

---

## 4. Configuración

### 4.1 Ficheros y perfiles

| Fichero | Cuándo se carga | Qué contiene |
|---|---|---|
| `application.yaml` | **Siempre** | Conexión a la BD (con variables), ajustes de JPA, Actuator con la mínima exposición |
| `application-dev.yaml` | Solo con el perfil **`dev`** | Importa `infra/.env`, añade la carpeta `db/seed` a Flyway (datos de ejemplo) y muestra los detalles de salud |

El perfil activo **lo decide el entorno** (IDE, terminal o servidor), nunca el propio fichero. Si una propiedad está en los dos ficheros, gana la del perfil.

### 4.2 Variables de entorno

El backend **no contiene credenciales**. Lee la conexión de estas variables, que son **las mismas que usa Docker Compose** (`infra/.env`):

| Variable | Para qué | ¿Obligatoria? | Por defecto en el backend |
|---|---|---|---|
| `DB_HOST` | Host de PostgreSQL visto desde el backend | No | `127.0.0.1` |
| `DB_HOST_PORT` | Puerto de PostgreSQL **en la máquina anfitriona** (parte izquierda de `ports:` en `compose.yaml`) | No | `5432` |
| `POSTGRES_DB` | Nombre de la base de datos | No | `clickandeat` |
| `POSTGRES_USER` | Usuario de PostgreSQL | **Sí** | — |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL | **Sí** | — |

Con ellas, Spring construye la URL de conexión:

```
jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_HOST_PORT:5432}/${POSTGRES_DB:clickandeat}
                  └────── host ──────┘ └───── puerto ─────┘ └──── base de datos ────┘
```

> ⚠️ `infra/.env` **nunca se sube a Git** (lo ignora `.gitignore`). Se crea a partir de `infra/.env.example`. Los valores van **sin comillas**.

### 4.3 Ajustes de JPA / Hibernate

| Propiedad | Valor | Motivo |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | El esquema lo crea Flyway. Hibernate solo **comprueba** al arrancar que las entidades coinciden con las tablas; nunca las crea ni las modifica |
| `spring.jpa.open-in-view` | `false` | La conexión solo se usa mientras el servicio trabaja con la BD; evita consultas ocultas (N+1) y no retiene conexiones del pool |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `UTC` | Fechas coherentes sea cual sea la zona horaria del servidor |
| `spring.datasource.hikari.pool-name` | `clickandeat-pool` | Identificar el pool en logs y métricas |

### 4.4 Actuator

| Propiedad | Común | `dev` |
|---|---|---|
| `management.endpoints.web.exposure.include` | `health,info` | (hereda) |
| `management.endpoint.health.show-details` | `never` | `always` |
| `management.endpoint.health.probes.enabled` | `true` | (hereda) |

---

## 5. Cómo arrancar en local

### 5.1 Crear el `.env` (solo la primera vez)

```bash
cd ~/proyectos/clickandeat
cp infra/.env.example infra/.env
# Editar infra/.env con los valores reales (sin comillas)
```

### 5.2 Levantar PostgreSQL

```bash
cd ~/proyectos/clickandeat/infra
docker compose up -d
docker compose ps        # esperar a "running (healthy)"
```

### 5.3 Arrancar el backend con el perfil `dev`

> ⚠️ **La aplicación se arranca SIEMPRE desde la carpeta `backend/`.** El perfil `dev` importa `../infra/.env`, una ruta relativa a esa carpeta. Desde cualquier otra carpeta, el `.env` no se carga y la conexión falla con `${POSTGRES_USER}` literal.

**Opción A · Terminal (Warp / bash)**

```bash
cd ~/proyectos/clickandeat/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Para pararla: `Ctrl+C`.

**Opción B · IntelliJ IDEA**

En *Run → Edit Configurations → ClickandeatApplication*:

| Campo | Valor |
|---|---|
| Active profiles | `dev` |
| Working directory (*Modify options → Working directory*) | la carpeta `backend` del proyecto |

En *Settings → Build Tools → Maven → Runner*:

| Opción | Valor | Motivo |
|---|---|---|
| Delegate IDE build/run actions to Maven | ✅ activada | Con el proyecto en WSL, el compilador propio de IntelliJ falla (`Failed to copy additional plugin`) |
| Skip Tests | ✅ activada | Para que ▶ no ejecute todos los tests antes de arrancar |

Después, ▶ Run.

### 5.4 Log de un arranque correcto

```
Starting ClickandeatApplication … in /home/<usuario>/proyectos/clickandeat/backend   ← carpeta correcta
The following 1 profile is active: "dev"                                              ← perfil dev
clickandeat-pool - Start completed.                                                   ← conectado a PostgreSQL
Schema "public" is up to date. No migration necessary.                                ← Flyway (o "Migrating…" si hay algo nuevo)
Database dialect: PostgreSQLDialect                                                   ← Hibernate lo detecta solo
Exposing 2 endpoints beneath base path '/actuator'                                    ← health e info
Started ClickandeatApplication in X seconds
```

No debe aparecer el aviso de `spring.jpa.open-in-view`.

---

## 6. Comprobar que funciona (Actuator)

Con la aplicación arrancada:

```bash
curl -i http://localhost:8080/actuator/health          # 200 y "status":"UP" (detalles en dev)
curl -i http://localhost:8080/actuator/health/liveness # ¿el proceso está vivo?
curl -i http://localhost:8080/actuator/health/readiness# ¿puede recibir tráfico?
curl -i http://localhost:8080/actuator/info
curl -i http://localhost:8080/actuator/env             # 404: NO está expuesto (correcto)
```

Con `jq` se lee mejor: `curl -s http://localhost:8080/actuator/health | jq`

### Qué significa cada componente de `/actuator/health`

| Componente | Comprueba |
|---|---|
| `db` | Que PostgreSQL responde (pide una conexión al pool y la valida con `isValid()`) |
| `diskSpace` | Que queda espacio en disco por encima del umbral |
| `livenessState` / `readinessState` | Las probes para orquestadores |
| `ping` | Siempre `UP` |
| `ssl` | Estado de los certificados SSL configurados (ninguno por ahora) |

El estado global es `UP` (HTTP **200**) solo si **todos** los componentes lo están; si alguno está `DOWN`, devuelve **503**.

### Comportamiento si cae la base de datos

| Situación | `/actuator/health` |
|---|---|
| PostgreSQL parado | **503** · `DOWN` · `db: DOWN`. Tarda hasta ~30 s (`connection-timeout` de HikariCP). La app **sigue viva** |
| PostgreSQL vuelve | **200** · `UP`, **sin reiniciar la app**: el pool recupera las conexiones solo |

---

## 7. Tests

```bash
cd ~/proyectos/clickandeat/backend
./mvnw test      # solo tests unitarios (Surefire)
./mvnw verify    # todos: unitarios + de integración (Surefire + Failsafe)
```

- Necesitan **Docker en marcha**: cada fase arranca un **PostgreSQL 18 temporal** con Testcontainers, en un puerto aleatorio, y lo borra al terminar.
- **No necesitan** `infra/.env` ni variables de entorno: `@ServiceConnection` conecta Spring al contenedor automáticamente, con prioridad sobre `spring.datasource.*`.
- Flyway aplica las migraciones de `db/migration` en el contenedor vacío, así que cada ejecución de los tests comprueba también que las migraciones funcionan. **No** se aplica el seed: los tests no usan el perfil `dev`.

| Test | Tipo | Plugin | Comprueba |
|---|---|---|---|
| `ClickandeatApplicationTests` | Arranque del contexto | Surefire (`*Tests`) | Que la app arranca con una BD real |
| `ActuatorHealthIT` | Integración (MockMvc) | Failsafe (`*IT`) | Que `GET /actuator/health` responde `200` y `"status":"UP"` |

Ejecutar un solo test de integración:

```bash
./mvnw verify -Dit.test=ActuatorHealthIT
```

---

## 8. Comandos útiles

| Comando (desde `backend/`) | Para qué |
|---|---|
| `./mvnw -v` | Versión de Maven y de Java que se usa |
| `./mvnw compile` | Compilar |
| `./mvnw clean` | Borrar `target/` |
| `./mvnw test` | Tests unitarios |
| `./mvnw verify` | Todos los tests (lo que ejecutará la CI) |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | Arrancar en desarrollo |
| `./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug` | Ver el informe de autoconfiguración |
| `./mvnw dependency:tree` | Árbol de dependencias |

---

## 9. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `password authentication failed for user "${POSTGRES_USER}"` (con `${…}` literal) | No se ha cargado `infra/.env`: la app no se arrancó desde `backend/` o el perfil `dev` no está activo | Arrancar desde `backend/` con el perfil `dev` (sección 5.3) |
| `Unable to determine Dialect without JDBC metadata` | **Consecuencia**: Hibernate no pudo conectarse | Buscar más arriba la causa real (normalmente un error de conexión). **No** configurar `hibernate.dialect` |
| `Failed to configure a DataSource: 'url' attribute is not specified` | Sin perfil activo o error de sangría en el YAML | Activar `dev`; revisar la sangría |
| `Connection refused` | PostgreSQL parado o `DB_HOST_PORT` distinto | `docker compose up -d`; revisar el puerto |
| `/actuator/health` tarda ~30 s | PostgreSQL no responde: HikariCP espera su `connection-timeout` | Levantar PostgreSQL |
| `Failed to copy additional plugin` (IntelliJ) | Compilador de IntelliJ con proyecto en WSL | Activar *Delegate IDE build/run actions to Maven* |
| Los tests fallan con `Could not find a valid Docker environment` | Docker Desktop parado | Arrancar Docker Desktop |
| Aviso `CRLF will be replaced by LF` al hacer `git add` | Fichero creado desde Windows con finales CRLF | IntelliJ → *Code Style → Line separator: Unix (\n)* |
| Avisos de Mockito / "Java agent loaded dynamically" en los tests | Aviso conocido de Java 21+ | No afecta; se ignora |
| `Migration checksum mismatch for migration version N` (la app no arranca) | Se ha editado una migración que ya estaba aplicada | Deshacer el cambio (`git restore <fichero>`) y hacer el cambio en una migración **nueva**. Solo si la migración no ha salido de tu máquina: editarla y recrear la BD local (sección 11.2) |
| `Schema-validation: missing table [...]` o `missing column [...]` | Una entidad JPA no coincide con las tablas (`ddl-auto: validate`) | Crear la migración que falta o corregir el mapeo de la entidad. **Nunca** cambiar a `update` |
| `Could not transfer metadata ... HTTP Status: 401` (Red-gate, flyway-community-db-support) en Maven | Repositorios que declara el BOM de Flyway y que requieren autenticación | Inofensivo; se ignora |

---

## 10. Decisiones técnicas

### Spring Boot 4.1 y Java 25
Spring Boot 3.5 dejó de tener soporte gratuito en junio de 2026; la rama 4.1 lo tiene hasta julio de 2027. Java 25 es la LTS vigente y Spring Boot 4.1 la soporta.

### Coordenadas Maven: `io.github.eladiolramos` / `clickandeat`
El `groupId` sigue la convención de dominio invertido basada en una identidad que controlo (mi usuario de GitHub), en lugar de `com.example`. El `artifactId` `clickandeat` identifica la aplicación principal del monorepo; los futuros servicios llevarán sufijo (por ejemplo, `clickandeat-notifications`).

### Configuración en YAML
Funcionalmente equivale a `.properties`. Se elige por su legibilidad jerárquica y por coherencia con el resto de ficheros del proyecto (`compose.yaml`, workflows de CI). Exige cuidar la sangría.

### Credenciales solo por variables de entorno, reutilizando las de Docker Compose
El código y el YAML no contienen secretos. Spring usa **las mismas variables** que Compose (`POSTGRES_*`, `DB_HOST_PORT`), así hay una única fuente de verdad (`infra/.env`) para crear la base de datos y para conectarse a ella. Host, puerto y base tienen valor por defecto porque no son secretos; usuario y contraseña no, para no dejar credenciales en el repositorio.

### Carga del `.env` con `spring.config.import` en el perfil `dev`
Funciona igual desde cualquier IDE y desde la terminal, sin depender de configuraciones locales del IDE. Va solo en el perfil `dev` porque en otros entornos no existe el `.env`: las variables las aporta el propio entorno. Se usa `optional:` para que no falle donde no exista, y una ruta **relativa** (`../infra/.env`) para que funcione en cualquier máquina que clone el repo.

### Host por defecto `127.0.0.1`
El `compose.yaml` publica PostgreSQL solo en la interfaz de loopback IPv4 (`127.0.0.1`), para no exponer la base de datos a la red. Usar la misma dirección evita que `localhost` se resuelva a IPv6 (`::1`) y no encuentre el servicio.

### JPA: `ddl-auto: validate`, `open-in-view: false` y UTC
El esquema es responsabilidad de migraciones versionadas (Flyway), nunca de Hibernate: `validate` solo comprueba al arrancar que las entidades y las tablas coinciden, y si no, la app no arranca (mejor fallar al arrancar que en mitad de una petición). OSIV se desactiva para no retener conexiones durante toda la petición ni ocultar consultas. Las fechas se guardan en UTC para que no dependan de la zona horaria del servidor.

### Actuator con exposición mínima
Solo se exponen `health` e `info`; endpoints como `env`, `beans` o `heapdump` revelarían configuración o memoria interna. Los detalles de salud solo se muestran en `dev`. Las probes de liveness y readiness se activan pensando en el despliegue (Corte 5); la base de datos no debe afectar a la liveness, porque reiniciar el backend no la recupera.

### Tests contra PostgreSQL real con Testcontainers
H2 no se comporta como PostgreSQL. Los tests usan la **misma imagen** que Compose (`postgres:18`, fijada en lugar de `latest`) para probar exactamente lo mismo que se usa en desarrollo. Los tests de integración (`*IT`) se separan con Failsafe para que `./mvnw test` sea rápido y `./mvnw verify` lo ejecute todo.

---

## 11. Base de datos y migraciones

### 11.1 Cómo funciona

- El esquema se gestiona con **Flyway**. Las migraciones son ficheros SQL versionados dentro del repositorio.
- Flyway se ejecuta **al arrancar la aplicación, antes que Hibernate**: aplica las migraciones pendientes y después Hibernate valida el resultado (`ddl-auto: validate`).
- Flyway apunta en la tabla **`flyway_schema_history`** qué migraciones ha aplicado, cuándo y con qué **checksum** (una huella calculada sobre el texto del fichero). Así cada migración se aplica **una sola vez** en cada base de datos.

| Carpeta | Contenido | Dónde se aplica |
|---|---|---|
| `src/main/resources/db/migration` | Migraciones de esquema (`V__`) | **Todos** los entornos: desarrollo, tests y producción (ubicación por defecto de Flyway) |
| `src/main/resources/db/seed` | Datos de ejemplo (`R__`) | **Solo** con el perfil `dev`, que la añade en `spring.flyway.locations` |

El seed **no llega a los tests ni a producción** porque esos entornos no activan el perfil `dev`, así que Flyway nunca ve la carpeta `db/seed`. Los tests empiezan siempre con las tablas vacías y crean sus propios datos.

> En `application-dev.yaml` se indica también `classpath:db/migration`, porque definir `spring.flyway.locations` **reemplaza** la ubicación por defecto en lugar de ampliarla.

### 11.2 Reglas

**Nombre de los ficheros** (con **doble guion bajo**):

| Tipo | Formato | Ejemplo | Comportamiento |
|---|---|---|---|
| Versionada | `V<n>__<descripcion>.sql` | `V3__create_table_customer.sql` | Se aplica **una vez**, en orden de versión |
| Repetible | `R__<descripcion>.sql` | `R__catalog_sample_data.sql` | Se aplica **después** de las versionadas y **de nuevo cada vez que cambia** su contenido. Debe ser **idempotente** (`ON CONFLICT DO NOTHING`) |

Descripción en inglés y en snake_case, con el patrón `create_table_<tabla>`, `add_column_<columna>_to_<tabla>`, etc.

**Una migración aplicada no se edita nunca.**

| Situación | Qué hacer |
|---|---|
| La migración ya está en `develop`, `main` o cualquier otro entorno | **No se toca.** El cambio se hace con una migración **nueva** (por ejemplo, `V3__…` con un `ALTER TABLE`). Si se edita, su checksum deja de coincidir y la app no arranca (`Migration checksum mismatch`); además, los entornos donde ya se aplicó se quedarían con el esquema antiguo |
| La migración solo existe en tu máquina (aún no ha llegado a `develop`) | Se puede editar, recreando la base de datos local: `cd infra && docker compose down -v && docker compose up -d`. ⚠️ `-v` **borra todos los datos locales** |

**Otras reglas:**
- Las migraciones y el seed **solo los ejecuta Flyway al arrancar**. Nunca se lanzan a mano desde IntelliJ (▶) ni desde psql.
- Nada de `IF NOT EXISTS` en las migraciones versionadas: Flyway ya garantiza que se ejecutan una sola vez, y si algo no cuadra es mejor que falle.
- `ddl-auto` nunca en `update` ni en `create`.
- El seed **no borra** lo que se quita del fichero (`INSERT` solo añade). Para limpiar la base local: `docker compose down -v`.

### 11.3 Esquema actual

```
category                          product
─────────────────────────         ──────────────────────────────────
id          BIGINT  PK            id          BIGINT  PK
name        VARCHAR(50) UNIQUE    name        VARCHAR(100) UNIQUE
created_at  TIMESTAMPTZ           description TEXT        (opcional)
updated_at  TIMESTAMPTZ           image_url   TEXT        (opcional)
                                  price       NUMERIC(10,2)  CHECK >= 0
                                  available   BOOLEAN     DEFAULT TRUE
                                  category_id BIGINT  FK → category (RESTRICT)
                                  created_at  TIMESTAMPTZ
                                  updated_at  TIMESTAMPTZ
```

### 11.4 Decisiones de diseño

| Decisión | Motivo |
|---|---|
| **Idioma:** tablas, columnas, código, URLs, tests y commits en **inglés**; documentación y datos (nombres de categorías y productos) en **español** | Estándar del sector y coherencia con el código Java; los datos son los que ve el cliente final |
| **snake_case** y nombres en minúscula | PostgreSQL pasa a minúsculas los nombres sin comillas; así no hay que entrecomillar nunca |
| **Restricciones con nombre** (`pk_`, `uk_`, `ck_`, `fk_`, `idx_`) | Los errores dicen exactamente qué regla se ha roto (por ejemplo, `ck_product_price_non_negative`) |
| `BIGINT GENERATED ALWAYS AS IDENTITY` para los `id` | Estándar SQL; impide insertar ids a mano y da margen de sobra |
| `NUMERIC(10,2)` para el precio (`BigDecimal` en Java) | Exacto: `float`/`double` producen errores de redondeo con dinero |
| `CHECK (price >= 0)` | Se admite precio 0 para extras gratuitos (salsas…); nunca negativo |
| Longitudes: `category.name` 50, `product.name` 100; `description` e `image_url` como `TEXT` | Límites razonables para nombres; en PostgreSQL `TEXT` no penaliza y evita cortar descripciones o URLs largas |
| Nombre de categoría y de producto **únicos** | Evita duplicados en la carta y permite el `ON CONFLICT (name)` del seed |
| `ON DELETE RESTRICT` en `product.category_id` | No se puede borrar una categoría que tiene productos; nunca se pierden productos en cascada |
| Índice `idx_product_category_id` | PostgreSQL crea índices para `PRIMARY KEY` y `UNIQUE`, pero **no** para las claves foráneas. Acelera listar los productos de una categoría y la comprobación del `RESTRICT` al borrar |
| `available` en lugar de borrar productos | Un producto agotado o retirado se oculta sin perder su historial (los pedidos futuros lo referenciarán) |
| Auditoría `created_at` / `updated_at` (`TIMESTAMPTZ`, UTC) | Saber cuándo se creó y modificó cada fila. `DEFAULT NOW()` solo actúa en el `INSERT`; `updated_at` lo mantendrá Hibernate desde las entidades (paso 1.3) |
| Seed como `R__` en carpeta aparte | Se puede editar libremente (se reaplica al cambiar) sin consumir números de versión, y no contamina tests ni producción |

### 11.5 Consultar la base de datos

**psql** (cliente de terminal dentro del contenedor), desde `infra/`:

```bash
cd ~/proyectos/clickandeat/infra
docker compose exec postgres-db psql -U "$(grep ^POSTGRES_USER .env | cut -d= -f2)" -d "$(grep ^POSTGRES_DB .env | cut -d= -f2)"
```

El comando lee el usuario y la base del `.env`, así no se escriben credenciales a mano.

| Comando de psql | Para qué |
|---|---|
| `\dt` | Listar las tablas |
| `\d product` | Ver columnas, restricciones e índices de una tabla |
| `SELECT * FROM flyway_schema_history;` | Ver qué migraciones se han aplicado |
| `\q` | Salir |

> En Warp, pega los comandos de psql **de uno en uno**: un meta-comando como `\d` se come las líneas que vienen detrás.

**IntelliJ IDEA (Database)**: un *data source* PostgreSQL con `127.0.0.1`, el puerto y las credenciales del `.env`. Sirve **solo para consultar** (`SELECT`); el esquema y los datos de ejemplo los aplica siempre Flyway.
