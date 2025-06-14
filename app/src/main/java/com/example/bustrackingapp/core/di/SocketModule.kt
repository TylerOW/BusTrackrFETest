package com.example.bustrackingapp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import io.socket.client.IO
import io.socket.client.Socket

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {

    // Point this at your backend’s address:
    private const val SOCKET_URL = "http://10.0.2.2:5000"

    @Provides
    @Singleton
    fun provideSocket(): Socket = IO.socket(SOCKET_URL)
}
