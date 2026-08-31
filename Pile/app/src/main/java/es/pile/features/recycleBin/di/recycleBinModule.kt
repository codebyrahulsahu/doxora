package es.pile.features.recycleBin.di

import es.pile.features.recycleBin.data.workers.TrashCleanupWorker
import es.pile.features.recycleBin.domain.useCases.DeleteTrashedDocumentUseCase
import es.pile.features.recycleBin.domain.useCases.GetTrashDataUseCase
import es.pile.features.recycleBin.domain.useCases.PurgeExpiredTrashEntriesUseCase
import es.pile.features.recycleBin.domain.useCases.RestoreTrashedDocumentUseCase
import es.pile.features.recycleBin.ui.RecycleBinViewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recycleBinModule = module {
    factoryOf(::DeleteTrashedDocumentUseCase)
    factoryOf(::RestoreTrashedDocumentUseCase)
    factoryOf(::PurgeExpiredTrashEntriesUseCase)
    factoryOf(::GetTrashDataUseCase)

    workerOf(::TrashCleanupWorker)

    viewModelOf(::RecycleBinViewModel)
}
