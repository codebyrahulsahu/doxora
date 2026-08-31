package es.pile.features.home.ui

import es.pile.DocumentModel
import es.pile.PileModel
import es.pile.core.domain.models.UserSettings
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.PileModelRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.domain.useCases.CreatePileUseCase
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import es.pile.features.documentDetail.domain.helper.DocumentOpener
import es.pile.features.documentDetail.domain.useCases.MoveDocumentToTrashUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentImagesUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentUseCase
import es.pile.features.documentDetail.domain.useCases.export.GetPdfUriUseCase
import es.pile.features.home.domain.schedulers.CleanupScheduler
import es.pile.features.home.domain.useCases.CreateDocumentUseCase
import es.pile.features.home.domain.useCases.GetHomeDataUseCase
import es.pile.features.home.domain.useCases.ManageTemporaryDocumentUseCase
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val createDocumentUseCase: CreateDocumentUseCase = mockk()
    private val manageTemporaryDocumentUseCase: ManageTemporaryDocumentUseCase = mockk()
    private val getHomeDataUseCase: GetHomeDataUseCase = mockk()
    private val createPileUseCase: CreatePileUseCase = mockk()
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase = mockk(relaxed = true)
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase =
        mockk(relaxed = true)
    private val cleanupScheduler: CleanupScheduler = mockk()
    private val bitmapCacheRepository: BitmapCacheRepository = mockk(relaxed = true)
    private val fileRepository: FileRepository = mockk()
    private val favoritesRepository: FavoritesRepository = mockk {
        every { favoriteDocumentIds } returns flowOf(emptyList())
    }
    private val documentLockRepository: DocumentLockRepository = mockk {
        every { lockedDocumentIds } returns flowOf(emptySet())
    }
    private val settingsRepository: SettingsRepository = mockk(relaxed = true) {
        every { userSettings } returns flowOf(UserSettings())
    }
    private val pileModelRepository: PileModelRepository = mockk(relaxed = true)
    private val moveDocumentToTrashUseCase: MoveDocumentToTrashUseCase = mockk(relaxed = true)
    private val getPdfUriUseCase: GetPdfUriUseCase = mockk(relaxed = true)
    private val exportDocumentUseCase: ExportDocumentUseCase = mockk(relaxed = true)
    private val exportDocumentImagesUseCase: ExportDocumentImagesUseCase = mockk(relaxed = true)
    private val documentOpener: DocumentOpener = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load home data`() = runTest {
        // Given
        val homeData = GetHomeDataUseCase.HomeData(
            piles = listOf(PileModel("p1", "Pile 1", "icon1", 1, 0)),
            documents = listOf(mockk<DocumentModel>(relaxed = true) {
                every { id } returns "d1"
            }),
            temporaryDocument = null,
            coloredPileIds = listOf("p1")
        )
        every { getHomeDataUseCase() } returns flowOf(homeData)
        coEvery { getDocumentSizesUseCase(any()) } returns emptyMap()

        // When
        val viewModel = HomeViewModel(
            createDocumentUseCase,
            manageTemporaryDocumentUseCase,
            getHomeDataUseCase,
            createPileUseCase,
            requestCoverThumbnailUseCase,
            getDocumentSizesUseCase,
            cleanupScheduler,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            pileModelRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        // Then
        val state = viewModel.state.value
        assertEquals(1, state.pileModels.size)
        assertEquals(1, state.documentCoverItems.size)
        assertFalse(state.isInitialLoading)
    }
}
