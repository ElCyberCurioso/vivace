package com.guitarchords.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guitarchords.app.training.TrainingArea

@Database(
    entities = [
        Playlist::class, Song::class, SongVersion::class, CustomChord::class,
        PendingDelete::class,
        TrainingProfile::class, AreaProgress::class, ExerciseResult::class,
        AchievementUnlock::class
    ],
    version = 17,
    // El esquema se exporta a app/schemas/ (ver ksp {} en build.gradle.kts).
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun songVersionDao(): SongVersionDao
    abstract fun customChordDao(): CustomChordDao
    abstract fun pendingDeleteDao(): PendingDeleteDao
    abstract fun trainingDao(): TrainingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE songs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        playlist_id INTEGER,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        genre TEXT NOT NULL,
                        content TEXT NOT NULL,
                        favorite INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(playlist_id) REFERENCES playlists(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO songs_new (id, playlist_id, title, artist, genre, content, favorite, position, updated_at)
                    SELECT id, playlist_id, title, artist, genre, content, favorite, position, updated_at FROM songs
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE songs")
                db.execSQL("ALTER TABLE songs_new RENAME TO songs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_playlist_id ON songs(playlist_id)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN capo INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Convert chord brackets [X] to {X} in existing song content.
                // Tab markers {tab}/{/tab} unaffected (already braces).
                db.execSQL("UPDATE songs SET content = REPLACE(REPLACE(content, '[', '{'), ']', '}')")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN remote_key TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN remote_etag TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN remote_updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS song_versions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        song_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        content TEXT NOT NULL,
                        capo INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_song_versions_song_id ON song_versions(song_id)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE song_versions ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_chords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chord_key TEXT NOT NULL,
                        frets TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_chords_chord_key ON custom_chords(chord_key)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        xp_total INTEGER NOT NULL,
                        streak_current INTEGER NOT NULL,
                        streak_best INTEGER NOT NULL,
                        last_practice_day INTEGER NOT NULL,
                        placement_done INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS area_progress (
                        area TEXT PRIMARY KEY NOT NULL,
                        unlocked_level INTEGER NOT NULL,
                        xp_area INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exercise_id TEXT NOT NULL,
                        area TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        passed INTEGER NOT NULL,
                        xp_earned INTEGER NOT NULL,
                        duration_ms INTEGER NOT NULL,
                        details_json TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercise_results_exercise_id ON exercise_results(exercise_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercise_results_timestamp ON exercise_results(timestamp)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievements (
                        achievement_id TEXT PRIMARY KEY NOT NULL,
                        unlocked_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedTraining(db)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                // No había fecha de creación: se aproxima con la última modificación
                // de cada canción ya existente (lo más cercano disponible).
                db.execSQL("UPDATE songs SET created_at = updated_at")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Acordes personalizados sincronizables: id estable, tombstone y
                // marca de cambios locales. Las filas existentes reciben un UUID
                // y quedan dirty para subirse en la primera sincronización.
                db.execSQL("ALTER TABLE custom_chords ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_chords ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE custom_chords ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE custom_chords SET uuid = lower(hex(randomblob(16))) WHERE uuid = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_chords_uuid ON custom_chords(uuid)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bloqueo de partitura: el candado lo fija el Worker y baja con el
                // cuerpo (#locked). Las canciones existentes empiezan desbloqueadas.
                db.execSQL("ALTER TABLE songs ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Papelera de reciclaje: borrado lógico de partituras.
                db.execSQL("ALTER TABLE songs ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Antes de imponer la unicidad: si algún fichero del bucket quedó
                // enlazado a dos canciones, se conserva la más antigua y las demás
                // pasan a ser solo locales (el índice fallaría con duplicados).
                db.execSQL(
                    """
                    UPDATE songs SET remote_key = NULL, remote_etag = NULL, remote_updated_at = 0
                    WHERE remote_key IS NOT NULL AND id NOT IN (
                        SELECT MIN(id) FROM songs WHERE remote_key IS NOT NULL GROUP BY remote_key
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_songs_remote_key ON songs(remote_key)"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sincronización con cuenta de usuario: id de la partitura en la
                // API y visibilidad. remote_key se conserva para reconocer, en
                // el primer sync con sesión, lo que ya estaba subido.
                db.execSQL("ALTER TABLE songs ADD COLUMN remote_id TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN visibility TEXT NOT NULL DEFAULT 'private'")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sincronización automática: carpetas, versiones y borrados dejan
                // de ser cosa solo local.
                //
                // Las listas y versiones que ya existan nacen `dirty = 1` para
                // que la primera sincronización las suba: hasta ahora nunca
                // habían salido del dispositivo.
                db.execSQL("ALTER TABLE playlists ADD COLUMN remote_id TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE playlists ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE playlists SET updated_at = created_at")

                db.execSQL("ALTER TABLE song_versions ADD COLUMN remote_id TEXT")
                db.execSQL("ALTER TABLE song_versions ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE song_versions ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0")

                // Revisión del servidor. 0 = "no la sé": la primera subida va sin
                // `baseRev` y el servidor no la trata como conflicto.
                db.execSQL("ALTER TABLE songs ADD COLUMN remote_rev INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_deletes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remote_id TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        purge INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_deletes_remote_id ON pending_deletes(remote_id)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_dirty ON songs(dirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_deleted_at ON songs(deleted_at)")

                // Favoritos y carpetas ya no viajan escondidos en el texto: para
                // que la primera sincronización los suba como campos, todo lo que
                // esté enlazado con una cuenta se marca pendiente.
                db.execSQL("UPDATE songs SET dirty = 1 WHERE remote_id IS NOT NULL AND deleted_at = 0")
            }
        }

        /**
         * Filas semilla del entrenamiento (perfil único + una fila por área),
         * para que el código nunca encuentre un perfil inexistente. Se invoca
         * en la migración 9→10 Y en onCreate (instalaciones nuevas).
         */
        private fun seedTraining(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO training_profile
                    (id, xp_total, streak_current, streak_best, last_practice_day, placement_done, created_at)
                VALUES (1, 0, 0, 0, 0, 0, $now)
                """.trimIndent()
            )
            for (area in TrainingArea.entries) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO area_progress (area, unlocked_level, xp_area, updated_at)
                    VALUES ('${area.name}', 1, 0, $now)
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guitarchords.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                        MIGRATION_16_17
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            seedTraining(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
