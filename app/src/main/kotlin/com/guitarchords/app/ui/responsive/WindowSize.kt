package com.guitarchords.app.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Coarse width buckets used to adapt layouts to phones, foldables and tablets.
 *
 * Breakpoints follow the Material window-size-class guidelines:
 *   COMPACT  <  600dp  (most phones in portrait)
 *   MEDIUM   <  840dp  (small tablets, large phones in landscape)
 *   EXPANDED >= 840dp  (tablets, foldables in landscape)
 */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberWidthClass(): WidthClass {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w < 600 -> WidthClass.COMPACT
        w < 840 -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
}
