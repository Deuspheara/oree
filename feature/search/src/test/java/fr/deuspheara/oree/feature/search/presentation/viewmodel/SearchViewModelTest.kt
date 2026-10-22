package fr.deuspheara.oree.feature.search.presentation.viewmodel

import androidx.lifecycle.ViewModelStore
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.search.domain.SearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchViewModelTest {
    @Test fun onlyLatestSubscribedRequestProducesResultsAndReplayKeepsItsIdentity() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val model = SearchViewModel(AppDispatchers(dispatcher, dispatcher))
            store.put("search", model)
            fun request(query: String) = SearchRequest(
                query,
                listOf(TvApp("alpha", "alpha", "Alpha", 0), TvApp("beta", "beta", "Beta", 0)),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            )
            val first = request("alpha")
            val latest = request("beta")
            model.search(first)
            runCurrent()
            assertNull(model.state.value.request)
            val route = backgroundScope.launch { model.state.collect() }
            model.search(latest)
            runCurrent()
            assertEquals(latest, model.state.value.request)
            assertEquals(listOf("beta"), model.state.value.results.map { (it as SearchItem.App).app.id })
            route.cancel()
            runCurrent()
            model.search(first)
            runCurrent()
            // The old replay is identifiable while the worker has zero subscribers.
            assertEquals(latest, model.state.value.request)
            val resumed = backgroundScope.launch { model.state.collect() }
            runCurrent()
            assertEquals(first, model.state.value.request)
            assertEquals(listOf("alpha"), model.state.value.results.map { (it as SearchItem.App).app.id })
            resumed.cancel()
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
