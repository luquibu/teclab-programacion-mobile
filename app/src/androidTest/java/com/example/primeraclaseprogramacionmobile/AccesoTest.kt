package com.example.primeraclaseprogramacionmobile

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeraclaseprogramacionmobile.data.ErrorAcceso
import com.example.primeraclaseprogramacionmobile.data.MotivoAcceso
import com.example.primeraclaseprogramacionmobile.data.Sesiones
import com.example.primeraclaseprogramacionmobile.ui.Acceso
import com.example.primeraclaseprogramacionmobile.ui.AccesoViewModel
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccesoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun limpiaElErrorAlCambiarDeModoOCorregirLasCredenciales() {
        val envios = mutableListOf<Boolean>()
        val sesiones = object : Sesiones by appPrueba.sesiones {
            override suspend fun autenticar(correo: String, clave: String, registrar: Boolean) {
                envios.add(registrar)
                throw ErrorAcceso(if (registrar) MotivoAcceso.CorreoRegistrado else MotivoAcceso.Espera)
            }
        }
        compose.setContent {
            PrimeraClaseProgramacionMobileTheme { Acceso(viewModel { AccesoViewModel(sesiones) }) }
        }
        fun nodo(recurso: Int) = compose.onNodeWithText(appPrueba.getString(recurso))
        val correo = nodo(R.string.auth_email)
        val clave = nodo(R.string.auth_password)
        val espera = nodo(R.string.auth_wait)
        val usado = nodo(R.string.auth_used)

        correo.performClick().performTextReplacement("prueba@example.test")
        correo.performImeAction()
        clave.assertIsFocused().performTextReplacement("clave123")
        clave.performImeAction()
        espera.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        nodo(R.string.auth_create).performScrollTo().performClick()
        espera.assertDoesNotExist()
        correo.assertTextContains("prueba@example.test")

        nodo(R.string.auth_register).performScrollTo().performClick()
        usado.assertExists()
        nodo(R.string.auth_existing).performScrollTo().performClick()
        usado.assertDoesNotExist()
        correo.assertTextContains("prueba@example.test")

        nodo(R.string.auth_enter).performScrollTo().performClick()
        espera.assertExists()
        correo.performTextReplacement("otra@example.test")
        espera.assertDoesNotExist()
        nodo(R.string.auth_enter).performScrollTo().performClick()
        espera.assertExists()
        clave.performTextReplacement("otraClave")
        espera.assertDoesNotExist()
        assertEquals(listOf(false, true, false, false), envios)
    }
}
