package korlibs.video

import korlibs.datastructure.Extra
import korlibs.time.PerformanceCounter
import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.time.nanoseconds
import korlibs.audio.sound.AudioData
import korlibs.audio.sound.PlatformAudioOutput
import korlibs.image.annotation.KorimExperimental
import korlibs.image.bitmap.*
import korlibs.io.async.AsyncCloseable
import korlibs.io.async.Signal
import korlibs.io.async.delay
import korlibs.io.async.launchImmediately
import korlibs.io.file.VfsFile
import korlibs.io.stream.AsyncStream
import korlibs.math.geom.Matrix4
import korlibs.math.geom.slice.*
import korlibs.video.internal.korviInternal
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.native.concurrent.ThreadLocal

/** Associated transformation matrix that can be used by other engines. Experimental. Might be defined later as an extrinsic function where required. */
@KorimExperimental
@Deprecated("Not used in korge anymore. Use BmpCoords.transformed(Matrix3D) to get texture coordinates transformed")
@ThreadLocal
var Bitmap.transformMat: Matrix4 by Extra.Property { Matrix4() }

// Simple function to be used from swift, js or java if required
@KorimExperimental
@Deprecated("Not used in korge anymore. Use BmpCoords.transformed(Matrix3D) to get texture coordinates transformed")
fun KorviSetBitmapTransformMat(bitmap: Bitmap, transformMat: Matrix4) {
    bitmap.transformMat = transformMat
}

suspend fun KorviVideo(file: VfsFile): KorviVideo = korviInternal.createHighLevel(file)
fun KorviVideoLL(stream: AsyncStream): KorviVideoLL = korviInternal.createContainer(stream)

open class KorviVideo : BaseKorviSeekable {
    class Frame(
        var coords: BitmapCoords,
        val position: TimeSpan,
        val duration: TimeSpan,
    ) : Extra by Extra.Mixin() {
        @Deprecated("This doesn't have into account the transformed coordinates")
        val data get() = coords.base

        companion object {
            operator fun invoke(
                data: BitmapCoords,
                position: TimeSpan,
                duration: TimeSpan,
                transform: Matrix4
            ): Frame = Frame(data.transformed(transform), position, duration)

            // Soft deprecation of this
            operator fun invoke(
                data: Bitmap,
                position: TimeSpan,
                duration: TimeSpan,
                transform: Matrix4 = data.transformMat
            ): Frame = Frame(data.slice(), position, duration, transform)
        }
    }

    val onVideoFrame = Signal<Frame>()
    val onComplete = Signal<Unit>()
    open val running: Boolean = false
    open val elapsedTimeHr: TimeSpan get() = 0.milliseconds
    val elapsedTime: TimeSpan get() = elapsedTimeHr
    open fun prepare(): Unit = Unit
    open fun render(): Unit = Unit
    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): TimeSpan? = null
    open suspend fun play(): Unit = Unit
    open suspend fun pause(): Unit = Unit
    override suspend fun seekFrame(frame: Long): Unit = Unit
    override suspend fun seek(time: TimeSpan): Unit = Unit
    open suspend fun stop(): Unit = Unit
    override suspend fun close(): Unit = Unit
}

open class KorviVideoFromLL(val ll: KorviVideoLL) : KorviVideo() {
    override suspend fun getTotalFrames(): Long? = ll.getTotalFrames()
    override suspend fun getDuration(): TimeSpan? = ll.getDuration()

    override suspend fun seekFrame(frame: Long) = ll.seekFrame(frame)
    override suspend fun seek(time: TimeSpan) = ll.seek(time)

    override suspend fun close() {
        stop()
        ll.close()
    }

    override suspend fun stop() {
        videoJob?.cancel()
        audioJob?.cancel()
        audioStream?.stop()
        audioStream?.dispose()
        running = false
        videoJob = null
        audioJob = null
        audioStream = null
    }

    var videoJob: Job? = null
    var audioJob: Job? = null
    var audioStream: PlatformAudioOutput? = null

    override var running: Boolean = false
    override var elapsedTimeHr: TimeSpan = 0.nanoseconds

    override suspend fun play() {
        if (videoJob != null || audioJob != null) return

        running = true
        //audioStream = nativeSoundProvider.createAudioStream()

        videoJob = launchImmediately(coroutineContext) {
            val frames = arrayListOf<KorviVideoFrame>()
            val startTime = PerformanceCounter.reference
            var completed = false
            do {
                //ll.streams.forEach { stream ->
                //}
                //println("V[1] : ${frames.size}")
                //delay(100.milliseconds)
                if (frames.size <= 3) {
                    //var frame: KorviVideoFrame? = null
                    //ll.streams.fastForEach { s ->
                    //    println("S[0]")
                    //    val f = s.readFrame()
                    //    println("S[1]: $f")
                    //    if (f is KorviVideoFrame) {
                    //        frame = f
                    //    }
                    //}
                    val stream = ll.video.firstOrNull()
                    val frame = stream?.readFrame()
                    if (frame != null) {
                        //println("FRAME: ${frame!!.frame} : ${frame!!.position.millisecondsInt}")
                        frames.add(frame!!)
                        frames.sortBy { it.position.nanoseconds }
                    } else {
                        completed = true
                    }
                } else {
                    //delay(1.milliseconds)
                }
                val elapsedTime = PerformanceCounter.reference - startTime
                if (frames.size >= 3 || (completed && frames.isNotEmpty())) {
                    val frame = frames.removeAt(0)
                    val startTime = PerformanceCounter.reference
                    onVideoFrame(Frame(frame.data, frame.position, frame.duration))
                    val elapsedTime = PerformanceCounter.reference - startTime
                    val time = (frame.duration - elapsedTime)
                    delay(if (time < 1.milliseconds) 1.milliseconds else time)
                    /*
                    var lastFrame: KorviVideoFrame? = null
                    while (frames.isNotEmpty()) {
                        val frame = frames[0]
                        //println("FRAME: ${frame.position} : $elapsedTime")
                        if (elapsedTime >= frame.position) {
                            frames.removeAt(0)
                            lastFrame = frame
                        } else {
                            break
                        }
                    }
                    //println(currentThreadId)
                    if (lastFrame != null) {
                        val frame = lastFrame
                        elapsedTimeHr = frame.position
                        //println("${elapsedTimeHr.timeSpan}")
                        onVideoFrame(Frame(frame.data, frame.position, frame.duration))
                        delay(frame.duration.timeSpan)
                        //delay(4.milliseconds)
                    }
                     */
                } else {
                    //delay(4.milliseconds)
                }
                //println("V[2]: ${frame?.position?.millisecondsInt} : ${frame?.duration?.millisecondsInt}")
            } while (frames.isNotEmpty() || !completed)
            //println("COMPLETED")
            videoJob = null
            audioJob = null
            onComplete(Unit)
            running = false
        }
        //audioJob = launchImmediately(coroutineContext) {
        //    try {
        //        do {
        //            println("A[1]")
        //            var frames = 0
        //            val stream = ll.audio.firstOrNull()
        //            //delay(1000.seconds)
        //            val frame = stream?.readFrame()
        //            println("A[2]: ${frame?.position} : ${frame?.duration}")
        //            if (frame != null) {
        //                frames++
        //                audioStream!!.add(frame.data)
        //            }
        //            // 44100 - 1 second, 4410 - 100 milliseconds, 441 - 10 milliseconds
        //            while (audioStream!!.availableSamples > 4410) {
        //                delay(10.milliseconds)
        //            }
        //        } while (frames > 0)
        //    } catch (e: Throwable) {
        //        e.printStackTrace()
        //    }
        //}
    }
}

open class KorviVideoLL() : BaseKorviSeekable {
    open val video: List<KorviVideoStream> = listOf()
    open val audio: List<KorviAudioStream> = listOf()
    val streams: List<BaseKorviStream<out KorviFrame>> by lazy { video + audio }
    final override suspend fun getTotalFrames(): Long? = streams.mapNotNull { it.getTotalFrames() }.maxOrNull()
    final override suspend fun getDuration(): TimeSpan? = streams.mapNotNull { it.getDuration() }.maxOrNull()
    final override suspend fun seekFrame(frame: Long) { for (v in streams) v.seekFrame(frame) }
    final override suspend fun seek(time: TimeSpan) { for (v in streams) v.seek(time) }
    override suspend fun close() = Unit
}

interface BaseKorviSeekable : AsyncCloseable {
    suspend fun getTotalFrames(): Long? = null
    suspend fun getDuration(): TimeSpan? = null
    suspend fun seekFrame(frame: Long): Unit = TODO()
    suspend fun seek(time: TimeSpan): Unit = TODO()
    override suspend fun close() = Unit
}
suspend fun BaseKorviSeekable.seek(time: TimeSpan): Unit = seek(time)

interface BaseKorviStream<T : KorviFrame> : BaseKorviSeekable {
    suspend fun readFrame(): T? = TODO()
}

typealias KorviVideoStream = BaseKorviStream<KorviVideoFrame>
typealias KorviAudioStream = BaseKorviStream<KorviAudioFrame>

interface KorviFrame {
    val frame: Long
    val position: TimeSpan
    val duration: TimeSpan
    val end: TimeSpan get() = position + duration
}
data class KorviAudioFrame(val data: AudioData, override val frame: Long, override val position: TimeSpan, override val duration: TimeSpan) : KorviFrame
data class KorviVideoFrame(val dataGen: () -> Bitmap32, override val frame: Long, override val position: TimeSpan, override val duration: TimeSpan) : KorviFrame {
    val data by lazy { dataGen() }
}
