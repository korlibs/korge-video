package korlibs.video

import korlibs.time.hr.timeSpan
import korlibs.time.minutes
import korlibs.time.seconds
import korlibs.image.awt.awtShowImageAndWait
import kotlinx.coroutines.runBlocking

object KorviJvm {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            //val video = KorviVideo(rootLocalVfs["C:/tmp/dw11222.mp4"].open())
            val video = DummyKorviVideoLL(3.minutes)
            val duration = video.getDuration()!!.timeSpan
            println(duration)
            println((duration * 0.5).seconds)
            //container.seek((duration * 0.5))
            video.seek(121.seconds)
            val frame = video.video.first().readFrame()
            awtShowImageAndWait(frame!!.data)
        }
    }
}
