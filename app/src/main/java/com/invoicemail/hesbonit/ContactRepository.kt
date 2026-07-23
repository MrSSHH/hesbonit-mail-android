package com.invoicemail.hesbonit

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val dao: ContactDao) {
    fun getAll(): Flow<List<Contact>> = dao.getAll()
    fun search(query: String): Flow<List<Contact>> = dao.search(query)
    suspend fun insert(contact: Contact) = dao.insert(contact)
    suspend fun delete(contact: Contact) = dao.delete(contact)
}