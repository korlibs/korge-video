package korlibs.video.internal

import korlibs.time.minutes
import korlibs.io.file.VfsFile
import korlibs.io.stream.AsyncStream
import korlibs.video.DummyKorviVideoLL
import korlibs.video.KorviVideo
import korlibs.video.KorviVideoFromLL
import korlibs.video.KorviVideoLL

internal expect val korviInternal: KorviInternal

internal open class KorviInternal {
    open suspend fun createHighLevel(file: VfsFile): KorviVideo = KorviVideoFromLL(createContainer(file.open()))
    open fun createContainer(stream: AsyncStream): KorviVideoLL = DummyKorviVideoLL(3.minutes)
}
