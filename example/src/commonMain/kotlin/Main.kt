import com.soywiz.korge.*
import com.soywiz.korge.scene.*

suspend fun main() = Korge {
    sceneContainer().changeTo({ MainVideoScene() })
    //sceneContainer().changeTo({ MainJSMpegScene() })
}
