package com.endiq.turtlelauncher.core.resource

/**
 * Resource categories managed by the launcher.
 *
 * This is deliberately independent of UI and renderer implementations so it can be
 * reused by future resource-management screens.
 */
enum class ResourceType {
    MOD,
    SHADER_PACK,
    RESOURCE_PACK,
    MODPACK
}
