package com.guitarchords.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordMergeTest {

    private fun rec(uuid: String, ts: Long, frets: String = "0,2,2,1,0,0", deleted: Boolean = false) =
        ChordRecord(uuid = uuid, key = "Bm", frets = frets, position = 0, updatedAt = ts, deleted = deleted)

    @Test fun `remoto vacio sube lo local`() {
        val local = listOf(rec("a", 10), rec("b", 20))
        val r = ChordMerge.merge(local, emptyList())
        assertEquals(2, r.merged.size)
        assertTrue(r.needsPush)
        assertTrue(r.toApplyLocally.isEmpty()) // local ya está al día
    }

    @Test fun `local vacio aplica lo remoto sin subir`() {
        val remote = listOf(rec("a", 10), rec("b", 20))
        val r = ChordMerge.merge(emptyList(), remote)
        assertEquals(2, r.merged.size)
        assertFalse(r.needsPush)
        assertEquals(2, r.toApplyLocally.size)
    }

    @Test fun `gana el mas reciente - remoto`() {
        val local = listOf(rec("a", 10, frets = "x,x,x,x,x,x"))
        val remote = listOf(rec("a", 20, frets = "0,2,2,1,0,0"))
        val r = ChordMerge.merge(local, remote)
        assertEquals(20, r.merged.single().updatedAt)
        assertFalse(r.needsPush)
        assertEquals(1, r.toApplyLocally.size)
    }

    @Test fun `gana el mas reciente - local sube`() {
        val local = listOf(rec("a", 30))
        val remote = listOf(rec("a", 20))
        val r = ChordMerge.merge(local, remote)
        assertEquals(30, r.merged.single().updatedAt)
        assertTrue(r.needsPush)
        assertTrue(r.toApplyLocally.isEmpty())
    }

    @Test fun `tombstone mas reciente se propaga`() {
        val local = listOf(rec("a", 10))
        val remote = listOf(rec("a", 20, deleted = true))
        val r = ChordMerge.merge(local, remote)
        assertTrue(r.merged.single().deleted)
        assertEquals(1, r.toApplyLocally.size) // aplicar el tombstone en local
        assertFalse(r.needsPush)
    }

    @Test fun `union de uuids distintos`() {
        val local = listOf(rec("a", 10))
        val remote = listOf(rec("b", 10))
        val r = ChordMerge.merge(local, remote)
        assertEquals(2, r.merged.size)
        assertTrue(r.needsPush) // remoto no tiene "a"
        assertEquals(1, r.toApplyLocally.size) // aplicar "b" en local
    }

    @Test fun `empate vivo gana al tombstone`() {
        val local = listOf(rec("a", 50, deleted = true))
        val remote = listOf(rec("a", 50, deleted = false))
        val r = ChordMerge.merge(local, remote)
        assertFalse(r.merged.single().deleted)
    }
}
