package com.example.vera_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform