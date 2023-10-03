import korlibs.time.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.util.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.video.*
import kotlinx.coroutines.*

class MainVideoScene : ScaledScene(1280, 720) {
    override suspend fun SContainer.sceneMain() {
        //addUpdaterOnce {
        val view = korviView(this@MainVideoScene, resourcesVfs["video.mp4"])
        if (Platform.isJs) {
            val text = text("Click to start playing the video...")
            mouse.click.once {
                text.removeFromParent()
                view.play()
            }
        } else {
            view.play()
        }
        //}
    }


    fun View.addUpdaterOnce(block: () -> Unit) {
        var cancellable: Cancellable? = null
        cancellable = addUpdater {
            cancellable?.cancel()
            block()
        }
    }

    inline fun Container.korviView(coroutineScope: CoroutineScope, video: KorviVideo, callback: KorviView.() -> Unit = {}): KorviView = KorviView(coroutineScope, video).also { addChild(it) }.also { callback(it) }
    suspend inline fun Container.korviView(coroutineScope: CoroutineScope, video: VfsFile, autoPlay: Boolean = true, callback: KorviView.() -> Unit = {}): KorviView = KorviView(coroutineScope, video, autoPlay).also { addChild(it) }.also { callback(it) }
    class KorviView(val coroutineScope: CoroutineScope, val video: KorviVideo) : BaseImage(Bitmaps.transparent), AsyncCloseable, BaseKorviSeekable by video {
        val onPrepared = Signal<Unit>()
        val onCompleted = Signal<Unit>()
        var autoLoop = true

        companion object {
            suspend operator fun invoke(coroutineScope: CoroutineScope, file: VfsFile, autoPlay: Boolean = true): KorviView {
                return KorviView(coroutineScope, KorviVideo(file)).also {
                    if (autoPlay) {
                        it.play()
                    }
                }
            }
        }

        private var _prepared: Boolean = false

        private suspend fun ensurePrepared() {
            if (!_prepared) {
                onPrepared.waitOne()
            }
        }

        override fun renderInternal(ctx: RenderContext) {
            if (!_prepared) {
                video.prepare()
                _prepared = true
                onPrepared()
            } else {
                video.render()
            }
            super.renderInternal(ctx)
        }

        val elapsedTime: TimeSpan get() = video.elapsedTime
        val elapsedTimeHr: TimeSpan get() = video.elapsedTimeHr

        fun play() {
            if (video.running) return
            coroutineScope.launchImmediately {
                ensurePrepared()
                //try {
                video.play()
                //} finally {
                //    video.stop()
                //}
            }
        }

        private var bmp = Bitmap32(1, 1)

        init {
            coroutineScope.launchImmediately {
                try {
                    while (true) delay(100.seconds)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    video.stop()
                }
            }
            addUpdater {
                if (video.running) invalidateRender()
            }
            video.onVideoFrame {
                //println("VIDEO FRAME! : ${it.position.timeSpan},  ${it.duration.timeSpan}")
                if (Platform.isJs || Platform.isAndroid) {
                    //if (false) {
                    bitmap = it.data.slice()
                    //println(it.data)
                } else {
                    val itData = it.data.toBMP32IfRequired()
                    //println("itData: $itData: ${it.data.width}, ${it.data.height}")
                    if (bmp.width != itData.width || bmp.height != itData.height) {
                        bmp = Bitmap32(itData.width, itData.height)
                        bitmap = bmp.slice()
                    }

                    if (!itData.ints.contentEquals(bmp.ints)) {
                        bmp.lock {
                            arraycopy(itData.ints, 0, bmp.ints, 0, bmp.area)
                        }
                    }
                }
            }
            video.onComplete {
                coroutineScope.launchImmediately {
                    if (autoLoop) {
                        seekFrame(0L)
                        video.play()
                    }
                }
            }
        }
    }
}
