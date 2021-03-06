package com.smart.template.repository.remote.core

import com.google.gson.Gson

class CXResponseModel<T> {
    companion object Initializer {
        private val gson: Gson by lazy { Gson() }
    }

    var errorCode = -1
    var errorMessage = ""
    var result: T? = null

    override fun toString(): String = gson.toJson(this)
}
