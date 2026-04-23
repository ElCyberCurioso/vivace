package com.guitarchords.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Playlist::class, Song::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao

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

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guitarchords.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
