package com.guitarchords.app.tuner

import androidx.annotation.StringRes
import com.guitarchords.app.R
import com.guitarchords.app.chords.Instrument
import kotlin.math.pow

/** Una cuerda del afinador: cómo se llama y a qué frecuencia tiene que sonar. */
data class StringTarget(val name: String, val freq: Float)

/**
 * Una afinación completa, de la cuerda más grave del mástil a la más aguda.
 *
 * Las frecuencias se CALCULAN a partir del nombre de la nota (La4 = 440 Hz) en
 * vez de escribirse a mano: una tabla de decimales copiados es justo donde se
 * cuela el error que hace que el afinador diga que estás afinado cuando no lo
 * estás.
 */
data class Tuning(
    val id: String,
    val instrument: Instrument,
    @StringRes val labelRes: Int,
    /** Notas con octava, notación anglosajona: "E2", "A#3", "Db4"… */
    val notes: List<String>
) {
    val targets: List<StringTarget> by lazy {
        notes.map { StringTarget(it, Tunings.frecuenciaDe(it)) }
    }
}

object Tunings {

    private val SEMITONOS = mapOf(
        "C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11
    )

    /** Número MIDI de una nota escrita "C#3" / "Eb2"; -1 si no se entiende. */
    fun midiDe(nota: String): Int {
        val limpio = nota.trim()
        if (limpio.length < 2) return -1
        val base = SEMITONOS[limpio.substring(0, 1).uppercase()] ?: return -1
        var i = 1
        var alteracion = 0
        while (i < limpio.length && (limpio[i] == '#' || limpio[i] == 'b')) {
            alteracion += if (limpio[i] == '#') 1 else -1
            i++
        }
        val octava = limpio.substring(i).toIntOrNull() ?: return -1
        // MIDI 60 = C4, y de ahí para arriba y para abajo.
        return (octava + 1) * 12 + base + alteracion
    }

    /** La4 = 440 Hz y el resto por temperamento igual. */
    fun frecuenciaDe(nota: String): Float {
        val midi = midiDe(nota)
        if (midi < 0) return 0f
        return (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }

    /*
     * El catálogo. La estándar de cada instrumento va PRIMERA: es la que se usa
     * el 95 % de las veces y la que se ofrece por defecto.
     */
    val ALL: List<Tuning> = listOf(
        Tuning("guitar_standard", Instrument.GUITAR, R.string.tuning_standard,
            listOf("E2", "A2", "D3", "G3", "B3", "E4")),
        Tuning("guitar_drop_d", Instrument.GUITAR, R.string.tuning_drop_d,
            listOf("D2", "A2", "D3", "G3", "B3", "E4")),
        Tuning("guitar_half_down", Instrument.GUITAR, R.string.tuning_half_step_down,
            listOf("D#2", "G#2", "C#3", "F#3", "A#3", "D#4")),
        Tuning("guitar_full_down", Instrument.GUITAR, R.string.tuning_full_step_down,
            listOf("D2", "G2", "C3", "F3", "A3", "D4")),
        Tuning("guitar_drop_c", Instrument.GUITAR, R.string.tuning_drop_c,
            listOf("C2", "G2", "C3", "F3", "A3", "D4")),
        Tuning("guitar_dadgad", Instrument.GUITAR, R.string.tuning_dadgad,
            listOf("D2", "A2", "D3", "G3", "A3", "D4")),
        Tuning("guitar_open_g", Instrument.GUITAR, R.string.tuning_open_g,
            listOf("D2", "G2", "D3", "G3", "B3", "D4")),
        Tuning("guitar_open_d", Instrument.GUITAR, R.string.tuning_open_d,
            listOf("D2", "A2", "D3", "F#3", "A3", "D4")),
        Tuning("guitar_open_e", Instrument.GUITAR, R.string.tuning_open_e,
            listOf("E2", "B2", "E3", "G#3", "B3", "E4")),

        /*
         * Ukelele. La estándar en do es REENTRANTE: la cuarta cuerda (sol) suena
         * más aguda que la tercera (do). Va escrita tal cual —G4 primero— porque
         * el afinador tiene que buscar la nota de la cuerda que se está tocando,
         * no la más grave del conjunto.
         */
        Tuning("uke_standard", Instrument.UKULELE, R.string.tuning_uke_standard,
            listOf("G4", "C4", "E4", "A4")),
        Tuning("uke_low_g", Instrument.UKULELE, R.string.tuning_uke_low_g,
            listOf("G3", "C4", "E4", "A4")),
        Tuning("uke_baritone", Instrument.UKULELE, R.string.tuning_uke_baritone,
            listOf("D3", "G3", "B3", "E4")),
        Tuning("uke_d", Instrument.UKULELE, R.string.tuning_uke_d,
            listOf("A4", "D4", "F#4", "B4"))
    )

    fun forInstrument(instrument: Instrument): List<Tuning> =
        ALL.filter { it.instrument == instrument }

    fun byId(id: String?): Tuning? = ALL.firstOrNull { it.id == id }

    fun defaultFor(instrument: Instrument): Tuning =
        forInstrument(instrument).firstOrNull() ?: ALL.first()

    /**
     * La cuerda de [tuning] a la que más se parece [freq], comparando en
     * SEMITONOS y no en hercios: en hercios, la distancia entre dos cuerdas
     * graves es minúscula frente a la de dos agudas, y el afinador se iba
     * siempre a las agudas.
     */
    fun nearest(tuning: Tuning, freq: Float): StringTarget? {
        if (freq <= 0f) return null
        return tuning.targets.minByOrNull {
            kotlin.math.abs(kotlin.math.ln(freq / it.freq))
        }
    }

    /** Desafinación en cents de [freq] respecto de [target]. */
    fun cents(freq: Float, target: StringTarget): Float {
        if (freq <= 0f || target.freq <= 0f) return 0f
        return (1200.0 * (kotlin.math.ln(freq / target.freq.toDouble()) / kotlin.math.ln(2.0))).toFloat()
    }
}

/** Afinación elegida en el afinador; se recuerda entre sesiones. */
class TunerPrefs(context: android.content.Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("tuner", android.content.Context.MODE_PRIVATE)

    var tuning: Tuning
        get() = Tunings.byId(sp.getString(KEY, null)) ?: Tunings.defaultFor(Instrument.DEFAULT)
        set(value) { sp.edit().putString(KEY, value.id).apply() }

    private companion object { const val KEY = "tuning_id" }
}
