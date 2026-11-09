package com.clipcut.editor.engine

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.clipcut.editor.engine.db.ProjectDatabase
import com.clipcut.editor.engine.db.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProjectDatabase {
        return Room.databaseBuilder(
            context,
            ProjectDatabase::class.java,
            "clipcut.db"
        )
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(*ProjectDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideProjectDao(db: ProjectDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideMemoryTrimBreadcrumbStore(
        @ApplicationContext context: Context,
    ): MemoryTrimBreadcrumbStore {
        return MemoryTrimBreadcrumbStore.forContextFilesDir(context.filesDir)
    }
}
