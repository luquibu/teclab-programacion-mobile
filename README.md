# Mis recordatorios

Proyecto de Lucas Agustin Aguirre Gonzalez para la Actividad Práctica Integradora 4 de Programación Mobile. La versión **4.0-rc1** (código 4) guarda los recordatorios en el teléfono y los sincroniza entre dispositivos de la misma cuenta.

## Probar el APK de la entrega

1. Usá un teléfono o emulador con Android 7 o superior y conexión a Internet. Si ya tenés instalada una compilación de desarrollo con otra firma, elegí otro emulador para conservar esa instalación.
2. Instalá el APK adjunto a Canvas. Desde una PC con Android SDK Platform Tools también se puede usar `adb -s SERIAL install archivo.apk`. El archivo AAB se entrega para distribución. Para esta prueba instalá el APK.
3. Abrí **Mis recordatorios → Crear una cuenta**. Registrá un correo que controles y una contraseña de al menos seis caracteres. Para una cuenta que ya existe, elegí **Ingresar**.
4. Tocá **Crear recordatorio**, completá título, categoría y prioridad y guardá. Esperá **Cambios sincronizados**. Instalá el mismo APK en un segundo dispositivo, ingresá con la misma cuenta y abrí el recordatorio.
5. En el primer dispositivo, activá el modo avión, editá el título y guardá. El detalle indica que el cambio está pendiente. Podés cerrar la app y volver a abrirla. Al recuperar la red, la app envía el cambio al mismo documento. En el segundo dispositivo, tocá **Sincronizar** para actualizar su copia.
6. En **Ajustes → Cerrar sesión** podés cambiar de cuenta. Un usuario nuevo empieza con su propio listado. Si la cuenta anterior dejó cambios pendientes, se conservan en el dispositivo y se envían cuando esa cuenta vuelve a ingresar con conexión.

La versión firmada usa Firebase Authentication y Cloud Firestore en el proyecto académico `teclab-mis-recordatorios` (Spark, São Paulo). El docente puede usar el APK por Internet sin arrancar servicios en su PC.

## Datos y sesiones

La pantalla observa un ViewModel. El repositorio de la cuenta escribe en Room, envía los cambios a Firestore y actualiza Room después de la confirmación. El listado y el detalle leen Room. Una transacción de Firestore requiere conexión y usa siempre el mismo ID. Si una edición local cambia durante el envío, su revisión más reciente sigue pendiente. Cuando dos equipos editan el mismo registro, prevalece la última escritura confirmada por el servidor.

Room identifica cada fila por cuenta e ID. La migración desde API3 conserva las filas anteriores sin asignarles propietario. La app no las muestra ni las sube. En Firestore, las reglas de `users/{uid}/reminders/{id}` exigen que el UID coincida con el usuario autenticado. Authentication gestiona las credenciales. DataStore conserva las preferencias de presentación del dispositivo. La app excluye su base y sus datos de las copias de seguridad.

## Abrir y compilar el proyecto

Extraé el ZIP, abrí en Android Studio la carpeta con `settings.gradle.kts` y sincronizá Gradle. Se necesita SDK Platform 37 y el JBR incluido en Android Studio. Para compilar desde PowerShell en Windows:

```powershell
$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

**Desarrollo local.** Usá Node 22 o superior y el JBR del IDE. En la raíz arrancá `npx --yes firebase-tools@15.37.0 emulators:start --only auth,firestore --project demo-teclab-api4` y dejá esa terminal abierta. La variante debug se comunica desde el emulador Android con Authentication (9099) y Firestore (8184) de la PC. En Android 17 concedé el acceso a la red local cuando lo pida. Para instalar en un emulador, usá `.\gradlew.bat installDebug`. En otra terminal, `python -m unittest discover -s pruebas` verifica las reglas con dos cuentas del emulador. Con un emulador Android iniciado, `.\gradlew.bat connectedDebugAndroidTest` ejecuta las pruebas de interfaz, repositorio y migración.

**Versión firmada.** La configuración pública del cliente está en `app/src/release/google-services.json`. Para compilar con tu propia firma, configurá `TECLAB_STORE_FILE` con la ruta al almacén PKCS12 y `TECLAB_STORE_PASSWORD` con su contraseña en el entorno local. El alias es `teclab-mobile`, con la misma contraseña que el almacén. Ejecutá `.\gradlew.bat --no-configuration-cache assembleRelease bundleRelease`. La clave privada original se conserva fuera del proyecto. Los resultados aparecen en `app/build/outputs/apk/release/` y `app/build/outputs/bundle/release/`.

[Consigna de API4](https://teclab.instructure.com/courses/3864/files/1354762?wrap=1) · [Versión anterior, API3](https://github.com/luquibu/teclab-programacion-mobile/tree/3095150b794239c558a4a7f56be7ca004061ee7f)
