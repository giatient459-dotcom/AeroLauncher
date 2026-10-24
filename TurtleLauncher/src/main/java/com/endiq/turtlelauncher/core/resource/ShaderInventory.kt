package com.endiq.turtlelauncher.core.resource

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans a shader-pack directory without allowing one broken archive to abort the scan. */
class ShaderInventory {
    suspend fun scan(directory: File): List<LocalResource> = withContext(Dispatchers.IO) {
        ResourcePackFiles.listCandidates(directory).mapNotNull { file ->
            if (!ResourcePackFiles.isValidShaderPack(file)) return@mapNotNull null
            runCatching {
                LocalResource(
                    id = file.canonicalFile.path,
                    type = ResourceType.SHADER_PACK,
                    name = file.nameWithoutExtension,
                    file = file,
                    isDirectory = file.isDirectory
                )
            }.getOrNull()
        }
    }
}
