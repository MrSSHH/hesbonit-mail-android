package com.invoicemail.hesbonit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(private val repository: ContactRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val contacts: StateFlow<List<Contact>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAll() else repository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun selectContact(contact: Contact) {
        _selectedContact.value = contact
    }

    fun addContact(name: String, email: String) {
        if (name.isBlank() || email.isBlank()) return
        viewModelScope.launch {
            repository.insert(Contact(name = name, email = email))
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.delete(contact)
            if (_selectedContact.value?.id == contact.id) _selectedContact.value = null
        }
    }

    class Factory(private val repository: ContactRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactViewModel(repository) as T
        }
    }
}