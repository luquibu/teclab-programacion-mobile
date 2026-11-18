package com.example.primeraclaseprogramacionmobile

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test

class RecorridoMobileTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentacion get() = InstrumentationRegistry.getInstrumentation()

    private fun iniciar(escenario: String? = null): ActivityScenario<MainActivity> {
        prepararEjemplos(escenario != "vacio")
        appPrueba.fallaGuardado = escenario == "error_guardado"
        val intent = Intent(instrumentacion.targetContext, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return ActivityScenario.launch(intent)
    }

    private fun esperar(matcher: SemanticsMatcher) {
        compose.waitUntil(8_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun captura(nombre: String) {
        if (InstrumentationRegistry.getArguments().getString("evidencias") != "si") return
        compose.waitForIdle()
        instrumentacion.uiAutomation.waitForIdle(350, 4_000)
        val imagen = requireNotNull(instrumentacion.uiAutomation.takeScreenshot())
        val carpeta = File(instrumentacion.targetContext.getExternalFilesDir(null), "evidencias")
        carpeta.mkdirs()
        File(carpeta, "$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        imagen.recycle()
    }

    private fun abrirFormulario() {
        esperar(hasTestTag("crear"))
        compose.onNodeWithTag("crear").performClick()
        esperar(hasTestTag("titulo"))
    }

    private fun completarFormulario() {
        // Introducir por semántica ejercita los campos controlados sin depender del teclado instalado.
        compose.onNodeWithTag("titulo").performTextReplacement("Preparar el parcial")
        compose.onNodeWithTag("descripcion").performTextReplacement("Repasar navegación y estado de pantalla.")
        compose.onNodeWithText("Exámenes").performScrollTo().performClick()
        compose.onNodeWithText("Media").performScrollTo().performClick()
    }

    @Test fun crearConsultarYVolverAlListado() {
        iniciar().use { actividad ->
            esperar(hasTestTag("recordatorio-1"))
            captura("listado")
            abrirFormulario()
            compose.onNodeWithTag("guardar").assertIsNotEnabled()
            compose.onNodeWithTag("titulo").performTextReplacement("abc")
            compose.onNodeWithText("Escribí al menos cuatro caracteres.").assertExists()
            completarFormulario()
            actividad.recreate()
            esperar(hasTestTag("guardar") and isEnabled())
            captura("formulario")
            compose.onNodeWithTag("guardar").performScrollTo().performClick()
            compose.onNodeWithTag("guardar").assertIsNotEnabled()
            esperar(hasText("7 de 7"))
            compose.onNodeWithTag("cantidad-resultados").assertTextEquals("7 de 7")
            compose.onNodeWithText("Preparar el parcial").performClick()
            esperar(hasText("Pendiente de sincronizar"))
            compose.onNodeWithText("Preparar el parcial").assertIsDisplayed()
            compose.onNodeWithText("Exámenes").assertIsDisplayed()
            captura("detalle")
            compose.onNodeWithContentDescription("Volver").performClick()
            esperar(hasTestTag("cantidad-resultados"))
            compose.onNodeWithTag("cantidad-resultados").assertTextEquals("7 de 7")
            actividad.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(5_000) { actividad.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        }
    }

    @Test fun cancelarConservaListadoYFiltros() {
        iniciar().use {
            esperar(hasTestTag("prioridad-alta"))
            compose.onNodeWithTag("prioridad-alta").performClick()
            abrirFormulario()
            compose.onNodeWithTag("titulo").performTextReplacement("Descartar este borrador")
            compose.onNodeWithText("Cancelar").performScrollTo().performClick()
            esperar(hasTestTag("cantidad-resultados"))
            compose.onNodeWithTag("cantidad-resultados").assertTextEquals("2 de 6")
            compose.onNodeWithTag("prioridad-alta").assertIsSelected()
        }
    }

    @Test fun crearDesdeElListadoVacio() {
        iniciar("vacio").use {
            esperar(hasText("Todavía no hay recordatorios"))
            captura("vacio")
            compose.onNodeWithText("Crear recordatorio").performClick()
            completarFormulario()
            compose.onNodeWithTag("guardar").performScrollTo().performClick()
            esperar(hasTestTag("cantidad-resultados"))
            compose.onNodeWithTag("cantidad-resultados").assertTextEquals("1 de 1")
        }
    }

    @Test fun editarCompletarYEliminarUnRecordatorio() {
        iniciar().use {
            esperar(hasTestTag("recordatorio-1"))
            compose.onNodeWithTag("recordatorio-1").performClick()
            esperar(hasText("Editar"))
            compose.onNodeWithText("Editar").performClick()
            esperar(hasTestTag("titulo") and isEnabled())
            compose.onNodeWithTag("titulo").performTextReplacement("Repasar Room")
            compose.onNodeWithText("Completado").performScrollTo().performClick()
            compose.onNodeWithTag("guardar").performScrollTo().performClick()
            esperar(hasText("Detalle del recordatorio"))
            compose.onNodeWithText("Repasar Room").assertExists()
            compose.onNodeWithText("Completado").assertExists()
            compose.onNodeWithText("Eliminar").performClick()
            compose.onNodeWithText("¿Eliminar este recordatorio?").assertExists()
            compose.onAllNodesWithText("Eliminar").onLast().performClick()
            esperar(hasText("5 de 5"))
        }
    }

    @Test fun errorDeGuardadoConservaCamposYPermiteReintentar() {
        iniciar("error_guardado").use {
            abrirFormulario()
            completarFormulario()
            compose.onNodeWithTag("guardar").performScrollTo().performClick()
            esperar(hasText("No se pudo guardar.", substring = true))
            compose.onNodeWithTag("titulo").assertTextContains("Preparar el parcial")
            compose.onNodeWithTag("guardar").performScrollTo().performClick()
            esperar(hasTestTag("cantidad-resultados"))
            compose.onNodeWithTag("cantidad-resultados").assertTextEquals("7 de 7")
        }
    }
}
