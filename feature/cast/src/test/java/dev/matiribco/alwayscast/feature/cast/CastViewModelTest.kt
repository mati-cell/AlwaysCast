package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.cast.CastController
import dev.matiribco.alwayscast.core.model.CastState
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.MediaSourceResolver
import dev.matiribco.alwayscast.core.model.ResolvedSource
import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.network.MediaInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.Result

@OptIn(ExperimentalCoroutinesApi::class)
class CastViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onCastClick_hevcCandidate_setsUnsupportedPhaseAndNeverLoads() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unsupported(UnsupportedReason.HEVC)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        throw AssertionError(RESOLVER_SHOULD_NOT_BE_CALLED)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                )
            viewModel.onUrlChange(HEVC_URL)
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.UNSUPPORTED, state.phase)
            assertTrue(state.message?.contains("HEVC", ignoreCase = true) == true)
            assertEquals(0, recordingController.loadCallCount)
        }

    @Test
    fun onCastClick_supportedMediaAndAvailableDevice_transitionsToCastingWithResolvedUrl() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        ResolvedSource(RESOLVED_PROXY_URL, MIME_MP4)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                )
            viewModel.onUrlChange(VALID_URL)
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.CASTING, state.phase)
            assertEquals(RESOLVED_PROXY_URL, recordingController.lastLoadedUrl)
            assertEquals(1, recordingController.loadCallCount)
        }

    @Test
    fun onCastClick_deviceNotFound_setsErrorPhaseWithDiagnosticMessage() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        ResolvedSource(RESOLVED_PROXY_URL, MIME_MP4)
                }
            val failingConnectController = FailingConnectCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = failingConnectController,
                )
            viewModel.onUrlChange(VALID_URL)
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.ERROR, state.phase)
            assertTrue(state.message?.contains("Chromecast", ignoreCase = true) == true)
            assertEquals(0, failingConnectController.loadCallCount)
        }

    @Test
    fun onCastClick_unknownVerdictWithInspectorSupported_inspectsAndCasts() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unknown(null)
                }
            val fakeInspector =
                object : MediaInspector {
                    override suspend fun inspect(candidate: MediaCandidate): Verdict = Verdict.Supported(MIME_MP4)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        ResolvedSource(RESOLVED_PROXY_URL, MIME_MP4)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                    inspector = fakeInspector,
                )
            viewModel.onUrlChange(VALID_URL)
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.CASTING, state.phase)
            assertEquals(1, recordingController.loadCallCount)
        }

    @Test
    fun onCastClick_unknownVerdictWithInspectorUnsupported_setsUnsupportedPhase() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unknown(null)
                }
            val fakeInspector =
                object : MediaInspector {
                    override suspend fun inspect(candidate: MediaCandidate): Verdict =
                        Verdict.Unsupported(UnsupportedReason.AV1)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        throw AssertionError(RESOLVER_SHOULD_NOT_BE_CALLED)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                    inspector = fakeInspector,
                )
            viewModel.onUrlChange(VALID_URL)
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.UNSUPPORTED, state.phase)
            assertTrue(state.message?.contains("AV1", ignoreCase = true) == true)
            assertEquals(0, recordingController.loadCallCount)
        }

    @Test
    fun onCastClick_unknownVerdictRemainsUnknown_setsUnsupportedPhaseAndNeverLoads() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unknown(null)
                }
            val fakeInspector =
                object : MediaInspector {
                    override suspend fun inspect(candidate: MediaCandidate): Verdict = Verdict.Unknown(null)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        throw AssertionError(RESOLVER_SHOULD_NOT_BE_CALLED)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                    inspector = fakeInspector,
                )
            viewModel.onUrlChange("https://ok.ru/video/7715958885030")
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.UNSUPPORTED, state.phase)
            assertEquals(0, recordingController.loadCallCount)
        }

    @Test
    fun onCastClick_unsupportedNoMedia_setsUnsupportedPhaseWithWebpageNotice() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unsupported(UnsupportedReason.NO_MEDIA)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        throw AssertionError(RESOLVER_SHOULD_NOT_BE_CALLED)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                )
            viewModel.onUrlChange("https://ok.ru/video/7715958885030")
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.UNSUPPORTED, state.phase)
            val hasExpectedNotice =
                state.message?.contains("página", ignoreCase = true) == true ||
                    state.message?.contains("video", ignoreCase = true) == true
            assertTrue(hasExpectedNotice)
            assertEquals(0, recordingController.loadCallCount)
        }

    @Test
    fun onConnectClick_deviceAvailable_connectsAndSetsReadyPhase() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        ResolvedSource(RESOLVED_PROXY_URL, MIME_MP4)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                )
            viewModel.onConnectClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.READY, state.phase)
            assertEquals(TEST_DEVICE_NAME, state.deviceName)
        }

    @Test
    fun onUrlChange_blankUrl_doesNotTriggerCasting() =
        runTest(testDispatcher) {
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val fakeResolver =
                object : MediaSourceResolver {
                    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource =
                        throw AssertionError(RESOLVER_SHOULD_NOT_BE_CALLED)
                }
            val recordingController = RecordingCastController()

            val viewModel =
                CastViewModel(
                    classifier = fakeClassifier,
                    resolver = fakeResolver,
                    controller = recordingController,
                )
            viewModel.onUrlChange("   ")
            viewModel.onCastClick()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(Phase.IDLE, state.phase)
            assertEquals(0, recordingController.loadCallCount)
        }

    private class RecordingCastController : CastController {
        override val state: StateFlow<CastState> = MutableStateFlow(CastState.Idle).asStateFlow()
        var lastLoadedUrl: String? = null
        var loadCallCount: Int = 0
        var connectCallCount: Int = 0

        override suspend fun connect(): Result<String> {
            connectCallCount++
            return Result.success(TEST_DEVICE_NAME)
        }

        override suspend fun load(
            source: ResolvedSource,
            title: String?,
        ): Result<Unit> {
            loadCallCount++
            lastLoadedUrl = source.url
            return Result.success(Unit)
        }

        override fun playOrPause() = Unit

        override fun seekTo(seconds: Double) = Unit

        override fun stop() = Unit

        override fun disconnect() = Unit
    }

    private class FailingConnectCastController : CastController {
        override val state: StateFlow<CastState> = MutableStateFlow(CastState.Idle).asStateFlow()
        var loadCallCount: Int = 0

        override suspend fun connect(): Result<String> = Result.failure(IllegalStateException(CONNECT_FAILURE_MESSAGE))

        override suspend fun load(
            source: ResolvedSource,
            title: String?,
        ): Result<Unit> {
            loadCallCount++
            return Result.success(Unit)
        }

        override fun playOrPause() = Unit

        override fun seekTo(seconds: Double) = Unit

        override fun stop() = Unit

        override fun disconnect() = Unit
    }

    private companion object {
        const val HEVC_URL = "https://example.com/video.hevc"
        const val VALID_URL = "https://example.com/video.mp4"
        const val RESOLVED_PROXY_URL = "http://127.0.0.1:8080/s/token/video.mp4"
        const val MIME_MP4 = "video/mp4"
        const val TEST_DEVICE_NAME = "Living Room Chromecast"
        const val CONNECT_FAILURE_MESSAGE = "no se encontró el Chromecast en la red"
        const val RESOLVER_SHOULD_NOT_BE_CALLED = "resolver should not be called"
    }
}
