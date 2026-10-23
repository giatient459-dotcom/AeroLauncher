package com.endiq.turtlelauncher.launch

/** Central Java-major compatibility check shared by runtime selection and installation. */
object JavaRuntimePolicy {
    /**
     * A runtime is eligible only when its parsed Java major is known and at least the
     * Minecraft metadata requirement. Unknown/broken runtimes (major 0) are not eligible.
     */
    @JvmStatic
    fun isCompatible(installedJavaMajor: Int, requiredJavaMajor: Int): Boolean =
        requiredJavaMajor > 0 && installedJavaMajor >= requiredJavaMajor
}
