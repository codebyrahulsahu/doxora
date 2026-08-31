package es.pile.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import es.pile.core.data.local.AppPreferencesSerializer
import es.pile.core.data.local.UserSettingsSerializer
import es.pile.core.data.repositories.BitmapCacheRepositoryImpl
import es.pile.core.data.repositories.DataStoreAppPreferencesRepository
import es.pile.core.data.repositories.DataStoreSettingsRepository
import es.pile.core.data.repositories.DocumentImageRepositoryImpl
import es.pile.core.data.repositories.DocumentLockRepositoryImpl
import es.pile.core.data.repositories.DocumentModelRepositoryImpl
import es.pile.core.data.repositories.DocumentTextRepositoryImpl
import es.pile.core.data.repositories.FileRepositoryImpl
import es.pile.core.data.repositories.FavoritesRepositoryImpl
import es.pile.core.data.repositories.LocalBackupRepositoryImpl
import es.pile.core.data.repositories.MlKitTextRecognitionRepository
import es.pile.core.data.repositories.PileModelRepositoryImpl
import es.pile.core.data.repositories.TrashRepositoryImpl
import es.pile.core.data.util.ImageTransformationHelper
import es.pile.core.data.util.PdfRenderHelper
import es.pile.core.domain.models.AppPreferences
import es.pile.core.domain.models.UserSettings
import es.pile.core.domain.repositories.AppPreferencesRepository
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.DocumentTextRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.LocalBackupRepository
import es.pile.core.domain.repositories.PileModelRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.domain.repositories.TextRecognitionRepository
import es.pile.core.domain.repositories.TrashRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val Context.dataStore: DataStore<UserSettings> by dataStore(
    fileName = "settings.json",
    serializer = UserSettingsSerializer,
)

val Context.appPrefsDataStore: DataStore<AppPreferences> by dataStore(
    fileName = "app_preferences.json",
    serializer = AppPreferencesSerializer,
)

val dataModule = module {
    single<SettingsRepository> {
        DataStoreSettingsRepository(dataStore = androidContext().dataStore, ioDispatcher = get())
    }

    single<AppPreferencesRepository> {
        DataStoreAppPreferencesRepository(dataStore = androidContext().appPrefsDataStore, ioDispatcher = get())
    }

    singleOf(::ImageTransformationHelper)
    singleOf(::PdfRenderHelper)

    single<PileModelRepository> {
        PileModelRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<DocumentModelRepository> {
        DocumentModelRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<DocumentImageRepository> {
        DocumentImageRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<FavoritesRepository> {
        FavoritesRepositoryImpl(databaseQueries = get(), ioDispatcher = get())
    }

    single<DocumentTextRepository> {
        DocumentTextRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<DocumentLockRepository> {
        DocumentLockRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<TextRecognitionRepository> {
        MlKitTextRecognitionRepository(ioDispatcher = get())
    }

    single<TrashRepository> {
        TrashRepositoryImpl(
            databaseQueries = get(),
            ioDispatcher = get()
        )
    }

    single<LocalBackupRepository> {
        LocalBackupRepositoryImpl(
            appContext = get(),
            ioDispatcher = get(),
            databaseQueries = get(),
            fileRepository = get(),
            settingsRepository = get()
        )
    }

    single<FileRepository> {
        FileRepositoryImpl(
            appContext = get(),
            ioDispatcher = get(),
            pdfRenderHelper = get(),
            imageTransformationHelper = get()
        )
    }

    single<BitmapCacheRepository> {
        BitmapCacheRepositoryImpl(
            ioDispatcher = get(),
            pdfRenderHelper = get(),
            imageTransformationHelper = get()
        )
    }
}