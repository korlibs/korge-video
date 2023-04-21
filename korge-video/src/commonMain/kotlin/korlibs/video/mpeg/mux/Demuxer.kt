package korlibs.video.mpeg.mux

import korlibs.memory.Uint8Buffer
import korlibs.video.mpeg.stream.DecoderBase

/**
 * A Demuxer may sit between a Source and a Decoder. It separates the
 * incoming raw data into Video, Audio and other Streams.
 */
interface Demuxer {
    fun connect(streamId: Int, destinationNode: DecoderBase<*>)
    fun write(buffer: Uint8Buffer)
    /** In seconds */
    val currentTime: Double
    /** In seconds */
    val startTime: Double
}
