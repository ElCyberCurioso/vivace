package com.guitarchords.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Colores de la barra superior: **teal macizo con texto crema**, como en la web.
 *
 * Material 3 la pinta por defecto del color de la superficie, integrada en el
 * fondo. El paquete Accordio la quiere al revés —es el `--ac-nav-bg` del kit, y
 * es lo primero que identifica la app— así que aquí se fija por rol y no a mano:
 * en claro el teal de marca, en oscuro el teal casi negro, y encima siempre el
 * crema. Ver `ExtendedColors.nav`.
 *
 * Va en una función y no repetido en cada pantalla para que cambiarlo sea un
 * sitio, que barras superiores hay dos docenas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun accordioTopBarColors(): TopAppBarColors {
    val ext = MaterialTheme.extendedColors
    return TopAppBarDefaults.topAppBarColors(
        containerColor = ext.nav,
        scrolledContainerColor = ext.nav,
        titleContentColor = ext.onNav,
        navigationIconContentColor = ext.onNav,
        actionIconContentColor = ext.onNav
    )
}
