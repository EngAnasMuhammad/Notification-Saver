package com.anasdev.notificationsaver.di

import android.app.Application
import androidx.room.Room
import com.anasdev.notificationsaver.data.local.NotificationDao
import com.anasdev.notificationsaver.data.local.NotificationDatabase
import com.anasdev.notificationsaver.data.repository.NotificationRepositoryImpl
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationDatabase(app: Application): NotificationDatabase {
        return Room.databaseBuilder(
            app,
            NotificationDatabase::class.java,
            "notification_db"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(db: NotificationDatabase): NotificationDao {
        return db.notificationDao()
    }
}
