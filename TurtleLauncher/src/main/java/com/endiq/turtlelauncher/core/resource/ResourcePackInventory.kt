package com.endiq.turtlelauncher.core.resource

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans resource packs and includes only entries with valid pack.mcmeta metadata. */
class ResourcePackInventory {
    suspend fun scan(directory: File): List<LocalResource> = withContext(Dispatchers.IO) {
        ResourcePackFiles.listCandidates(directory).mapNotNull { file ->
            if (ResourcePackFiles.readResourcePackMetadata(file) == null) return@mapNotNull null
            runCatching {
                LocalResource(
                    id = file.canonicalFile.path,
                    type = ResourceType.RESOURCE_PACK,
                    name = file.nameWithoutExtension,
                    file = file,
                    isDirectory = file.isDirectory
                )
            }.getOrNull()
        }
    }
}
