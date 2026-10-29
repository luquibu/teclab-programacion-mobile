# Mis recordatorios

Actividad Práctica Integradora 1 de Programación Mobile, Teclab, bimestre 2B/2026.
Estudiante: Lucas Agustin Aguirre Gonzalez.

Aplicación Android con seis recordatorios académicos ficticios. Permite combinar filtros por prioridad y estado, volver a Todos y consultar un mensaje cuando no hay coincidencias. Los filtros sobreviven a la recreación de la actividad mediante rememberSaveable.

## Abrir y ejecutar

1. Clonar este repositorio o extraer el ZIP adjunto al PDF. Abrir la carpeta que contiene settings.gradle.kts en Android Studio.
2. Instalar Android SDK Platform 37 y las herramientas que solicite la sincronización. El proyecto usa minSdk 24 y targetSdk 37.
3. Configurar el JBR de Android Studio como JVM de Gradle. La configuración del proyecto requiere Java 25. Se verificó con JBR 25.0.3.
4. Dejar que Android Studio cree local.properties con la ruta local del SDK. Ese archivo no se distribuye. Para trabajar por terminal también puede definirse ANDROID_HOME.
5. Iniciar un emulador o conectar un dispositivo Android. Seleccionar app y pulsar Run. La verificación se hizo en el AVD Medium Phone, Android 17, API 37.2, x86_64.

Primera sincronización: necesita acceso a Google Maven, Maven Central y la distribución de Gradle. El ZIP contiene el Wrapper, pero no copia el SDK ni las cachés de dependencias.

En PowerShell, desde la raíz del proyecto, con JAVA_HOME apuntando al JBR y el emulador encendido:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
```

En macOS o Linux, usar ./gradlew con las mismas tareas. El APK de depuración se genera en app/build/outputs/apk/debug/app-debug.apk.

Se conservaron Gradle 9.6.0, Android Gradle Plugin 9.4.1, Kotlin 2.2.10 y Compose BOM 2026.02.01. El applicationId continúa siendo com.example.primeraclaseprogramacionmobile. El nombre visible es Mis recordatorios.

## Correspondencia con API1

| Requisito | Implementación |
| --- | --- |
| Modelos inmutables y alternativas cerradas | model/Recordatorio.kt contiene una data class con propiedades val y sealed classes para categoría, prioridad y estado. |
| Datos opcionales y validación | Descripción nullable. require exige id positivo y al menos cuatro caracteres de título después de quitar espacios externos para la validación. El título recibido se conserva. copy vuelve a validar. |
| List, Set y Map | data/DatosRecordatorios.kt conserva seis elementos en List y obtiene categorías únicas con Set. ui/PantallaRecordatorios.kt relaciona cada categoría con etiqueta y color en Map. |
| Filtrado | filtrarRecordatorios combina prioridad y estado con AND; null significa Todos. Devuelve otra lista y conserva el orden y los datos originales. |
| Pantalla Compose y Material 3 | Tarjetas con título, categoría, prioridad, estado y descripción cuando existe. Lista desplazable, filtros y estado vacío. |
| Ejecución Android | Pruebas instrumentadas en Medium Phone y capturas reales documentadas en el PDF. |

Las rutas anteriores parten de app/src/main/java/com/example/primeraclaseprogramacionmobile. Las pruebas de reglas están en app/src/test y las de interfaz en app/src/androidTest.

## Diseño visual

Adaptación móvil del sistema de diseño de AxForge solicitado por el estudiante. La paleta oscura se centraliza en ui/theme/Color.kt: fondo #0e0f12, tarjetas #16181d, bordes #2a2e38, texto principal #e7e9ee y secundario #9aa0ad. La selección usa azul #3a72e8 al 15% con borde azul. Completado usa verde #10b981, y las categorías usan colores de identidad desaturados.

La separación depende del fondo y los bordes, sin degradados ni sombras en las tarjetas. Se usan espacios de 4, 6, 8, 12 y 16 dp, radios de 4, 6 y 8 dp, peso semibold para títulos y texto secundario atenuado. La adaptación móvil usa texto escalable de 12 a 20 sp y filtros de al menos 48 dp de alto. No incluye selector de tema ni colores dinámicos del dispositivo.

## Verificación del 28/10/2026, Argentina

- assembleDebug: correcto.
- testDebugUnitTest: 10 pruebas correctas. Incluyen identificadores y títulos inválidos, descripción ausente, cada filtro, filtros combinados, lista vacía y conservación de los datos originales.
- connectedDebugAndroidTest: 4 pruebas correctas. Cubren pantalla inicial, desplazamiento al sexto elemento, filtros combinados, ausencia de resultados, vuelta a Todos y recreación de la actividad.
- lintDebug: correcto, sin errores. Conserva 15 avisos del proyecto base sobre dependencias, colores XML sin uso y una etiqueta redundante en el manifiesto. Se mantuvieron las versiones de producción acordadas.

Los reportes se generan en app/build/reports/tests/testDebugUnitTest, app/build/reports/androidTests/connected/debug y app/build/reports/lint-results-debug.html.

Durante las pruebas apareció NoSuchMethodException: InputManager.getInstance en Espresso 3.5.1 al usar Android 37. Se actualizó únicamente espresso-core a 3.7.0 y las cuatro pruebas pasaron. La corrección está documentada en las [notas oficiales de AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0). Gradle también emite un aviso de deprecación de sun.misc.Unsafe desde protobuf del ejecutor de pruebas; no impide su ejecución.

## Alcance y entrega

Los datos son simulados. No hay formularios de alta o edición, persistencia, sincronización ni servicios en la nube. El trabajo corresponde a API1.

El repositorio es privado. El ZIP adjunto al PDF incluye el mismo código publicado, los tests y el Gradle Wrapper, para permitir la revisión sin acceso a GitHub. No contiene compilaciones, cachés, local.properties, configuración personal del IDE ni credenciales.

Fuentes de la actividad: [API1 en Canvas](https://teclab.instructure.com/courses/3864/assignments/42397) y [consigna original](https://teclab.instructure.com/courses/3864/files/1354761?wrap=1).
