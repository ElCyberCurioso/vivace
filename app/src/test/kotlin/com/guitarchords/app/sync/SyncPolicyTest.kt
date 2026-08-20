package com.guitarchords.app.sync

import com.guitarchords.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPolicyTest {

    private fun song(
        id: Long = 1,
        etag: String? = "e1",
        dirty: Boolean = false,
        deletedAt: Long = 0
    ) = Song(
        id = id,
        title = "T",
        remoteKey = "songs/a.txt",
        remoteEtag = etag,
        dirty = dirty,
        deletedAt = deletedAt
    )

    @Test
    fun `sin copia local se importa`() {
        assertEquals(PullAction.IMPORT, SyncPolicy.decidePull(null, "e1"))
    }

    @Test
    fun `mismo etag solo fluye metadata`() {
        assertEquals(PullAction.METADATA_ONLY, SyncPolicy.decidePull(song(etag = "e1"), "e1"))
    }

    @Test
    fun `etag distinto sin cambios locales descarga`() {
        assertEquals(PullAction.DOWNLOAD, SyncPolicy.decidePull(song(etag = "viejo"), "nuevo"))
    }

    @Test
    fun `etag distinto con cambios locales es conflicto`() {
        assertEquals(
            PullAction.CONFLICT,
            SyncPolicy.decidePull(song(etag = "viejo", dirty = true), "nuevo")
        )
    }

    @Test
    fun `en papelera nunca se descarga aunque cambie el servidor`() {
        assertEquals(
            PullAction.SKIP_TRASHED,
            SyncPolicy.decidePull(song(etag = "viejo", deletedAt = 123), "nuevo")
        )
    }

    @Test
    fun `en papelera tampoco genera conflicto`() {
        assertEquals(
            PullAction.SKIP_TRASHED,
            SyncPolicy.decidePull(song(etag = "viejo", dirty = true, deletedAt = 123), "nuevo")
        )
    }

    @Test
    fun `huerfanas son las que ya no estan en el bucket`() {
        val locales = listOf(
            song(id = 1).copy(remoteKey = "songs/a.txt"),
            song(id = 2).copy(remoteKey = "songs/b.txt")
        )
        val ids = SyncPolicy.orphanIds(locales, setOf("songs/a.txt"))
        assertEquals(listOf(2L), ids)
    }

    @Test
    fun `lista remota vacia no desenlaza nada`() {
        val locales = listOf(song(id = 1), song(id = 2))
        assertEquals(emptyList<Long>(), SyncPolicy.orphanIds(locales, emptySet()))
    }
}
