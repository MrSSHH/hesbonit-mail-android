package com.invoicemail.hesbonit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ContactDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.contactDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetAll_returnsContactsSortedByName() = runTest {
        dao.insert(Contact(name = "׳™׳•׳¡׳™ ׳׳•׳™", email = "yossi@example.com"))
        dao.insert(Contact(name = "׳׳‘׳™ ׳›׳”׳", email = "avi@example.com"))

        val contacts = dao.getAll().first()

        assertEquals(2, contacts.size)
        assertEquals("׳׳‘׳™ ׳›׳”׳", contacts[0].name)
        assertEquals("׳™׳•׳¡׳™ ׳׳•׳™", contacts[1].name)
    }

    @Test
    fun search_filtersByNameSubstring() = runTest {
        dao.insert(Contact(name = "׳“׳ ׳” ׳›׳”׳", email = "dana@example.com"))
        dao.insert(Contact(name = "׳™׳•׳¡׳™ ׳׳•׳™", email = "yossi@example.com"))

        val results = dao.search("׳›׳”׳").first()

        assertEquals(1, results.size)
        assertEquals("׳“׳ ׳” ׳›׳”׳", results[0].name)
    }

    @Test
    fun search_withNoMatches_returnsEmptyList() = runTest {
        dao.insert(Contact(name = "׳“׳ ׳” ׳›׳”׳", email = "dana@example.com"))

        val results = dao.search("xyz").first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun delete_removesOnlyTheSpecifiedContact() = runTest {
        dao.insert(Contact(name = "׳׳‘׳™ ׳›׳”׳", email = "avi@example.com"))
        dao.insert(Contact(name = "׳“׳ ׳” ׳›׳”׳", email = "dana@example.com"))
        val toDelete = dao.getAll().first().first { it.name == "׳׳‘׳™ ׳›׳”׳" }

        dao.delete(toDelete)

        val remaining = dao.getAll().first()
        assertEquals(1, remaining.size)
        assertEquals("׳“׳ ׳” ׳›׳”׳", remaining[0].name)
    }
}