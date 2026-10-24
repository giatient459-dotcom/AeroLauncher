package com.endiq.turtlelauncher.core.resource

import java.io.File

/** A local resource discovered by an inventory scan. */
data class LocalResource(
    /** Stable within the same installation; derived from the canonical path. */
    val id: String,
    val type: ResourceType,
    val name: String,
    val file: File,
    val isDirectory: Boolean
)

/** Parsed, minimal metadata needed to identify a Minecraft resource pack. */
data class ResourcePackMetadata(
    val packFormat: Int,
    val description: String?
)
