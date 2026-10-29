package com.example.primeraclaseprogramacionmobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordatoriosPantallaTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun muestraLosSeisDatosYPermiteLlegarAlUltimo() {
        compose.onNodeWithText("Mis recordatorios").assertIsDisplayed()
        compose.onNodeWithTag("cantidad-resultados").assertTextEquals("6 de 6")
        compose.onNodeWithTag("lista-recordatorios").performScrollToIndex(8)
        compose.onNodeWithTag("recordatorio-6").assertIsDisplayed()
        compose.onNodeWithText("Organizar las evidencias de ejecución").assertIsDisplayed()
    }

    @Test
    fun combinaLosFiltrosYMuestraElRecordatorioEsperado() {
        compose.onNodeWithTag("prioridad-media").performClick()
        compose.onNodeWithTag("estado-completado").performClick()
        compose.onNodeWithTag("cantidad-resultados").assertTextEquals("1 de 6")
        compose.onNodeWithTag("lista-recordatorios").performScrollToIndex(3)
        compose.onNodeWithTag("recordatorio-5").assertIsDisplayed()
        compose.onNodeWithTag("recordatorio-3").assertDoesNotExist()
    }

    @Test
    fun muestraVacioYPermiteVolverATodos() {
        compose.onNodeWithTag("prioridad-alta").performClick()
        compose.onNodeWithTag("estado-completado").performClick()
        compose.onNodeWithTag("cantidad-resultados").assertTextEquals("0 de 6")
        compose.onNodeWithTag("lista-recordatorios").performScrollToIndex(3)
        compose.onNodeWithText("No hay coincidencias").assertIsDisplayed()
        compose.onNodeWithTag("lista-recordatorios").performScrollToIndex(0)
        compose.onNodeWithTag("prioridad-todos").performClick()
        compose.onNodeWithTag("estado-todos").performClick()
        compose.onNodeWithTag("cantidad-resultados").assertTextEquals("6 de 6")
    }

    @Test
    fun conservaAmbosFiltrosAlRecrearLaActividad() {
        compose.onNodeWithTag("prioridad-baja").performClick()
        compose.onNodeWithTag("estado-pendiente").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("prioridad-baja").assertIsSelected()
        compose.onNodeWithTag("estado-pendiente").assertIsSelected()
        compose.onNodeWithTag("cantidad-resultados").assertTextEquals("1 de 6")
        compose.onNodeWithTag("lista-recordatorios").performScrollToIndex(3)
        compose.onNodeWithTag("recordatorio-6").assertIsDisplayed()
    }
}
