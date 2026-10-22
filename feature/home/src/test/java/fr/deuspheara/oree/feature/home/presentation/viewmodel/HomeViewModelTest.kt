package fr.deuspheara.oree.feature.home.presentation.viewmodel

import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.model.TvApp
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun noSubscriberMeansNoObserverAndResumeRefreshPreservesCachedFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var observers = 0
            var loads = 0
            val app = TvApp("app", "package", "Name", 0)
            val repository =
                object : AppRepository {
                    override val changes = flow {
                        observers++
                        try {
                            emit(Unit)
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            observers--
                        }
                    }

                    override suspend fun load(): List<TvApp> {
                        loads++
                        if (loads > 1) throw IOException("fixture")
                        return listOf(app)
                    }
                }
            val viewModel =
                HomeViewModel(
                    repository,
                    object : OrganizationRepository {
                        override suspend fun setFavorite(id: String, component: String, favorite: Boolean) =
                            error("Unused fixture operation")

                        override suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>) =
                            error("Unused fixture operation")

                        override suspend fun createFolder(profileId: String, name: String, appId: String) =
                            error("Unused fixture operation")

                        override suspend fun renameFolder(profileId: String, folderId: String, name: String) =
                            error("Unused fixture operation")

                        override suspend fun deleteFolder(profileId: String, folderId: String) = error("Unused fixture operation")

                        override suspend fun moveToFolder(profileId: String, appId: String, folderId: String?) =
                            error("Unused fixture operation")

                        override suspend fun reorderFolder(
                            profileId: String,
                            folderId: String,
                            expected: List<String>,
                            order: List<String>,
                        ) = error("Unused fixture operation")
                    },
                )
            runCurrent()
            assertEquals(0, observers)
            val first = backgroundScope.launch { viewModel.state.collect() }
            runCurrent()
            assertEquals(listOf(app), viewModel.state.value.apps)
            first.cancel()
            runCurrent()
            assertEquals(0, observers)
            val resumed = backgroundScope.launch { viewModel.state.collect() }
            runCurrent()
            assertEquals(2, loads)
            assertEquals(listOf(app), viewModel.state.value.apps)
            assertTrue(viewModel.state.value.failed)
            resumed.cancel()
            runCurrent()
            assertEquals(0, observers)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
