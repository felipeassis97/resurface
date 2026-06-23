package com.resurface.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Internal design QA screen: every color swatch + the full type scale, both schemes.
 * Use the two @Preview functions below to eyeball tokens (esp. thin-serif-on-dark, task 7.2).
 */
@Composable
private fun Swatch(name: String, fill: Color, on: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(fill, RoundedCornerShape(8.dp))
            .padding(Spacing.space3),
    ) {
        Text(name, color = on, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ThemeShowcase() {
    val c = MaterialTheme.colorScheme
    val ext = resurfaceColors
    Surface(color = c.surface) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space2),
        ) {
            Text("Resurface", style = MaterialTheme.typography.displaySmall, color = c.onSurface)
            Text("surfacing for air", style = MaterialTheme.typography.headlineSmall, color = c.onSurfaceVariant)

            // Color roles
            Swatch("primary", c.primary, c.onPrimary)
            Swatch("primary container", c.primaryContainer, c.onPrimaryContainer)
            Swatch("secondary", c.secondary, c.onSecondary)
            Swatch("secondary container", c.secondaryContainer, c.onSecondaryContainer)
            Swatch("tertiary · nudge (amber)", c.tertiary, c.onTertiary)
            Swatch("tertiary container", c.tertiaryContainer, c.onTertiaryContainer)
            Swatch("success · focus kept", ext.success, ext.onSuccess)
            Swatch("success container", ext.successContainer, ext.onSuccessContainer)
            Swatch("error · faults only", c.error, c.onError)
            Swatch("surface container", c.surfaceContainer, c.onSurface)

            // Type scale
            Text("Display L", style = MaterialTheme.typography.displayLarge, color = c.onSurface)
            Text("Headline L", style = MaterialTheme.typography.headlineLarge, color = c.onSurface)
            Text("Title L", style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            Text("Body L — calm reading text", style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
            Text("LABEL L", style = MaterialTheme.typography.labelLarge, color = c.onSurface)
            Text("22 min", style = ResurfaceTextStyles.statDisplay, color = c.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Box(Modifier.size(40.dp).background(c.outline, ResurfaceShapes.full))
                Box(Modifier.size(40.dp).background(c.outlineVariant, ResurfaceShapes.full))
            }
        }
    }
}

@Preview(name = "Tokens · Light", showBackground = true)
@Composable
private fun ThemeShowcaseLight() {
    ResurfaceTheme(darkTheme = false) { ThemeShowcase() }
}

@Preview(name = "Tokens · Dark", showBackground = true)
@Composable
private fun ThemeShowcaseDark() {
    ResurfaceTheme(darkTheme = true) { ThemeShowcase() }
}
