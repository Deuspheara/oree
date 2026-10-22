package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.source.AppCatalogDataSource
import java.text.Collator
import kotlinx.coroutines.withContext

class DefaultAppRepository(private val source: AppCatalogDataSource, private val dispatchers: AppDispatchers) : AppRepository {
    override val changes = source.changes

    override suspend fun load() = withContext(dispatchers.default) {
        val apps = source.load()
        val collator = Collator.getInstance()
        apps.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
}
