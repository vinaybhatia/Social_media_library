package ly.kite.socialmedia.common

import android.graphics.drawable.ColorDrawable
import ly.kite.socialmedia.R
import ly.kite.socialmedia.common.LoadingView
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import ly.kite.socialmedia.common.DeviceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.os.Build
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences

/**
 * Created by ITDIVISION\maximess139 on 7/2/18.
 */
object Constant {
    const val FACEBOOK_PHOTOS = "FACEBOOK_PHOTOS"
    const val FACEBOOK_ALBUMS = "FACEBOOK_ALBUMS"
    const val FACEBOOK_PHOTO_ID = "FACEBOOK_PHOTO_ID"
    const val SOCIAL_MEDIA_ALBUM_NAME = "SOCIAL_MEDIA_ALBUM_NAME"
    const val GOOGLEMEDIAID = "googlemediaid"
    const val SOCIAL_MEDIA_ALBUM_ID = "SOCIAL_MEDIA_ALBUM_ID"
    const val FACEBOOK_DIRECTORY_NAME = "/DCIM/SnapTouchImages/Facebook"
    const val FACEBOOK_THUMBNAIL = "/DCIM/thumbnails"
    const val DIRECTORY_NAME = "/DCIM/SnapTouchImages"
    const val INSTAGRAM_DIRECTORY_NAME = "/DCIM/SnapTouchImages/Instagram"
    const val GOOGLE_DIRECTORY_NAME = "/DCIM/SnapTouchImages/Google"
    const val GOOGLE_JSON_DIRECTORY_NAME = "/DCIM/SnapTouchImages/Google/Json"
    const val FACEBOOK = "Facebook"
    const val NEWEST_FIRST = "NEWEST_FIRST"
    const val OLDEST_FIRST = "OLDEST_FIRST"
    const val SOCIAL_MEDIA_ALBUM_FACEBOOK_ID = "SOCIAL_MEDIA_ALBUM_FACEBOOK_ID"
    const val REQUEST_GOOGLE_PHOTOS = 100
    const val SOCIAL_MEDIA_PAGE_NEXT_COUNT = "SOCIAL_MEDIA_PAGE_NEXT_COUNT"
}