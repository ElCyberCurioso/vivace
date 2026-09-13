package com.guitarchords.app.chords

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Instrumento de cuerda para el que se dibujan acordes.
 *
 * Un acorde no es el mismo objeto en guitarra que en ukelele: cambia el número
 * de cuerdas, la afinación al aire y, por tanto, la digitación entera. Por eso
 * el instrumento es un dato de primera clase y no un ajuste de pintado: de él
 * dependen el diccionario que se lee, las notas que suenan y el diagrama.
 *
 * [openPitchClasses] y [openMidi] van de la cuerda MÁS GRAVE que se dibuja a la
 * izquierda a la más aguda, que es el orden en que llegan los trastes en
 * [ChordShape.frets] y el que usa chords-db.
 *
 * Nota sobre el ukelele: la afinación estándar (sol-do-mi-la) es *reentrante* —
 * la cuarta cuerda suena MÁS AGUDA que la tercera—. Aquí se respeta tal cual:
 * el orden es el del mástil, no el de la altura.
 */
enum class Instrument(
    val id: String,
    val strings: Int,
    val openPitchClasses: IntArray,
    val openMidi: IntArray,
    val openNames: List<String>,
    /** Diccionario empaquetado, si lo hay. El de ukelele aún no viene con la app. */
    val asset: String
) {
    GUITAR(
        id = "guitarra",
        strings = 6,
        openPitchClasses = intArrayOf(4, 9, 2, 7, 11, 4),
        openMidi = intArrayOf(40, 45, 50, 55, 59, 64),
        openNames = listOf("Mi", "La", "Re", "Sol", "Si", "Mi"),
        asset = "chords/guitar.json"
    ),
    UKULELE(
        id = "ukelele",
        strings = 4,
        openPitchClasses = intArrayOf(7, 0, 4, 9),
        openMidi = intArrayOf(67, 60, 64, 69),
        openNames = listOf("Sol", "Do", "Mi", "La"),
        asset = "chords/ukulele.json"
    );

    companion object {
        val DEFAULT = GUITAR

        fun byId(id: String?): Instrument = entries.firstOrNull { it.id == id } ?: DEFAULT

        /**
         * A qué instrumento pertenece una digitación, por el número de trastes
         * que trae. Es lo que permite que un diagrama suelto —el de un ejercicio,
         * el de una tarjeta— se pinte bien sin que haya que arrastrar el
         * instrumento por media aplicación.
         */
        fun forShape(shape: ChordShape): Instrument = forStringCount(shape.frets.size)

        fun forStringCount(n: Int): Instrument = entries.firstOrNull { it.strings == n } ?: DEFAULT
    }
}

/**
 * Instrumento que se está mirando. Es una preferencia de quien toca, no de la
 * partitura, así que vive en el aparato y no en los datos (lo mismo hace la web
 * con `accordio_instrument`).
 */
object InstrumentPrefs {

    private const val PREFS = "instrument"
    private const val KEY = "current"

    private var sp: android.content.SharedPreferences? = null

    private val _current = MutableStateFlow(Instrument.DEFAULT)
    val current: StateFlow<Instrument> = _current

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp = prefs
        _current.value = Instrument.byId(prefs.getString(KEY, null))
    }

    fun set(instrument: Instrument) {
        _current.value = instrument
        sp?.edit()?.putString(KEY, instrument.id)?.apply()
    }
}
