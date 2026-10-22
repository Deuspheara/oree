package fr.deuspheara.oree.core.platform.image

import coil3.Extras
import fr.deuspheara.oree.core.domain.rendering.ReflectionKey

/** Request identity shared by presentation and the bounded artwork interceptor. */
val ReflectionRequestKey = Extras.Key<ReflectionKey?>(null)
