# Mis recordatorios

Aplicación de Programación Mobile de Lucas Agustin Aguirre Gonzalez. La actividad 2 permite crear recordatorios académicos, consultar su detalle y filtrar el listado por estado y prioridad. Los seis ejemplos iniciales y las altas se guardan en memoria durante la sesión.

## Ejecutar

Abrir la carpeta que contiene `settings.gradle.kts` en Android Studio. Usar el JBR 25 del IDE para Gradle, instalar Android SDK Platform 37 y ejecutar el módulo `app` en un dispositivo con Android API 24 o posterior. Se probó en Medium Phone, Android 17, imagen API 37.2. La primera sincronización descarga las dependencias y el Gradle Wrapper.

En PowerShell, con `JAVA_HOME` apuntando al JBR, `ANDROID_HOME` al SDK y el emulador encendido:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. En Linux y macOS usar `./gradlew`. El proyecto conserva Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01 y el identificador `com.example.primeraclaseprogramacionmobile`.

## Pantallas y datos

| Ruta | Argumento | Acciones |
| --- | --- | --- |
| `listado` | Ninguno | Combinar filtros, abrir un recordatorio o crear uno. |
| `crear` | Ninguno | Validar el borrador, guardar o cancelar. |
| `detalle/{id}` | `id: Long` | Consultar los datos del registro y volver. |

`ui/Navegacion.kt` conecta las acciones de las pantallas con el grafo tipado de Navigation Compose. Guardar, cancelar y volver desde el detalle retiran el destino actual. La entrada del listado conserva sus filtros. Cada pantalla observa el estado inmutable de su ViewModel con `collectAsStateWithLifecycle`. Los ViewModel reciben la interfaz `RepositorioRecordatorios`; la implementación en memoria comparte la colección entre los tres destinos.

`model/Borrador.kt` valida el título recortado, el máximo de 200 caracteres de descripción y la selección de categoría y prioridad. Guardar se habilita al completar los datos válidos. Durante el envío se bloquea un segundo guardado. Un error conserva los campos para reintentar. Los nuevos registros empiezan pendientes.

## Reproducir los estados

Tras instalar la app, ejecutar en PowerShell con `adb` en el PATH:

```powershell
$actividad = 'com.example.primeraclaseprogramacionmobile/.MainActivity'
adb shell am start -S -n $actividad --es escenario vacio
adb shell am start -S -n $actividad --es escenario error
adb shell am start -S -n $actividad --es escenario error_guardado
```

Cada comando inicia una sesión en memoria. Con `vacio` se puede crear el primer recordatorio. Con `error` falla la primera carga y **Reintentar** recupera los datos. Con `error_guardado` el primer guardado falla y el segundo agrega el registro. Para ver los seis ejemplos originales, iniciar con `adb shell am start -S -n $actividad` sin argumentos. La carga y el guardado usan demoras suspendidas para poder observarlos.

Las pruebas de `app/src/test` cubren validaciones, colecciones y filtros. Las de `app/src/androidTest` recorren las pantallas, recrean la actividad y ejercitan los fallos y sus reintentos.

[Consigna de API2](https://teclab.instructure.com/courses/3864/files/1354756?wrap=1) · [Versión de API1](https://github.com/luquibu/teclab-programacion-mobile/tree/b61851c3d9599dd1400fb54b471457263530f325)
