package ly.kite.socialmedia.instagramphotopicker

import ly.kite.socialmedia.common.DeviceManager.deleteFileAndFolderFromInternalStorage
import ly.kite.socialmedia.common.DeviceManager.deleteFileAndFolderFromAppCache
import ly.kite.socialmedia.common.UIUtil.showProgressDialogWithText
import ly.kite.socialmedia.common.UIUtil.dismissDialog
import android.os.Parcelable
import android.os.Parcel
import ly.kite.socialmedia.instagramphotopicker.InstagramPhoto
import android.content.SharedPreferences
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker
import android.content.Intent
import ly.kite.socialmedia.common.DeviceManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent
import android.app.Activity
import android.os.AsyncTask
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest.InstagramMediaRequestListener
import ly.kite.socialmedia.common.UIUtil
import kotlin.Throws
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest.InstagramMediaIdRequestListener
import org.json.JSONArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ProgressBar
import android.os.Bundle
import ly.kite.socialmedia.R
import ly.kite.socialmedia.instagramphotopicker.InstagramLoginActivity
import android.webkit.WebSettings
import android.webkit.WebViewClient
import java.lang.Exception

/**
 * Created by deon on 03/08/15.
 */
class InstagramPhotoPickerException(val code: Int, detailsMessage: String?) :
    Exception(detailsMessage) {

    companion object {
        const val CODE_GENERIC_NETWORK_EXCEPTION = 0
        const val CODE_INVALID_ACCESS_TOKEN = 1
    }
}