import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.video.mpeg.*

class MainJSMpegScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val image = image(Bitmaps.transparent)
        val data = resourcesVfs["blade-runner-2049-360p-5sec.mpeg1"].openInputStream()
        //val data = resourcesVfs["blade-runner-2049-1080p.ts.mpeg"].openInputStream()
        val player = JSMpegPlayer(coroutineContext)

        player.onDecodedVideoFrame.add {
            it.bitmap.lock {}

            //println("player.video.decodedTime=${player.video.decodedTime}, player.demuxer.currentTime=${player.demuxer.currentTime}, player.lastVideoTime=${player.lastVideoTime}")
            image.bitmap = it.bitmap.slice()
        }

        player.setStream(data)
        //addUpdater { player.frameSync() }

        launchImmediately {
            while (true) {
                player.frame()
            }
        }
    }
}
