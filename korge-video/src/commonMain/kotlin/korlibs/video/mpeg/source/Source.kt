package korlibs.video.mpeg.source

import korlibs.video.mpeg.mux.Demuxer

interface Source {
    fun connect(demuxer: Demuxer)
}
