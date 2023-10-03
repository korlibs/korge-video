package korlibs.video

import korlibs.audio.sound.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.math.geom.*
import korlibs.time.*
import korlibs.time.seconds
import korlibs.time.toTimeString
import kotlin.time.*

class DummyKorviVideoLL(
    val totalFrames: Long,
    val timePerFrame: TimeSpan,
    val width: Int = 320,
    val height: Int = 240
) : KorviVideoLL() {
    companion object {
        operator fun invoke(
            time: TimeSpan = 60.seconds,
            fps: Number = 24,
            width: Int = 320,
            height: Int = 240
        ) : KorviVideoLL {
            val timePerFrame = 1.seconds * (1 / fps.toDouble())
            return DummyKorviVideoLL((time / timePerFrame).toLong(), timePerFrame, width, height)
        }
    }
    override val video: List<KorviVideoStream> = listOf(DummyKorviVideoStream(this))
    override val audio: List<KorviAudioStream> = listOf(DummyKorviAudioStream(this))

    override suspend fun close() {
    }

    // https://youtrack.jetbrains.com/issue/KT-46214
    open class DummyBaseStream<TFrame : KorviFrame>(val base: DummyKorviVideoLL) : BaseKorviStream<TFrame> {
        var currentFrame = 0L

        override suspend fun getTotalFrames(): Long? = base.totalFrames
        override suspend fun getDuration(): TimeSpan? = base.timePerFrame * base.totalFrames.toDouble()
        override suspend fun seekFrame(frame: Long) { currentFrame = frame }
        override suspend fun seek(time: TimeSpan) { seekFrame((time / base.timePerFrame).toLong()) }
    }

    class DummyKorviVideoStream(base: DummyKorviVideoLL) : DummyBaseStream<KorviVideoFrame>(base) {
        override suspend fun readFrame(): KorviVideoFrame? {
            if (currentFrame >= base.totalFrames) return null
            val frame = currentFrame++
            val currentTime: Duration = base.timePerFrame * frame.toDouble()
            val data = NativeImage(base.width, base.height)
            data.context2d {
                fill(Colors.DARKGREEN) {
                    fillRect(0, 0, base.width, base.height)
                }
                fillText(
                    currentTime.toTimeString(),
                    pos = Point(base.width * 0.5, base.height * 0.5),
                    color = Colors.WHITE,
                    font = SystemFont("Arial"),
                    size = 32f,
                    align = TextAlignment.CENTER,
                )
            }
            return KorviVideoFrame({ data.toBMP32() }, frame, base.timePerFrame * frame.toDouble(), base.timePerFrame)
        }
    }

    class DummyKorviAudioStream(base: DummyKorviVideoLL) : DummyBaseStream<KorviAudioFrame>(base) {
        override suspend fun readFrame(): KorviAudioFrame? {
            if (currentFrame >= base.totalFrames) return null
            val frame = currentFrame++
            val data = AudioData(44100, AudioSamples(2, (44100 * base.timePerFrame.seconds).toInt()))
            return KorviAudioFrame(data, frame, base.timePerFrame * frame.toDouble(), base.timePerFrame)
        }
    }
}
