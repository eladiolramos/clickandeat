# Click&Eat · Backend

API REST de **Click&Eat**, la plataforma de pedidos de comida a domicilio y para recoger.

Este documento explica **cómo arrancar, configurar y probar** el backend en local, y **por qué** está montado así.

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


---

## 1. Stack y versiones

| Pieza | Versión | Para qué |
|---|---|---|
| Java | 25 (LTS) | Lenguaje |
| Spring Boot | 4.1.2 | Framework (Spring Framework 7) |
| Maven | Maven Wrapper (`./mvnw`) | Construcción y dependencias. No hace falta instalar Maven |
| PostgreSQL | 18 | Base de datos, en Docker Compose (`infra/`) |
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
    │       └── application-dev.yaml             ← configuración solo de DESARROLLO
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
| `application-dev.yaml` | Solo con el perfil **`dev`** | Importa `infra/.env` y muestra los detalles de salud |

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
| `spring.jpa.hibernate.ddl-auto` | `none` | El esquema lo gestionará Flyway (paso 1.2). Hibernate nunca modifica tablas |
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

### JPA: `ddl-auto: none`, `open-in-view: false` y UTC
El esquema será responsabilidad de migraciones versionadas (Flyway), nunca de Hibernate. OSIV se desactiva para no retener conexiones durante toda la petición ni ocultar consultas. Las fechas se guardan en UTC para que no dependan de la zona horaria del servidor.

### Actuator con exposición mínima
Solo se exponen `health` e `info`; endpoints como `env`, `beans` o `heapdump` revelarían configuración o memoria interna. Los detalles de salud solo se muestran en `dev`. Las probes de liveness y readiness se activan pensando en el despliegue (Corte 5); la base de datos no debe afectar a la liveness, porque reiniciar el backend no la recupera.

### Tests contra PostgreSQL real con Testcontainers
H2 no se comporta como PostgreSQL. Los tests usan la **misma imagen** que Compose (`postgres:18`, fijada en lugar de `latest`) para probar exactamente lo mismo que se usa en desarrollo. Los tests de integración (`*IT`) se separan con Failsafe para que `./mvnw test` sea rápido y `./mvnw verify` lo ejecute todo.

## 11. Enlaces 

