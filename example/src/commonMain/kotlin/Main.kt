import korlibs.korge.*
import korlibs.korge.scene.*

suspend fun main() = Korge {
    sceneContainer().changeTo({ MainVideoScene() })
    //sceneContainer().changeTo({ MainJSMpegScene() })
}
