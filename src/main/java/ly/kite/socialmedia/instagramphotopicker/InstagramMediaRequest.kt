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
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest.MediaResponse
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest.MediaIdResponse
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
import android.text.TextUtils
import android.util.Log
import ly.kite.socialmedia.R
import ly.kite.socialmedia.instagramphotopicker.InstagramLoginActivity
import android.webkit.WebSettings
import android.webkit.WebViewClient
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Created by deon on 03/08/15.
 */
class InstagramMediaRequest : Parcelable {
    private val baseURL: String?
    private val TAG = "InstagramMediaRequest"
    private var position: String? = null
    private var showLoader = false
    private var requestTask: AsyncTask<Void?, Void?, MediaResponse?>? = null
    private var mediaIdRequestTask: AsyncTask<Void?, Void?, MediaIdResponse?>? = null

    constructor() {
        baseURL = MEDIA_URL_ENDPOINT
    }

    constructor(url: String?) {
        baseURL = url
    }

    constructor(position: String?, showLoader: Boolean) {
        this.position = position
        this.showLoader = showLoader
        baseURL = MEDIA_URL_ENDPOINT
    }

    constructor(`in`: Parcel) {
        baseURL = `in`.readString()
    }

    fun getMedia(
        mAccessToken: String,
        position: String?,
        redirectUri: String?,
        mediaId: ArrayList<String>,
        listener: InstagramMediaRequestListener,
        ctx: Context?,
        isToShowLoaders: Boolean
    ) {
        requestTask = object : AsyncTask<Void?, Void?, MediaResponse?>() {
            private var context: Context? = null
            override fun onPreExecute() {
                super.onPreExecute()
                context = ctx
                Log.d(TAG, "onPreExecute: $position")
                if (TextUtils.isEmpty(getPosition()) && getPosition() == null && position != null) {
                    if (!TextUtils.isEmpty(position) && position != null && position.equals(
                            "0",
                            ignoreCase = true
                        ) && isToShowLoaders
                    ) {
                        dialogGetMedia = showProgressDialogWithText(context)
                    }

//                } else {
//                    Log.d(TAG, "onPreExecute: "+getPosition() +"isSHowLoader"+isShowLoader());
//                    if (getPosition().equalsIgnoreCase("0") && isShowLoader()) {
//                        dialogGetMedia = UIUtil.showProgressDialogWithText(context);
//
//                    }
                }
            }



            @Throws(IOException::class, JSONException::class)
            private fun getMediaPhotos(
                mediaResponse: MediaResponse,
                accessToken: String,
                photos: ArrayList<InstagramPhoto>,
                mediaId: ArrayList<String>
            ) {
                var count = 0
                Log.d(TAG, "mediaID.size()==" + mediaId.size + "")
                for (j in mediaId.indices) {
                    count++
                    val urlImage =
                        URL("https://graph.instagram.com/" + mediaId[j] + "?fields" + "=media_type,media_url" + "&access_token=" + accessToken)
                    val responseImage = getInstagramUrlResponse(urlImage)
                    val jsonObjImage = JSONTokener(responseImage).nextValue() as JSONObject
                    val media_type = jsonObjImage.getString("media_type")
                    val media_url = jsonObjImage.getString("media_url")
                    if (media_type.equals("IMAGE", ignoreCase = true)) {
                        addPhotosToList(photos, media_type, media_url, context)
                    } else if (media_type.equals("CAROUSEL_ALBUM", ignoreCase = true)) {
                        getAlbumPhotos(photos, mediaId[j], media_type, context, accessToken)
                    }
                    if (count == 7) {
                        InstaImagIdIndex = InstaImagIdIndex + 7
                        break
                    }
                }
                mediaResponse.ids = mediaId
                if (mediaId.size > 7) {
                    mediaResponse.isLoading = true
                }
                mediaResponse.photos = photos
                mediaResponse.httpStatusCode = 200
            }

            override fun onPostExecute(mediaResponse: MediaResponse?) {
                if (mediaResponse!!.error != null) {
                    listener.onError(mediaResponse.error)
                } else {
//                    Log.d(TAG, "mediaResponse.photos getMedia=" + mediaResponse.photos.size() + "");
                    //  Log.d(TAG, "mediaResponse.ids getMedia=" + mediaResponse.ids.size() + "");
                    listener.onMedia(
                        mediaResponse.photos,
                        mediaResponse.isLoading,
                        mediaResponse.ids,
                        mediaResponse.nextPageRequest
                    )
                }
                Log.d(TAG, "onPostExecute: dismissDialogue" + dialogGetMedia)
                //  dismissDialoag(dialogGetMedia);
            }

            override fun doInBackground(vararg p0: Void?): MediaResponse {
                val mediaResponse = MediaResponse()
                try {
                    //String accessToken =compareDate(context,mAccessToken,clientId,redirectUri);
                    val photos = ArrayList<InstagramPhoto>()
                    Log.d(TAG, "mediaId getMedia=" + mediaId.size + "")
                    getMediaPhotos(mediaResponse, mAccessToken, photos, mediaId)
                } catch (e: Exception) {
                }
                return mediaResponse
            }
        }
        requestTask?.execute()
    }

    fun getMediaId(
        mAccessToken: String,
        clientId: String,
        redirectUri: String,
        listener: InstagramMediaIdRequestListener,
        ctx: Context?,
        isToShowLoader: Boolean
    ) {
        mediaIdRequestTask = object : AsyncTask<Void?, Void?, MediaIdResponse?>() {
            private var context: Context? = null
            override fun onPreExecute() {
                super.onPreExecute()
                context = ctx
                if (isToShowLoader) {
                    dialog = showProgressDialogWithText(context)
                }
            }



            @Throws(IOException::class, JSONException::class)
            private fun getMediaIds(
                mediaResponse: MediaIdResponse,
                accessToken: String,
                url: URL,
                ids: ArrayList<String>
            ) {
                var url = url
                var next = ""
                val response = getInstagramUrlResponse(url)
                val jsonObj = JSONTokener(response).nextValue() as JSONObject
                val jsonArray = jsonObj.getJSONArray("data")
                if (jsonObj.has("paging")) {
                    val pagingObj = jsonObj.getJSONObject("paging")
                    next = if (pagingObj.has("next")) {
                        pagingObj.getString("next")
                    } else {
                        ""
                    }
                }
                if (jsonArray != null) {
                    Log.d(TAG, "jsonArray.length()=" + jsonArray.length() + "")
                    if (jsonArray.length() > 0) {
                        for (j in 0 until jsonArray.length()) {
                            val imageId = jsonArray.getJSONObject(j).getString("id")
                            ids.add(imageId)
                        }
                        Log.d(TAG, "ids.length()=" + ids.size + "")
                    }
                }
                val photos = ArrayList<InstagramPhoto>()
                var count = 0
                if (next == "") {
                    if (ids.size >= 4) {
                        for (j in ids.indices) {
                            count++
                            Log.d(TAG, "ids.size() >= 4=$j")
                            getImaage(accessToken, ids, photos, j)
                            if (count == 4) {
                                InstaImagIdIndex = 4
                                break
                            }
                        }
                    } else if (ids.size >= 3) {
                        for (j in 0 until jsonArray.length()) {
                            Log.d(TAG, "ids.size() >= 3$j")
                            getImaage(accessToken, ids, photos, j)
                        }
                    } else if (ids.size >= 2) {
                        for (j in 0 until jsonArray.length()) {
                            Log.d(TAG, "ids.size() >= 2$j")
                            getImaage(accessToken, ids, photos, j)
                        }
                    } else if (ids.size == 1) {
                        for (j in 0 until jsonArray.length()) {
                            Log.d(TAG, "ids.size() == 1$j")
                            getImaage(accessToken, ids, photos, j)
                        }
                    }
                }
                if (ids.size > 4) {
                    mediaResponse.isLoading = true
                }
                mediaResponse.ids = ids
                mediaResponse.photos = photos
                mediaResponse.accessToken = accessToken
                mediaResponse.httpStatusCode = 200
                if (next != "") {
                    url = URL(next)
                    Log.d(TAG, "next=$next")
                    Log.d(TAG, "url next$url")
                    getMediaIds(mediaResponse, accessToken, url, ids)
                }
            }

            @Throws(JSONException::class, IOException::class)
            private fun getImaage(
                accessToken: String,
                id: ArrayList<String>,
                photos: ArrayList<InstagramPhoto>,
                j: Int
            ) {
                val imageId = id[j]
                Log.d(TAG, "getImaage J = $j")
                Log.d(TAG, "getImaage imageId = $imageId")
                val urlImage =
                    URL("https://graph.instagram.com/$imageId?fields=media_type,media_url,timestamp&access_token=$accessToken")
                val responseImage = getInstagramUrlResponse(urlImage)
                val jsonObjImage = JSONTokener(responseImage).nextValue() as JSONObject
                val media_type = jsonObjImage.getString("media_type")
                val media_url = jsonObjImage.getString("media_url")
                Log.d(TAG, "id=" + id.size + "")
                Log.d(TAG, "photos1=" + photos.size + "")
                Log.d(TAG, "media_type=$media_type")
                if (media_type.equals("IMAGE", ignoreCase = true)) {
                    addPhotosToList(photos, media_type, media_url, context)
                } else if (media_type.equals("CAROUSEL_ALBUM", ignoreCase = true)) {
                    Log.d(TAG, "photos2=" + photos.size + "")
                    getAlbumPhotos(photos, imageId, media_type, context, accessToken)
                }
                Log.d(TAG, "photos3=" + photos.size + "")
            }

            override fun onPostExecute(mediaResponse: MediaIdResponse?) {
                if (mediaResponse!!.error != null) {
                    listener.onError(mediaResponse.error)
                } else {
                    Log.d(TAG, "mediaResponse.photos=" + mediaResponse.photos!!.size + "")
                    Log.d(TAG, "mediaResponse.ids=" + mediaResponse.ids!!.size + "")
                    listener.onMedia(
                        mediaResponse.ids,
                        mediaResponse.photos,
                        mediaResponse.isLoading,
                        mediaResponse.accessToken,
                        mediaResponse.nextPageRequest
                    )
                }

                //  dismissDialoag(dialog);
            }

            override fun doInBackground(vararg p0: Void?): MediaIdResponse {
                val mediaResponse = MediaIdResponse()
                try {
                    val accessToken = compareDate(context, mAccessToken, clientId, redirectUri)
                    val url =
                        URL("https://graph.instagram.com/me/media?fields=id,caption&access_token=$accessToken")
                    Log.d(TAG, "url doInBackground$url")
                    val ids = ArrayList<String>()
                    getMediaIds(mediaResponse, accessToken, url, ids)
                } catch (e: Exception) {
                }
                return mediaResponse
            }
        }
        mediaIdRequestTask?.execute()
    }

    private fun addPhotosToList(
        photos: ArrayList<InstagramPhoto>,
        media_type: String,
        media_url: String,
        context: Context?
    ) {
        if (media_type.equals("IMAGE", ignoreCase = true)) {
//            String standardURL = adjustedURL(media_url, "", context);
            val instagramPhoto = InstagramPhoto(null, media_url)
            photos.add(instagramPhoto)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun getAlbumPhotos(
        photos: ArrayList<InstagramPhoto>,
        imageId: String,
        media_type: String,
        context: Context?,
        accessToken: String
    ) {
        Log.d(TAG, "media_type=$media_type")
        if (media_type.equals("CAROUSEL_ALBUM", ignoreCase = true)) {
            val albumImageUrl =
                URL("https://graph.instagram.com/$imageId/children?fields=media_type,media_url&access_token=$accessToken")
            val albumImageResponse = getInstagramUrlResponse(albumImageUrl)
            val albumJsonObjImage = JSONTokener(albumImageResponse).nextValue() as JSONObject
            val albumJsonArray = albumJsonObjImage.getJSONArray("data")
            Log.d(TAG, "albumJsonArray=" + albumJsonArray.length() + "")
            if (albumJsonArray.length() > 0) {
                for (i in 0 until albumJsonArray.length()) {
                    val album_media_type = albumJsonArray.getJSONObject(i).getString("media_type")
                    val album_media_url = albumJsonArray.getJSONObject(i).getString("media_url")
                    val album_media_id = albumJsonArray.getJSONObject(i).getString("id")
                    if (album_media_type.equals("IMAGE", ignoreCase = true)) {
                        addPhotosToList(photos, album_media_type, album_media_url, context)
                    } else {
                        getAlbumPhotos(
                            photos,
                            album_media_id,
                            album_media_type,
                            context,
                            accessToken
                        )
                    }
                }
            }
        }
    }

    fun cancel() {
        if (requestTask != null) {
            requestTask!!.cancel(true)
            requestTask = null
        }
    }

    fun getPosition(): String? {
        Log.d(TAG, "getPosition: $position")
        return position
    }

    fun setPosition(position: String) {
        this.position = position
        Log.d(TAG, "setPosition: $position")
    }

    fun isShowLoader(): Boolean {
        Log.d(TAG, "isShowLoader: $showLoader")
        return showLoader
    }

    fun setShowLoader(showLoader: Boolean) {
        Log.d(TAG, "setShowLoader: $showLoader")
        this.showLoader = showLoader
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(baseURL)
    }

    @Throws(IOException::class)
    private fun getInstagramUrlResponse(urlImage: URL): String {
        val urlConnectionImage = urlImage.openConnection() as HttpURLConnection
        urlConnectionImage.requestMethod = "GET"
        urlConnectionImage.doInput = true
        urlConnectionImage.connect()
        return streamToString(urlConnectionImage.inputStream)
    }

    @Throws(IOException::class)
    private fun streamToString(`is`: InputStream?): String {
        var str = ""
        if (`is` != null) {
            val sb = StringBuilder()
            var line: String?
            try {
                val reader = BufferedReader(
                    InputStreamReader(`is`)
                )
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
            } finally {
                `is`.close()
            }
            str = sb.toString()
        }
        return str
    }

    private fun compareDate(
        context: Context?,
        accessToken: String,
        clientId: String,
        redirectUri: String
    ): String {
        var accessToken = accessToken
        try {
            val applicationContext = context!!.applicationContext
            val preferences = applicationContext.getSharedPreferences(
                InstagramPhotoPicker.Companion.PREFERENCE_FILE,
                Context.MODE_PRIVATE
            )
            val loginDate =
                preferences.getLong(InstagramPhotoPicker.Companion.PREFERENCE_LOGIN_DATE, 0)
            if (loginDate != null
            ) {
                val c = Calendar.getInstance().time
                val difference = c.time - loginDate
                val secondsInMilli: Long = 1000
                val minutesInMilli = secondsInMilli * 60
                val hoursInMilli = minutesInMilli * 60
                val daysInMilli = hoursInMilli * 24
                val elapsedDays = difference / daysInMilli
                if (elapsedDays > 58) {
                    accessToken = getLongLivedRefreshAccessToken(accessToken)
                    InstagramPhotoPicker.Companion.saveInstagramPreferences(
                        context,
                        accessToken,
                        clientId,
                        redirectUri
                    )
                }
            }
        } catch (e: Exception) {
        }
        return accessToken
    }

    @Throws(IOException::class, JSONException::class)
    private fun getLongLivedRefreshAccessToken(accessToken: String): String {
        var accessToken = accessToken
        val url: URL
        val response: String
        val jsonObj: JSONObject
        url =
            URL("https://graph.instagram.com/refresh_access_token?grant_type=ig_refresh_token&access_token=$accessToken")
        response = getInstagramUrlResponse(url)
        jsonObj = JSONTokener(response).nextValue() as JSONObject
        accessToken = jsonObj.getString("access_token")
        return accessToken
    }

    interface InstagramMediaRequestListener {
        fun onMedia(
            media: ArrayList<InstagramPhoto>?,
            isLoading: Boolean,
            ids: ArrayList<String>?,
            nextPageRequest: InstagramMediaRequest?
        )

        fun onError(error: Exception?)
    }

    interface InstagramMediaIdRequestListener {
        fun onMedia(
            media: ArrayList<String>?,
            photos: ArrayList<InstagramPhoto>?,
            isInstagramLoading: Boolean,
            accessTOken: String?,
            nextPageRequest: InstagramMediaRequest?
        )

        fun onError(error: Exception?)
    }

    private class MediaResponse {
        val error: Exception? = null
        var httpStatusCode = 0
        var photos: ArrayList<InstagramPhoto>? = null
        var ids: ArrayList<String>? = null
        var isLoading = false
        val nextPageRequest: InstagramMediaRequest? = null
    }

    private class MediaIdResponse {
        val error: Exception? = null
        var httpStatusCode = 0
        var ids: ArrayList<String>? = null
        var accessToken: String? = null
        var photos: ArrayList<InstagramPhoto>? = null
        var isLoading = false
        val nextPageRequest: InstagramMediaRequest? = null
    }

    companion object {
        const val USER_INFO_URL = "https://api.instagram.com/v1/users/self"
        const val INSTAGRAM_DIRECTORY_NAME = "/DCIM//SnapTouchImages/Instagram"
        const val INSTAGRAM_THUMBNAIL = "/DCIM//thumbnails"
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<Any?> {
            override fun createFromParcel(`in`: Parcel): InstagramMediaRequest? {
                return InstagramMediaRequest(`in`)
            }

            override fun newArray(size: Int): Array<InstagramMediaRequest?> {
                return arrayOfNulls(size)
            }
        }
        private const val GENERIC_NETWORK_EXCEPTION_MESSAGE =
            "Failed to reach Instagram. Please check your internet connectivity and try again"
        private const val MEDIA_URL_ENDPOINT =
            "https://api.instagram.com/v1/users/self/media/recent"
        var InstaImagIdIndex = 10
        var dialog: Dialog? = null
        var dialogGetMedia: Dialog? = null
        @Throws(JSONException::class)
        private fun parsePhotosFromResponseJSON(
            json: JSONObject,
            context: Context
        ): ArrayList<InstagramPhoto> {
            val photos = ArrayList<InstagramPhoto>()
            val data = json.getJSONArray("data")
            for (i in 0 until data.length()) {
                try {
                    val photoJSON = data.getJSONObject(i)

                    //saveUserNameToPreferences(photoJSON, context);
                    var type = photoJSON.getString("type")
                    if (type.equals("image", ignoreCase = true)) {
                        val images = photoJSON.getJSONObject("images")
                        val photo = getInstagramPhotos(images, context)
                        photos.add(photo)
                    }
                    if (type.equals("carousel", ignoreCase = true)) {
                        val carouselMedia = photoJSON.getJSONArray("carousel_media")
                        for (j in 0 until carouselMedia.length()) {
                            val carouselPhoto = carouselMedia.getJSONObject(j)
                            type = carouselPhoto.getString("type")
                            if (type.equals("image", ignoreCase = true)) {
                                val images = carouselPhoto.getJSONObject("images")
                                val photo = getInstagramPhotos(images, context)
                                photos.add(photo)
                            }
                        }
                    }
                } catch (ex: Exception) { /* ignore */
                }
            }
            return photos
        }

        @Throws(JSONException::class)
        private fun saveUserNameToPreferences(jsonArray: JSONArray, context: Context) {
            val username = jsonArray.getJSONObject(0).getString("username")
            if (username != null) {
                val preferences = context.getSharedPreferences(
                    InstagramPhotoPicker.Companion.PREFERENCE_FILE,
                    Context.MODE_PRIVATE
                )
                val userName = preferences.getString(
                    InstagramPhotoPicker.Companion.PREFERENCE_INSTA_USER_NAME,
                    null
                )
                if (userName == null) {
                    val editor = preferences.edit()
                    editor.putString(
                        InstagramPhotoPicker.Companion.PREFERENCE_INSTA_USER_NAME,
                        username
                    )
                    editor.commit()
                }
            }
        }

        @Throws(JSONException::class, MalformedURLException::class)
        private fun getInstagramPhotos(images: JSONObject, context: Context): InstagramPhoto {
            val thumbnail = images.getJSONObject("thumbnail")
            val lowResolution = images.getJSONObject("low_resolution")
            val standard = images.getJSONObject("standard_resolution")

            //String thumbnailURL = adjustedURL(thumbnail.getString("url"));
            val lowResolutionURL =
                adjustedURL(lowResolution.getString("url"), INSTAGRAM_THUMBNAIL, context)
            val standardURL = adjustedURL(standard.getString("url"), "", context)


            // We use the low resolution image for the picking; the thumbnail image is too
            // low resolution for larger devices.
            return InstagramPhoto(lowResolutionURL, standardURL)
        }

        private fun adjustedURL(
            originalURL: String,
            subDirectory: String,
            conttext: Context
        ): String? {
            var adjustedUrl: String? = null
            try {
                val url = URL(originalURL)
                val conn = url.openConnection()
                val bitmap = BitmapFactory.decodeStream(conn.getInputStream())
                val fileName = originalURL.substring(originalURL.lastIndexOf("/"))
                adjustedUrl = saveImageIntoSD(
                    bitmap,
                    INSTAGRAM_DIRECTORY_NAME + subDirectory,
                    fileName,
                    conttext
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }

            /*if ( originalURL.startsWith( "http://" ) ) return ( originalURL.replace( "http://", "https://" ) );

    return ( originalURL );*/return adjustedUrl
        }

        @Throws(JSONException::class)
        private fun parseNextPageRequestFromResponseJSON(json: JSONObject): InstagramMediaRequest? {
            val pagination = json.getJSONObject("pagination")
            val nextPageURL = pagination.optString("next_url", null)
            return if (nextPageURL != null) InstagramMediaRequest(nextPageURL) else null
        }

        @JvmStatic
        fun saveImageIntoSD(
            bitmap: Bitmap,
            directoryName: String,
            fileName: String,
            context: Context
        ): String {
            var fileName = fileName
            var out: FileOutputStream? = null
            var newFolder: File? = null
            var file: File? = null
            try {
                //Get External Directory
                val externalFilesDirectory = context.externalCacheDir
                newFolder = File(externalFilesDirectory, directoryName)
                if (!newFolder.exists()) {
                    newFolder.mkdirs()
                }
                if (directoryName.contains("Instagram")) {
                    fileName = try {
                        fileName.substring(0, fileName.indexOf("?"))
                    } catch (e: Exception) {
                        fileName.substring(0, 16)
                    }
                }
                file = File(newFolder, fileName)
                file.parentFile.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                    out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    return file.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return file.toString()
        }

        fun generateUniqueFileName(): String {
            val tsLong = System.currentTimeMillis() / 1000
            val timeStamp = tsLong.toString()
            val r = Random()
            val randomNo = r.nextInt(100).toString()
            return timeStamp + randomNo
        }

        fun dismissDialoag(dialogGetMedia: Dialog?) {
            // Log.d(TAG, "dismissDialoag: "+dialogGetMedia);
            if (dialogGetMedia != null && dialogGetMedia.isShowing) {
                dismissDialog(dialogGetMedia)
            }
        }
    }

     object CREATOR : Parcelable.Creator<InstagramMediaRequest> {
        override fun createFromParcel(parcel: Parcel): InstagramMediaRequest {
            return InstagramMediaRequest(parcel)
        }

        override fun newArray(size: Int): Array<InstagramMediaRequest?> {
            return arrayOfNulls(size)
        }
    }
}