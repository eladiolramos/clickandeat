# CLICK&EAT
Aplicación desarrollada para establecimientos del sector de la restauración, más concretamente para el Delivery y el Take Away. Con ella se podrá digitalizar los procesos de gestión del establecimiento, así como el proceso de captura y gestión de pedidos por parte del cliente.

## Stack seleccionado

### Bases de datos
- PostgreSQL

### Backend
- Java como lenguaje.
- Spring Boot como framework.
- Spring Security para la capa de seguridad.
- Spring Data JPA / Hibernate para la persistencia.
- Maven como gestor de dependencias.
- JUnit 5 y Mockito para el control de pruebas.

### Frontend
- TypeScript como lenguaje.
- Angular como framework.

### Infraestructura y Herramientas
- Docker para la contenedorización y despliegue de entornos.
- Git como sistema de control de versiones.
- GitHub como repositorio en la nube y revisión de código.

## Estado del Proyecto
La aplicación se encuentra terminada como un "MVP" en modo local, pero no ha sido subida a GitHub. El objetivo actual es rehacerla y refactorizarla para que sea más robusta, escalable y funcional. Por ello, se irán subiendo los cambios de forma progresiva aplicando una nueva reestructuración.

## Puesta en marcha rápida

### Requisitos Previos
Para desplegar y ejecutar la aplicación localmente se necesita:
- Tener Git para poder clonar el repositorio.
- Docker compose en su versión V2.0 o superior para poder empaquetar la base de datos mediante contenedor.

### Despliegue de la Infraestructura
1. **Clonar el repositorio y navegar hasta la carpeta "infra".**
```Bash
git clone <https://github.com/eladiolramos/clickandeat>
cd infra
```
2. **Configurar las variables de entorno,** copiando la plantilla .env.example para crear el .env local privado.
```Bash
cp .env.example .env
```
3. **Arrancar el servicio**
```Bash
docker compose up -d
```
4. **Verificar el estado el contenedor**
```Bash
docker compose ps
```
5. **Ver los registros (logs) en tiempo real,** para comprobar que no hay errores y todo funciona a la perfección.
```Bash
docker compose logs -f
```

### Comando varios posiblemente necesarios.
1. **Para detener la infrastructura.**
```Bash
docker compose down
```
2. **Para destruir el entorno y eliminar los datos de la base de datos.**
```Bash
docker compose down -v
```
3. **Pausar o parar el contenedor.** Detiene el contenedor en ejecución sin borrar ni alterar nada
```Bash
docker compose stop
```
4. **Arrancar el contenedor despues de parar con stop.**
```Bash
docker compose start
```

### Resolución de posibles problemas.
- El puerto 5432 ya está ocupado en tu equipo: Cambia la variable DB_HOST_PORT en tu archivo .env local (por ejemplo DB_HOST_PORT=5433) y vuelve a ejecutar docker compose up -d.

- Las credenciales de PostgreSQL no cambian tras modificar el .env: PostgreSQL solo lee las credenciales durante la primera inicialización del volumen. Para aplicar nuevos cambios de usuario o contraseña, destruye el volumen con docker compose down -v y vuelve a ejecutar docker compose up -d.

