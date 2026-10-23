package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.MAX_DOCK_FAVORITES
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.orderFavoriteApps

class ProjectHomeUseCase {
    operator fun invoke(apps: List<TvApp>, favorites: List<String>, folders: List<AppFolder>): HomeProjection {
        val ordered = orderFavoriteApps(apps, favorites).take(MAX_DOCK_FAVORITES)
        val occupied = ordered.mapTo(hashSetOf()) { it.id }
        folders.forEach { occupied.addAll(it.apps) }
        return HomeProjection(ordered, apps.filterNot { it.id in occupied })
    }
}
