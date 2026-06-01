# TODO - Refactor MVC "La P'tite France"

## Paso 1 - Estructura MVC
- [x] Identificar acoplamientos actuales en `LoginView` y `DashboardAsistenteView`.
- [ ] Crear paquetes/estructura sugerida (controllers + exceptions).

## Paso 2 - Excepciones manejables
- [ ] Crear `exceptions/ValidationException.java`
- [ ] Crear `exceptions/NotFoundException.java`
- [ ] Crear `exceptions/DuplicateException.java`

## Paso 3 - Controllers nuevos
- [ ] Crear `controllers/LoginController.java` (verifica empleado por código)
- [ ] Crear `controllers/ClienteController.java` (buscar/registrar cliente con validaciones)

## Paso 4 - Limpiar Views
- [x] Refactorizar `views/LoginView.java` para que use `LoginController` y elimine repositorio
- [x] Refactorizar `views/DashboardAsistenteView.java` para que use `ClienteController` y elimine validaciones/repositorios (pendiente implementación en esta iteración)



## Paso 5 - Verificación
- [ ] Ejecutar `mvn test` o `mvn -q test` para verificar compilación
- [ ] Ejecutar la app y comprobar flujo Login -> Dashboard

