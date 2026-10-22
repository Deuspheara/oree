package fr.deuspheara.oree.core.designsystem.resource

import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.model.ContentAvailability

fun contentStatus(status: ContentAvailability): Int = when (status) {
    ContentAvailability.LOADING -> R.string.loading
    ContentAvailability.DISABLED -> R.string.content_disabled
    ContentAvailability.AVAILABLE -> R.string.content_available
    ContentAvailability.DENIED -> R.string.content_denied
    ContentAvailability.UNSUPPORTED -> R.string.content_unsupported
    ContentAvailability.EMPTY -> R.string.content_empty
    ContentAvailability.STALE -> R.string.content_stale
    ContentAvailability.NO_LOCAL_ART -> R.string.content_no_art
    ContentAvailability.ERROR -> R.string.content_error
}
