package es.pile.features.pileDetail.ui

import android.graphics.Bitmap
import android.net.Uri
import app.cash.turbine.test
import es.pile.DocumentModel
import es.pile.PileModel
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.PileModelRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import es.pile.core.domain.models.ImageCompressionChoice
import es.pile.core.domain.models.UserSettings
import es.pile.features.documentDetail.domain.helper.DocumentOpener
import es.pile.features.documentDetail.domain.useCases.MoveDocumentToTrashUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentImagesUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentUseCase
import es.pile.features.documentDetail.domain.useCases.export.GetPdfUriUseCase
import es.pile.features.home.domain.useCases.CreateDocumentUseCase
import es.pile.features.pileDetail.domain.usecases.DeletePileUseCase
import es.pile.features.pileDetail.domain.usecases.UpdatePileUseCase
import io.mockk.any
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class PileDetailViewModelTest {

    private val pileId = "test-pile-id"
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase = mockk(relaxed = true)
    private val createDocumentUseCase: CreateDocumentUseCase = mockk()
    private val updatePileUseCase: UpdatePileUseCase = mockk()
    private val deletePileUseCase: DeletePileUseCase = mockk()
    private val pileModelRepository: PileModelRepository = mockk()
    private val documentModelRepository: DocumentModelRepository = mockk()
    private val bitmapCacheRepository: BitmapCacheRepository = mockk(relaxed = true)
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase =
        mockk(relaxed = true)
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
    private val moveDocumentToTrashUseCase: MoveDocumentToTrashUseCase = mockk(relaxed = true)
    private val getPdfUriUseCase: GetPdfUriUseCase = mockk(relaxed = true)
    private val exportDocumentUseCase: ExportDocumentUseCase = mockk(relaxed = true)
    private val exportDocumentImagesUseCase: ExportDocumentImagesUseCase = mockk(relaxed = true)
    private val documentOpener: DocumentOpener = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { pileModelRepository.getPileModelById(pileId) } returns flowOf(PileModel(pileId, "Pile", "icon", 0, 0))
        every { documentModelRepository.documentModels } returns flowOf(emptyList())
        coEvery { getDocumentSizesUseCase(any()) } returns emptyMap()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `OnPdfImported should call createFromPdf with pileId and navigate`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val mockDoc = mockk<DocumentModel> {
            every { id } returns "new-doc-id"
        }
        coEvery { createDocumentUseCase.createFromPdf(mockUri, initialPileIds = listOf(pileId)) } returns mockDoc

        val viewModel = PileDetailViewModel(
            pileId,
            requestCoverThumbnailUseCase,
            createDocumentUseCase,
            updatePileUseCase,
            deletePileUseCase,
            getDocumentSizesUseCase,
            pileModelRepository,
            documentModelRepository,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.handleEvent(PileDetailEvent.OnPdfImported(mockUri))
            assertEquals(mockDoc, awaitItem())
        }
    }

    @Test
    fun `OnImagesImported should ask for compression and import on confirmation`() = runTest {
        // Given
        val mockUris = listOf(mockk<Uri>())
        val mockDoc = mockk<DocumentModel> {
            every { id } returns "new-doc-id"
        }
        val compressionChoice = ImageCompressionChoice.original()
        coEvery {
            createDocumentUseCase.createFromImages(
                mockUris,
                initialPileIds = listOf(pileId),
                compression = compressionChoice
            )
        } returns mockDoc

        val viewModel = PileDetailViewModel(
            pileId,
            requestCoverThumbnailUseCase,
            createDocumentUseCase,
            updatePileUseCase,
            deletePileUseCase,
            getDocumentSizesUseCase,
            pileModelRepository,
            documentModelRepository,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        // When & Then
        viewModel.navigationEvent.test {
            // The images are not imported right away: the compression prompt is shown first.
            viewModel.handleEvent(PileDetailEvent.OnImagesImported(mockUris))
            assertEquals(mockUris, viewModel.state.value.pendingImageImport?.uris)

            // Answering the prompt performs the actual import.
            viewModel.handleEvent(PileDetailEvent.OnImageCompressionConfirmed(compressionChoice))
            assertEquals(null, viewModel.state.value.pendingImageImport)
            assertEquals(mockDoc, awaitItem())
        }
    }

    @Test
    fun `OnHubPicturePicked should open the cropper and store the cropped picture`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val pickedBitmap = mockk<Bitmap>()
        val croppedBitmap = mockk<Bitmap>()

        coEvery { fileRepository.loadPictureForCropping(mockUri) } returns pickedBitmap
        coEvery { fileRepository.saveProfilePicture(croppedBitmap, null) } returns "profile_1.jpg"

        val viewModel = PileDetailViewModel(
            pileId,
            requestCoverThumbnailUseCase,
            createDocumentUseCase,
            updatePileUseCase,
            deletePileUseCase,
            getDocumentSizesUseCase,
            pileModelRepository,
            documentModelRepository,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        // When: the picture is picked, it is not stored yet but sent to the cropper
        viewModel.handleEvent(PileDetailEvent.OnHubPicturePicked(mockUri))

        // Then
        assertEquals(pickedBitmap, viewModel.state.value.hubPictureToCrop)
        coVerify(exactly = 0) { fileRepository.saveProfilePicture(any<Bitmap>(), any()) }

        // When: the crop is confirmed, the cropped picture becomes the hub picture
        viewModel.handleEvent(PileDetailEvent.OnHubPictureCropConfirmed(croppedBitmap))

        // Then
        assertEquals(null, viewModel.state.value.hubPictureToCrop)
        coVerify { fileRepository.saveProfilePicture(croppedBitmap, null) }
        coVerify { settingsRepository.updateHubPicturePath(pileId, "profile_1.jpg") }
    }

    @Test
    fun `OnHubPictureCropDismissed should discard the picked picture`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val pickedBitmap = mockk<Bitmap>()

        coEvery { fileRepository.loadPictureForCropping(mockUri) } returns pickedBitmap

        val viewModel = PileDetailViewModel(
            pileId,
            requestCoverThumbnailUseCase,
            createDocumentUseCase,
            updatePileUseCase,
            deletePileUseCase,
            getDocumentSizesUseCase,
            pileModelRepository,
            documentModelRepository,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        viewModel.handleEvent(PileDetailEvent.OnHubPicturePicked(mockUri))
        assertEquals(pickedBitmap, viewModel.state.value.hubPictureToCrop)

        // When
        viewModel.handleEvent(PileDetailEvent.OnHubPictureCropDismissed)

        // Then
        assertEquals(null, viewModel.state.value.hubPictureToCrop)
        coVerify(exactly = 0) { fileRepository.saveProfilePicture(any<Bitmap>(), any()) }
    }

    @Test
    fun `answering the resizer prompt is remembered in the settings`() = runTest {
        // Given
        val mockUris = listOf(mockk<Uri>())
        val mockDoc = mockk<DocumentModel> {
            every { id } returns "new-doc-id"
        }
        val choice = ImageCompressionChoice(compress = true, targetSizeKb = 1024)

        coEvery {
            createDocumentUseCase.createFromImages(
                mockUris,
                initialPileIds = listOf(pileId),
                compression = choice
            )
        } returns mockDoc

        val viewModel = PileDetailViewModel(
            pileId,
            requestCoverThumbnailUseCase,
            createDocumentUseCase,
            updatePileUseCase,
            deletePileUseCase,
            getDocumentSizesUseCase,
            pileModelRepository,
            documentModelRepository,
            bitmapCacheRepository,
            fileRepository,
            favoritesRepository,
            documentLockRepository,
            settingsRepository,
            moveDocumentToTrashUseCase,
            getPdfUriUseCase,
            exportDocumentUseCase,
            exportDocumentImagesUseCase,
            documentOpener
        )

        // When
        viewModel.navigationEvent.test {
            viewModel.handleEvent(PileDetailEvent.OnImagesImported(mockUris))
            viewModel.handleEvent(PileDetailEvent.OnImageCompressionConfirmed(choice))
            assertEquals(mockDoc, awaitItem())
        }

        // Then: the ON/OFF switch and the size become the new defaults
        coVerify { settingsRepository.updateDocumentResizerEnabled(true) }
        coVerify { settingsRepository.updateDocumentResizerTargetSizeKb(1024) }
    }
}
