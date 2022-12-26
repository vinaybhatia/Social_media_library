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

/**
 * Created by deon on 03/08/15.
 */
class InstagramPhoto : Parcelable {
    val thumbnailURL: String?
    val fullURL: String?

    constructor(thumbURL: String?, fullURL: String?) {
        thumbnailURL = thumbURL
        this.fullURL = fullURL
    }

    constructor(`in`: Parcel) {
        thumbnailURL = `in`.readString()
        fullURL = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(thumbnailURL)
        dest.writeValue(fullURL)
    }

    override fun hashCode(): Int {
        var v = 17
        v = v * 31 + thumbnailURL.hashCode()
        v = v * 31 + fullURL.hashCode()
        return v
    }

    override fun equals(o: Any?): Boolean {
        if (o !is InstagramPhoto) {
            return false
        }
        val photo = o
        return photo.thumbnailURL == thumbnailURL && photo.fullURL == fullURL
    }

    companion object {
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<Any?> {
            override fun createFromParcel(`in`: Parcel): InstagramPhoto? {
                return InstagramPhoto(`in`)
            }

            override fun newArray(size: Int): Array<InstagramPhoto?> {
                return arrayOfNulls(size)
            }
        }
    }
}