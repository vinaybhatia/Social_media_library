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
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent
import android.app.Activity
import android.content.Context
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
import android.util.Log
import android.webkit.*
import ly.kite.socialmedia.R
import ly.kite.socialmedia.instagramphotopicker.InstagramLoginActivity
import ly.kite.socialmedia.common.Constant
import java.util.*

/**
 * Created by deon on 28/07/15.
 */
class InstagramPhotoPicker {
    fun getUserName(context: Context): String? {
        val applicationContext = context.applicationContext
        val preferences =
            applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        return preferences.getString(PREFERENCE_FB_USER_NAME, "")
    }

    companion object {
        const val EXTRA_SELECTED_PHOTOS = "ly.kite.instagramphotopicker.EXTRA_SELECTED_PHOTOS"
        const val PREFERENCE_FILE = "ly.kite.instagramphotopicker.PREFERENCE_FILE"
        const val PREFERENCE_ACCESS_TOKEN = "ly.kite.instagramphotopicker.PREFERENCE_ACCESS_TOKEN"
        const val PREFERENCE_FB_ACCESS_TOKEN =
            "ly.kite.instagramphotopicker.PREFERENCE_FB_ACCESS_TOKEN"
        const val PREFERENCE_INSTA_USER_NAME =
            "ly.kite.instagramphotopicker.PREFERENCE_INSTA_USER_NAME"
        const val PREFERENCE_FB_USER_NAME = "ly.kite.instagramphotopicker.PREFERENCE_FB_USER_NAME"
        const val PREFERENCE_INSTA_USER_ID = "ly.kite.instagramphotopicker.PREFERENCE_INSTA_USER_ID"
        const val PREFERENCE_GOOGLE_ACCESS_TOKEN =
            "ly.kite.instagramphotopicker.PREFERENCE_GOOGLE_ACCESS_TOKEN"
        const val PREFERENCE_DROPBOX_TOKEN = "ly.kite.instagramphotopicker.PREFERENCE_DROPBOX_TOKEN"
        const val PREFERENCE_FB_USER_ID = "ly.kite.instagramphotopicker.PREFERENCE_FB_USER_ID"
        const val PREFERENCE_GOOGLE_USER_ID =
            "ly.kite.instagramphotopicker.PREFERENCE_GOOGLE_USER_ID"
        const val PREFERENCE_GOOGLE_USER_NAME =
            "ly.kite.instagramphotopicker.PREFERENCE_GOOGLE_USER_NAME"
        const val PREFERENCE_CLIENT_ID = "ly.kite.instagramphotopicker.PREFERENCE_CLIENT_ID"
        const val PREFERENCE_REDIRECT_URI = "ly.kite.instagramphotopicker.PREFERENCE_REDIRECT_URI"
        const val PREFERENCE_LOGIN_DATE = "ly.kite.instagramphotopicker.PREFERENCE_LOGIN_DATE"
        var cachedAccessToken: String? = null
        var cachedFbAccessToken: String? = null
        var cachedClientId: String? = null
        var cachedRedirectUri: String? = null
        var cachedLoginDate: Long? = null
        var cachedDropboxToken: String? = null
        var cachedGoogleTokenID: String? = null
        fun getResultPhotos(data: Intent): Array<InstagramPhoto?> {
            val photos = data.getParcelableArrayExtra(EXTRA_SELECTED_PHOTOS)
            val instagramPhotos = arrayOfNulls<InstagramPhoto>(
                photos!!.size
            )
            System.arraycopy(photos, 0, instagramPhotos, 0, photos.size)
            return instagramPhotos
        }

        fun getAccessToken(context: Context): String? {
            if (cachedAccessToken == null) {
                loadInstagramPreferences(context)
            }
            return cachedAccessToken
        }

        @JvmStatic
        fun getFbAccessToken(context: Context): String? {
            if (cachedFbAccessToken == null) {
                loadFbPreferences(context)
            }
            return cachedFbAccessToken
        }

        fun getClientId(context: Context): String? {
            if (cachedClientId == null) {
                loadInstagramPreferences(context)
            }
            return cachedClientId
        }

        fun getRedirectUri(context: Context): String? {
            if (cachedRedirectUri == null) {
                loadInstagramPreferences(context)
            }
            return cachedRedirectUri
        }

        private fun loadInstagramPreferences(context: Context) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            cachedAccessToken = preferences.getString(PREFERENCE_ACCESS_TOKEN, null)
            cachedClientId = preferences.getString(PREFERENCE_CLIENT_ID, null)
            cachedRedirectUri = preferences.getString(PREFERENCE_REDIRECT_URI, null)
        }

        fun saveInstagramPreferences(
            context: Context?,
            accessToken: String?,
            clientId: String?,
            redirectURI: String?
        ) {
            val applicationContext = context!!.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_ACCESS_TOKEN, accessToken)
            editor.putString(PREFERENCE_CLIENT_ID, clientId)
            editor.putString(PREFERENCE_REDIRECT_URI, redirectURI)
            val c = Calendar.getInstance().time
            editor.putLong(PREFERENCE_LOGIN_DATE, c.time)
            editor.commit()
            cachedAccessToken = accessToken
            cachedClientId = clientId
            cachedLoginDate = c.time
            cachedRedirectUri = redirectURI
        }

        @JvmStatic
        fun saveFbTokenToPreferences(context: Context, accessToken: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_FB_ACCESS_TOKEN, accessToken)
            editor.commit()
            cachedFbAccessToken = accessToken
        }

        @JvmStatic
        fun saveFbUserNameToPreferences(context: Context, username: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_FB_USER_NAME, username)
            Log.d(">>>>fbUsername", username!!)
            editor.commit()
        }

        fun loadFbPreferences(context: Context) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            cachedFbAccessToken = preferences.getString(PREFERENCE_FB_ACCESS_TOKEN, null)
        }

        fun logoutInsta(context: Context) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.remove(PREFERENCE_ACCESS_TOKEN)
            editor.remove(PREFERENCE_LOGIN_DATE)
            editor.remove(PREFERENCE_CLIENT_ID)
            editor.remove(PREFERENCE_REDIRECT_URI)
            editor.remove(PREFERENCE_INSTA_USER_NAME)
            editor.remove(PREFERENCE_INSTA_USER_ID)
            editor.commit()
            cachedAccessToken = null
            cachedClientId = null
            cachedRedirectUri = null
            cachedLoginDate = null
            deleteFileAndFolderFromInternalStorage(context, Constant.INSTAGRAM_DIRECTORY_NAME)
            deleteFileAndFolderFromAppCache(context, Constant.INSTAGRAM_DIRECTORY_NAME)
            CookieSyncManager.createInstance(context)
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            WebView(context).clearCache(true)
        }

        fun logoutFb(context: Context) {
            val agent = FacebookAgent.getInstance(context as Activity)
            FacebookAgent.logout()
            val applicationContext = context.getApplicationContext()
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.remove(PREFERENCE_FB_ACCESS_TOKEN)
            editor.remove(PREFERENCE_FB_USER_NAME)
            editor.remove(PREFERENCE_FB_USER_ID)
            editor.commit()
            cachedFbAccessToken = null
            deleteFileAndFolderFromInternalStorage(context, Constant.FACEBOOK_DIRECTORY_NAME)
            deleteFileAndFolderFromAppCache(context, Constant.FACEBOOK_DIRECTORY_NAME)
        }

        @JvmStatic
        fun saveFbUserIdToPreferences(context: Context, userId: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_FB_USER_ID, userId)
            editor.commit()
        }

        fun getDropboxAccessToken(context: Context): String? {
            if (cachedDropboxToken == null) {
                loadDropboxPreferences(context)
            }
            return cachedDropboxToken
        }

        fun getGoogleTokenID(context: Context): String? {
            if (cachedGoogleTokenID == null) {
                loadGooglePreferences(context)
            }
            return cachedGoogleTokenID
        }

        fun loadGooglePreferences(context: Context) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            cachedGoogleTokenID = preferences.getString(PREFERENCE_GOOGLE_ACCESS_TOKEN, null)
        }

        private fun loadDropboxPreferences(context: Context) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            cachedDropboxToken = preferences.getString(PREFERENCE_DROPBOX_TOKEN, null)
        }

        fun saveDropboxTokenToPreferences(context: Context, accessToken: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_DROPBOX_TOKEN, accessToken)
            editor.commit()
            cachedDropboxToken = accessToken
        }

        @JvmStatic
        fun saveGoogleTokenIDToPreferences(context: Context, tokenID: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_GOOGLE_ACCESS_TOKEN, tokenID)
            editor.commit()
            cachedGoogleTokenID = tokenID
        }

        @JvmStatic
        fun saveGoogleUserNameToPreferences(context: Context, username: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_GOOGLE_USER_NAME, username)
            Log.d(">>>>GoogleName", username!!)
            editor.commit()
        }

        @JvmStatic
        fun saveGoogleUserIdToPreferences(context: Context, userId: String?) {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(PREFERENCE_GOOGLE_USER_ID, userId)
            Log.d(">>>>GoogleName", userId!!)
            editor.commit()
        }

        fun getFbUserID(context: Context): String? {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            return preferences.getString(PREFERENCE_FB_USER_ID, "")
        }

        fun getFbUserName(context: Context): String? {
            val applicationContext = context.applicationContext
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            return preferences.getString(PREFERENCE_FB_USER_NAME, "")
        }
    }
}