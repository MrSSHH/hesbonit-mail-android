package com.invoicemail.hesbonit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: FakeContactDao
    private lateinit var repository: ContactRepository
    private lateinit var viewModel: ContactViewModel

    @Before
    fun setup() {
        dao = FakeContactDao()
        repository = ContactRepository(dao)
        viewModel = ContactViewModel(repository)
    }

    @Test
    fun `addContact inserts contact and appears in list`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "dana@example.com")
        advanceUntilIdle()

        val contacts = viewModel.contacts.value
        assertEquals(1, contacts.size)
        assertEquals("׳“׳ ׳” ׳›׳”׳", contacts[0].name)
        assertEquals("dana@example.com", contacts[0].email)

        job.cancel()
    }

    @Test
    fun `addContact with blank name does not insert`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("", "dana@example.com")
        advanceUntilIdle()

        assertTrue(viewModel.contacts.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `addContact with blank email does not insert`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "")
        advanceUntilIdle()

        assertTrue(viewModel.contacts.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `onQueryChange filters contacts by name substring`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "dana@example.com")
        viewModel.addContact("׳™׳•׳¡׳™ ׳׳•׳™", "yossi@example.com")
        advanceUntilIdle()

        viewModel.onQueryChange("׳›׳”׳")
        advanceUntilIdle()

        val filtered = viewModel.contacts.value
        assertEquals(1, filtered.size)
        assertEquals("׳“׳ ׳” ׳›׳”׳", filtered[0].name)

        job.cancel()
    }

    @Test
    fun `selectContact updates selectedContact state`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "dana@example.com")
        advanceUntilIdle()
        val contact = viewModel.contacts.value.first()

        viewModel.selectContact(contact)

        assertEquals(contact, viewModel.selectedContact.value)
        job.cancel()
    }

    @Test
    fun `deleteContact removes contact and clears selection if it was selected`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "dana@example.com")
        advanceUntilIdle()
        val contact = viewModel.contacts.value.first()
        viewModel.selectContact(contact)

        viewModel.deleteContact(contact)
        advanceUntilIdle()

        assertTrue(viewModel.contacts.value.isEmpty())
        assertNull(viewModel.selectedContact.value)
        job.cancel()
    }

    @Test
    fun `deleteContact does not clear selection of a different contact`() = runTest {
        val job = launch { viewModel.contacts.collect {} }

        viewModel.addContact("׳“׳ ׳” ׳›׳”׳", "dana@example.com")
        viewModel.addContact("׳™׳•׳¡׳™ ׳׳•׳™", "yossi@example.com")
        advanceUntilIdle()

        val contacts = viewModel.contacts.value
        val dana = contacts.first { it.name == "׳“׳ ׳” ׳›׳”׳" }
        val yossi = contacts.first { it.name == "׳™׳•׳¡׳™ ׳׳•׳™" }
        viewModel.selectContact(dana)

        viewModel.deleteContact(yossi)
        advanceUntilIdle()

        assertEquals(dana, viewModel.selectedContact.value)
        job.cancel()
    }
}