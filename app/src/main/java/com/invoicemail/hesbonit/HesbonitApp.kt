package com.invoicemail.hesbonit

import android.app.Application

class HesbonitApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}