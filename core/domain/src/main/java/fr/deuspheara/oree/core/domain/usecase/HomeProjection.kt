package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.model.TvApp

data class HomeProjection(val favorites: List<TvApp>, val catalog: List<TvApp>)
