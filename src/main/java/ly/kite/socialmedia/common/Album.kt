package ly.kite.socialmedia.common

import java.io.Serializable

/**
 * Created by ITDIVISION\maximess139 on 8/2/18.
 */
class Album(var albumId: String, var albumName: String, var count: Int, var coverUri: String) :
    Serializable