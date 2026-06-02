package com.guitarchords.app.ui.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.chords.MusicTheory

/**
 * Basic music-theory guide shown inside the chord dictionary:
 * notes, intervals and how each chord quality is built.
 */
@Composable
fun TheoryGuideView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Una pequeña guía para entender de dónde salen los acordes y " +
                "qué significan los sufijos del diccionario.",
            style = MaterialTheme.typography.bodyMedium
        )

        Section("Cifrado: americano y latino") {
            Text(
                "Los acordes usan el cifrado americano (letras). Su equivalencia:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            MusicTheory.LETTER_TO_LATIN.forEach { (letter, latin) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        letter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.width(40.dp)
                    )
                    Text(latin, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "El símbolo # sube un semitono (sostenido) y b lo baja (bemol).",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section("Notas, tono y semitono") {
            Text(
                "Hay 12 notas que se repiten en cada octava:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                MusicTheory.NOTES.joinToString("  "),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "El semitono es la distancia mínima entre dos notas (un traste en " +
                    "la guitarra). Un tono son dos semitonos. Esta sucesión de las 12 " +
                    "notas se llama escala cromática.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Section("Intervalos") {
            Text(
                "Un intervalo es la distancia entre dos notas, medida en semitonos. " +
                    "Es la base para construir acordes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            MusicTheory.INTERVALS.forEach { iv ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        "${iv.semitones}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        iv.short,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.width(48.dp)
                    )
                    Text(iv.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Section("La escala mayor") {
            Text(
                "La escala mayor es la referencia para nombrar los grados. Se forma " +
                    "con el patrón de tonos (T) y semitonos (S):",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "T · T · S · T · T · T · S",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "En Do mayor: Do Re Mi Fa Sol La Si. Sus grados se numeran del 1 al 7; " +
                    "el 1 es la tónica.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Section("Cómo se forma un acorde") {
            Text(
                "Una tríada (acorde básico de 3 notas) se construye apilando terceras " +
                    "desde la tónica: tónica, 3ª y 5ª.\n\n" +
                    "• La 3ª decide si el acorde es mayor (3ª mayor) o menor (3ª menor).\n" +
                    "• La 5ª da estabilidad.\n" +
                    "• Añadiendo más terceras aparecen las séptimas, novenas, etc.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Section("Fórmulas de los acordes") {
            Text(
                "Cada sufijo del diccionario es una fórmula de intervalos. Ejemplo " +
                    "calculado sobre la tónica Do (C):",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            MusicTheory.FORMULAS.forEach { f ->
                FormulaCard(f)
                Spacer(Modifier.height(6.dp))
            }
        }

        Section("Consejo") {
            Text(
                "Usa el círculo de quintas (icono del disco) para ver qué acordes " +
                    "pertenecen a un mismo tono y suenan bien juntos.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormulaCard(f: MusicTheory.ChordFormula) {
    val example = MusicTheory.notesFor(0, f.semitones).joinToString(" · ")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    f.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    "sufijo: ${f.symbol}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Grados: ${f.degrees}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                "Ejemplo (C): $example",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(f.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(6.dp))
    content()
}
