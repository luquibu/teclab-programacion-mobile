# Mis recordatorios

Proyecto de Lucas Agustin Aguirre Gonzalez para la Actividad Práctica Integradora 4 de Programación Mobile. La versión **4.0-rc1** (código 4) guarda los recordatorios en el teléfono y los sincroniza entre dispositivos de la misma cuenta.

## Probar el APK de la entrega

1. Para probar la sincronización, usá dos teléfonos o emuladores con Android 7 o superior y conexión a Internet. Empezá con equipos donde aún no esté instalada la app. Si querés conservar una instalación anterior, podés crear un emulador desde el **Device Manager** de Android Studio.
2. Instalá el APK de la entrega en ambos equipos. Si usás Android SDK Platform Tools, ejecutá `adb devices` para ver sus números de serie. Para cada equipo, ejecutá `adb -s SERIAL install archivo.apk` con su número de serie y la ruta al APK. El AAB se incluye para distribución.
3. En el primer equipo, abrí **Mis recordatorios**, tocá **Crear una cuenta**, escribí un correo que controles y una contraseña de al menos seis caracteres, y tocá **Registrarme**. El listado muestra **Todavía no hay recordatorios**.
4. Tocá **Crear recordatorio**, escribí un título, elegí una categoría y una prioridad y tocá **Guardar**. Cuando el listado indique **Cambios sincronizados**, abrí el recordatorio. En el segundo equipo, ingresá con esa misma cuenta y abrí el recordatorio que aparece en el listado.
5. En el primer equipo, activá el modo avión y comprobá que el wifi también esté apagado. Tocá **Editar**, cambiá el título y guardá. El detalle muestra **Pendiente de sincronizar**. Dejá el detalle abierto con la sesión iniciada y restablecé la conexión. El indicador cambia a **Sincronizado** cuando la app envía la edición.
6. En el segundo equipo, volvé del detalle al listado y tocá **Sincronizar**. Si la sincronización anterior falló, el botón se llama **Reintentar**. Esperá el título nuevo y abrilo para comprobar el cambio.
7. Para probar el cambio de cuenta, volvé al listado y entrá en **Ajustes → Cerrar sesión**. Cada cuenta ve sus propios recordatorios. Si cerrás sesión con una edición pendiente, la app la conserva en ese equipo y la envía cuando volvés a ingresar con la cuenta original y conexión.

La versión firmada usa Firebase Authentication y Cloud Firestore en el proyecto académico `teclab-mis-recordatorios` (Spark, São Paulo). El docente puede usar el APK por Internet sin arrancar servicios en su PC.

## Datos y sesiones

El listado y el detalle de Compose observan el estado de sus ViewModels. El repositorio de la cuenta escribe en Room, envía los cambios a Firestore y actualiza Room después de la confirmación. Los ViewModels leen los recordatorios desde Room. Una transacción de Firestore requiere conexión y usa siempre el mismo ID. Si una edición local cambia durante el envío, su revisión más reciente sigue pendiente. Cuando dos equipos editan el mismo registro, prevalece la última escritura confirmada por el servidor.

Room identifica cada fila por cuenta e ID. La migración desde API3 conserva las filas anteriores sin asignarles propietario. La app no las muestra ni las sube. En Firestore, las reglas de `users/{uid}/reminders/{id}` exigen que el UID coincida con el usuario autenticado. Authentication gestiona las credenciales. DataStore conserva las preferencias de presentación del dispositivo. La app excluye su base y sus datos de las copias de seguridad.

## Abrir y compilar el proyecto

Extraé el ZIP y abrí en Android Studio la carpeta `MisRecordatorios`, que contiene `settings.gradle.kts` y `gradlew.bat`. Instalá Android SDK Platform 37 y sincronizá Gradle. El proyecto usa **Java 25** para el daemon de Gradle. Estos comandos de PowerShell se ejecutan desde `MisRecordatorios` y usan el JBR 25 de Android Studio instalado en su ubicación habitual.

```powershell
$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Para ejecutar la variante debug, instalá Node.js 22 o superior. En la misma terminal, iniciá los dos emuladores de Firebase y dejalos en ejecución.

```powershell
npx.cmd --yes firebase-tools@15.37.0 emulators:start --only 'auth,firestore' --project demo-teclab-api4
```

La consola debe mostrar **Authentication** en el puerto **9099** y **Firestore** en el **8184**. Abrí otra terminal en `MisRecordatorios` y configurá las mismas variables de entorno. Con un emulador Android iniciado, ejecutá `.\gradlew.bat installDebug` para instalar la app. En Android 17, concedé el acceso a la red local cuando lo pida. Si el sistema ya dejó de mostrar el diálogo, abrí **Ajustes de Android → Apps → Mis recordatorios → Permisos** y habilitá **Dispositivos cercanos**.

Con los emuladores de Firebase en ejecución, `python -m unittest discover -s pruebas` verifica las reglas de acceso con dos cuentas de prueba. Este comando requiere Python 3. Para probar la interfaz, el repositorio y la migración en el emulador Android, ejecutá `.\gradlew.bat connectedDebugAndroidTest`.

**Versión firmada.** La configuración pública del cliente está en `app/src/release/google-services.json`. Para compilar con tu propia firma, configurá `TECLAB_STORE_FILE` con la ruta al almacén PKCS12 y `TECLAB_STORE_PASSWORD` con su contraseña en el entorno local. El alias es `teclab-mobile`, con la misma contraseña que el almacén. Ejecutá `.\gradlew.bat --no-configuration-cache assembleRelease bundleRelease`. La clave privada original se conserva fuera del proyecto. Los resultados aparecen en `app/build/outputs/apk/release/` y `app/build/outputs/bundle/release/`.

[Consigna de API4](https://teclab.instructure.com/courses/3864/files/1354762?wrap=1) · [Versión anterior, API3](https://github.com/luquibu/teclab-programacion-mobile/tree/3095150b794239c558a4a7f56be7ca004061ee7f)
