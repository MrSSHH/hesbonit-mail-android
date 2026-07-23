package com.invoicemail.hesbonit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeContactDao : ContactDao {

    private val contactsFlow = MutableStateFlow<List<Contact>>(emptyList())
    private var nextId = 1

    override fun getAll(): Flow<List<Contact>> =
        contactsFlow.map { list -> list.sortedBy { it.name } }

    override fun search(query: String): Flow<List<Contact>> =
        contactsFlow.map { list ->
            list.filter { it.name.contains(query) }.sortedBy { it.name }
        }

    override suspend fun insert(contact: Contact) {
        contactsFlow.value = contactsFlow.value + contact.copy(id = nextId++)
    }

    override suspend fun delete(contact: Contact) {
        contactsFlow.value = contactsFlow.value.filterNot { it.id == contact.id }
    }
}