package expo.modules.morzexvless

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class MorzexVlessModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("MorzexVless")

        Function("getStatus") {
            mapOf(
                "available" to false,
                "connected" to false,
                "message" to "VLESS core is not installed"
            )
        }
    }
}
