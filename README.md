# Mis recordatorios

Aplicación Android de Lucas Agustin Aguirre Gonzalez para Programación Mobile. En la actividad 3 los recordatorios se guardan en el teléfono. Se pueden crear, consultar, editar y eliminar sin conexión. En Ajustes se eligen el tema, el orden y la visibilidad de los completados.

## Ejecutar en el emulador

1. Descomprimir el ZIP y localizar la carpeta que contiene `settings.gradle.kts`. Se necesita Android Studio con Android SDK Platform 37, un emulador iniciado y Python 3. Para crear el emulador desde Android Studio, abrir **Tools > Device Manager**. La prueba se hizo con un Medium Phone con Android 17.
2. Abrir una terminal en la carpeta descomprimida y ejecutar el servidor. Usa SQLite, incluido en Python. Dejar la terminal abierta. Al visitar `http://127.0.0.1:8765/recordatorios` en la PC, una base nueva devuelve `[]`.

   ```powershell
   python servidor/servidor.py
   ```

3. Instalar y abrir la app por una de estas vías.

   **Android Studio.** Abrir la carpeta que contiene `settings.gradle.kts`. Al abrir por primera vez una copia descargada, el IDE puede mostrar **Trust Project**. Revisar que la ruta corresponda al proyecto extraído. Esperar la sincronización de Gradle, seleccionar el emulador y ejecutar el módulo `app` con ▶. Para Gradle se usó el entorno Java incluido con el IDE.

   **PowerShell en Windows.** Abrir una segunda terminal en la misma carpeta. Estos comandos usan las rutas habituales de Android Studio y del SDK. Si el SDK se instaló en otra carpeta, usar la que figura en **SDK Manager > Android SDK Location**.

   ```powershell
   $env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
   $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
   $env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
   .\gradlew.bat assembleDebug installDebug
   adb shell am start -n com.example.primeraclaseprogramacionmobile/.MainActivity
   ```

4. En Android 17, tocar **Autorizar** y aceptar el permiso para dispositivos cercanos. Si después aparece **Sin conexión con el servidor**, dejar el servidor encendido y tocar **Reintentar**. El listado muestra **Cambios sincronizados** cuando el teléfono se comunica con la PC. En una primera instalación el listado está vacío.

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. La primera compilación descarga Gradle y las dependencias. En macOS y Linux usar `./gradlew`.

El emulador accede a la PC en `http://10.0.2.2:8765/`. El archivo `gradle.properties.example` muestra la propiedad para cambiar la dirección. Para probar otro puerto, arrancar el servidor con `--puerto 8766` y compilar con `"-PrecordatoriosUrl=http://10.0.2.2:8766/"`. En un dispositivo físico se puede usar `adb reverse tcp:8765 tcp:8765` y `"-PrecordatoriosUrl=http://127.0.0.1:8765/"`. Se admite HTTPS para otros servidores. El servidor de práctica escucha solo en la PC y conserva los datos en `recordatorios.sqlite3`; el parámetro `--archivo` permite elegir otra base.

## Datos y sincronización

- **Room** guarda título, descripción, categoría, prioridad, estado, fecha de creación e ID. El listado y el detalle observan las consultas mediante Flow. Los filtros por estado y prioridad pueden combinarse.
- **Preferences DataStore** guarda tres ajustes. Al instalar se usa el tema oscuro, se muestran los completados y el orden comienza por el más reciente. Un orden desconocido usa ese mismo criterio.
- **RepositorioLocal** escribe primero en Room. Una alta o edición queda pendiente hasta que el servidor confirma el mismo ID. Al eliminar, el registro desaparece del listado y se conserva una marca de baja hasta recibir la confirmación.
- **Retrofit y OkHttp** envían `PUT /recordatorios/{id}` o `DELETE /recordatorios/{id}`. Después, `GET /recordatorios` devuelve la colección completa. Los datos válidos se incorporan a Room sin pisar cambios locales pendientes. Una revisión por fila protege las ediciones que ocurren mientras se envía una versión anterior.
- Un solo proceso de sincronización atiende el inicio de la app, los cambios locales, la recuperación de la red y los reintentos manuales. Si el servidor recibió una escritura pero se perdió la respuesta, el reintento usa el mismo ID. La cola sobrevive al cierre de la aplicación. La sincronización se reanuda al abrirla o al recuperar conexión mientras está en ejecución.

## Comprobar el recorrido

1. Con un servidor de prueba propio, detener el servidor con Ctrl+C, crear un recordatorio y abrir su detalle. Aparece **Pendiente de sincronizar**. Se puede editar o eliminar sin red.
2. Cambiar los tres ajustes y cerrar por completo la app. En el emulador se puede usar `adb shell am force-stop com.example.primeraclaseprogramacionmobile`. Volver a abrirla y comprobar el listado y los ajustes.
3. Iniciar el servidor de nuevo con el mismo archivo de datos. Tocar **Reintentar** si la red del emulador siguió activa. Al recuperar conectividad con la app en ejecución se reintenta automáticamente. Comprobar **Sincronizado** en el detalle y consultar `http://127.0.0.1:8765/recordatorios` en la PC.
4. Ejecutar las verificaciones con el emulador conectado:

   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
   ```

Las pruebas instrumentadas usan una base aislada y un servidor HTTP controlado. Ejercitan el recorrido de las pantallas, el CRUD, la respuesta perdida después de una escritura, una edición durante el envío, códigos 400 y 503, tiempo de espera agotado y el rechazo de una respuesta inválida.

[Consigna de API3](https://teclab.instructure.com/courses/3864/files/1354757?wrap=1) | [Versión de API2](https://github.com/luquibu/teclab-programacion-mobile/tree/3c429498c31cf4d3b3806a9e4c34498c61e000cc)
