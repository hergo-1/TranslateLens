package com.translatelens.di

import android.content.Context
import com.translatelens.data.database.AppDatabase
import com.translatelens.data.image.ImageFiles
import com.translatelens.data.image.Renderer
import com.translatelens.data.repository.HistoryRepositoryImpl
import com.translatelens.data.repository.ImageTranslationRepositoryImpl
import com.translatelens.data.repository.OcrRepositoryImpl
import com.translatelens.data.repository.SettingsRepositoryImpl
import com.translatelens.data.repository.TranslationRepositoryImpl
import com.translatelens.domain.repository.HistoryRepository
import com.translatelens.domain.repository.ImageTranslationRepository
import com.translatelens.domain.repository.OcrRepository
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase) = db.translationHistoryDao()

    @Provides
    @Singleton
    fun provideImageFiles(@ApplicationContext context: Context): ImageFiles {
        return ImageFiles(context)
    }

    @Provides
    @Singleton
    fun provideRenderer(): Renderer = Renderer()

    @Provides
    @Singleton
    fun provideOcrRepository(@ApplicationContext context: Context): OcrRepository {
        return OcrRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideTranslationRepository(
        @ApplicationContext context: Context,
        settings: SettingsRepository
    ): TranslationRepository {
        return TranslationRepositoryImpl(context, settings)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        dao: com.translatelens.data.dao.TranslationHistoryDao
    ): HistoryRepository {
        return HistoryRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideImageTranslationRepository(
        @ApplicationContext context: Context,
        ocr: OcrRepository,
        translation: TranslationRepository,
        files: ImageFiles,
        renderer: Renderer
    ): ImageTranslationRepository {
        return ImageTranslationRepositoryImpl(context, ocr, translation, files, renderer)
    }
}
