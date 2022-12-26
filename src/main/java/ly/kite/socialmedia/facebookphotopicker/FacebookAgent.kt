/*****************************************************
 *
 * FacebookAgent.java
 *
 *
 * Modified MIT License
 *
 * Copyright (c) 2010-2015 Kite Tech Ltd. https://www.kite.ly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The software MAY ONLY be used with the Kite Tech Ltd platform and MAY NOT be modified
 * to be used with any competitor platforms. This means the software MAY NOT be modified
 * to place orders with any competitors to Kite Tech Ltd, all orders MUST go through the
 * Kite Tech Ltd platform servers.
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
///// Package Declaration /////
package ly.kite.socialmedia.facebookphotopicker

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.facebook.*
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.GraphResponse.PagingDirection
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.gson.Gson
import ly.kite.socialmedia.common.*
import ly.kite.socialmedia.common.DeviceManager.deleteFileAndFolderFromInternalStorage
import ly.kite.socialmedia.common.UIUtil.dismissDialog
import ly.kite.socialmedia.common.UIUtil.showProgressDialogWithText
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveFbTokenToPreferences
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveFbUserIdToPreferences
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveFbUserNameToPreferences
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.util.*

///// Import(s) /////
///// Class Declaration /////
/*****************************************************
 *
 * This class is an agent for the Facebook APIs.
 *
 */

class FacebookAgent private constructor(mActivity: Activity) {
    ////////// Static Constant(s) //////////
    private var dialog: Dialog? = null
    var loginManager: LoginManager? = null
    private var exception: Exception? = null
    var mCallbackManager: CallbackManager?
    private var mPendingRequest: ARequest<*>? = null
    private var photosRequest: PhotosRequest? = null
    private var albumsRequest: AlbumsRequest? = null
    fun initAgent() {
        sFacebookAgent = null
    }




    ////////// Constructor(s) //////////
    init {
        sdkInitialize(mActivity!!.applicationContext)
        mCallbackManager = create()
    }
    ////////// Method(s) //////////
    /*****************************************************
     *
     * Called when an activity returns a result.
     *
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (mCallbackManager != null) mCallbackManager!!.onActivityResult(
            requestCode,
            resultCode,
            data
        )
    }

    /*****************************************************
     *
     * Processes a new access token.
     *
     */
    private fun newAccessToken(accessToken: AccessToken) {
//        Log.d(LOG_TAG, "newAcceessToken( accessToken ):\n" + stringFrom(accessToken));
        if (mPendingRequest != null) {
            val pendingRequest: ARequest<*>? = mPendingRequest
            mPendingRequest = null
            pendingRequest!!.onExecute()
        }
    }

    /*****************************************************
     *
     * Loads an initial set of photos.
     *
     */
    private fun executeRequest(request: ARequest<*>) {
        // If we don't have an access token - make a log-in request.
        val accessToken: AccessToken? = AccessToken.getCurrentAccessToken()
        if (accessToken == null || accessToken.userId == null) {
            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(mCallbackManager, LoginResultCallback())
            mPendingRequest = request
            loginManager.logInWithReadPermissions(
                mActivity, Arrays.asList(
                    PERMISSION_USER_PHOTOS, "public_profile"
                )
            )
            return
        }

//        Log.d(LOG_TAG, "Current access token = " + accessToken.getToken());


        // If the access token has expired - refresh it
        if (accessToken.isExpired) {
            Log.i(LOG_TAG, "Access token has expired - refreshing")
            mPendingRequest = request
            AccessToken.refreshCurrentAccessTokenAsync()
            return
        }


        // We have a valid access token, so execute the request
        request.onExecute()
    }

    /*****************************************************
     *
     * Clears any next page request, so photos are retrieved
     * from the start.
     *
     */
    fun resetPhotos() {
        mNextPhotosPageGraphRequest = null
    }

    /*****************************************************
     *
     * Loads the next available page of photos.
     *
     */
    fun getPhotos(
        photosCallback: IPhotosCallback?,
        photoId: Long,
        isDownload: Boolean,
        context: Context?
    ) {
        if (isDownload) {
            dialog = showProgressDialogWithText(context)
        }
        // PhotosRequest photosRequest = new PhotosRequest(photosCallback, photoId);
        photosRequest = PhotosRequest(photosCallback, photoId)
        executeRequest(photosRequest!!)
    }

    fun getAlbums(photosCallback: IAlbumsCallback?) {
        albumsRequest = AlbumsRequest(photosCallback)
        executeRequest(albumsRequest!!)
    }

    fun getuserEmail() {
        val userEmailRequest = UserEmailRequest(null)
        executeRequest(userEmailRequest)
    }
    ////////// Inner Class(es) //////////
    /*****************************************************
     *
     * A request.
     *
     */
    private abstract inner class ARequest<T : ICallback?> internal constructor(var mCallback: T?) {
        abstract fun onExecute()
        fun onError(exception: Exception?) {
            if (mCallback != null) mCallback!!.facOnError(exception)
        }

        fun onCancel() {
            if (mCallback != null) mCallback!!.facOnCancel()
        }
    }

    /*****************************************************
     *
     * A photos request.
     *
     */
    private inner class PhotosRequest internal constructor(
        photosCallback: IPhotosCallback?,
        var id: Long
    ) : ARequest<IPhotosCallback?>(photosCallback) {
        override fun onExecute() {
            // If we already have a next page request ready - execute it now. Otherwise
            // start a brand new request.
            val photosGraphRequestCallback = PhotosGraphRequestCallback(mCallback, id)
            if (mNextPhotosPageGraphRequest != null) {
                mNextPhotosPageGraphRequest!!.callback=photosGraphRequestCallback
                mNextPhotosPageGraphRequest!!.executeAsync()
                mNextPhotosPageGraphRequest = null
                return
            }
            val parameters = Bundle()

            // parameters.putString(PARAMETER_NAME_TYPE, PARAMETER_VALUE_TYPE);
            parameters.putString(PARAMETER_NAME_FIELDS, PARAMETER_VALUE_FIELDS)
            parameters.putString(PARAMETER_NAME_LIMIT, PARAMETER_VALUE_LIMIT)
            val request = GraphRequest(
                AccessToken.getCurrentAccessToken(),
                id.toString() + GRAPH_PATH_MY_PHOTOS,
                parameters, HttpMethod.GET,
                photosGraphRequestCallback
            )
            request.executeAsync()
        }
    }

    /*****************************************************
     *
     * A user email request.
     *
     */
    private inner class UserEmailRequest internal constructor(photosCallback: IAlbumsCallback?) :
        ARequest<IAlbumsCallback?>(photosCallback) {
        override fun onExecute() {
            // If we already have a next page request ready - execute it now. Otherwise
            // start a brand new request.
            val albumsGraphRequestCallback = UserEmailRequestCallback()
            if (mNextPhotosPageGraphRequest != null) {
                mNextPhotosPageGraphRequest!!.callback = albumsGraphRequestCallback
                mNextPhotosPageGraphRequest!!.executeAsync()
                mNextPhotosPageGraphRequest = null
                return
            }
            val parameters = Bundle()

            // parameters.putString(PARAMETER_NAME_TYPE, PARAMETER_VALUE_TYPE);
            parameters.putString(PARAMETER_NAME_FIELDS, NAME_PARAM_VALUE_FIELDS)
            val request = GraphRequest(
                AccessToken.getCurrentAccessToken(),
                GRAPH_PATH_ROOT,
                parameters, HttpMethod.GET,
                albumsGraphRequestCallback
            )
            request.executeAsync()
        }
    }

    /*****************************************************
     *
     * A photos request.
     *
     */
    private inner class AlbumsRequest internal constructor(photosCallback: IAlbumsCallback?) :
        ARequest<IAlbumsCallback?>(photosCallback) {
        override fun onExecute() {
            // If we already have a next page request ready - execute it now. Otherwise
            // start a brand new request.
            val albumsGraphRequestCallback = AlbumsGraphRequestCallback(mCallback)
            if (mNextPhotosPageGraphRequest != null) {
                mNextPhotosPageGraphRequest!!.callback = albumsGraphRequestCallback
                mNextPhotosPageGraphRequest!!.executeAsync()
                mNextPhotosPageGraphRequest = null
                return
            }
            val parameters = Bundle()

            // parameters.putString(PARAMETER_NAME_TYPE, PARAMETER_VALUE_TYPE);
            parameters.putString(PARAMETER_NAME_FIELDS, ALBUM_PARAM_VALUE_FIELDS)
            val request = GraphRequest(
                AccessToken.getCurrentAccessToken(),
                GRAPH_PATH_MY_ALBUMS,
                parameters, HttpMethod.GET,
                albumsGraphRequestCallback
            )
            request.executeAsync()
        }
    }

    /*****************************************************
     *
     * A callback interface.
     *
     */
    interface ICallback {
        fun facOnError(exception: Exception?)
        fun facOnCancel()
    }

    /*****************************************************
     *
     * A photos callback interface.
     *
     */
    interface IPhotosCallback : ICallback {
        fun facOnPhotosSuccess(photoList: ArrayList<Photo?>?, morePhotos: Boolean, id: Long)
    }

    interface IAlbumsCallback : ICallback {
        fun facOnAlbumsSuccess(albumList: ArrayList<Album>?, morePhotos: Boolean)
    }

    /*****************************************************
     *
     * A login result callback.
     *
     */
    private inner class LoginResultCallback : FacebookCallback<LoginResult> {
        /*****************************************************
         *
         * Called when login succeeds.
         *
         */
        override fun onSuccess(loginResult: LoginResult) {
//            Log.d(LOG_TAG, "onSuccess( loginResult = " + loginResult.toString() + " )");
            newAccessToken(loginResult.accessToken)
            saveFbTokenToPreferences(mActivity!!, loginResult.accessToken.toString())
            getuserEmail()
            deleteFileAndFolderFromInternalStorage(
                mActivity!!,
                Constant.FACEBOOK_DIRECTORY_NAME
            )
        }

        /*****************************************************
         *
         * Called when login is cancelled.
         *
         */
        override fun onCancel() {
//            Log.d(LOG_TAG, "onCancel()");
            if (mPendingRequest != null) mPendingRequest!!.onCancel()
        }

        /*****************************************************
         *
         * Called when login fails with an error.
         *
         */
        override fun onError(facebookException: FacebookException) {
//            Log.d(LOG_TAG, "onError( facebookException = " + facebookException + ")", facebookException);
            if (mPendingRequest != null) mPendingRequest!!.onError(facebookException)
        }
    }

    /*****************************************************
     *
     * A graph request callback for photos.
     *
     */
    private inner class AlbumsGraphRequestCallback internal constructor(var mAlbumCallback: IAlbumsCallback?) :
        GraphRequest.Callback {
        override fun onCompleted(graphResponse: GraphResponse) {
//            Log.d(LOG_TAG, "Graph response: " + graphResponse);


            // Check for error
            val error = graphResponse.error
            if (error != null) {
                Log.e(LOG_TAG, "Received Facebook server error: $error")
                when (error.category) {
                    FacebookRequestError.Category.LOGIN_RECOVERABLE -> {
                        Log.e(LOG_TAG, "Attempting to resolve LOGIN_RECOVERABLE error")
                        mPendingRequest = AlbumsRequest(mAlbumCallback)
                        LoginManager.getInstance().resolveError(mActivity, graphResponse)
                        return
                    }
                    FacebookRequestError.Category.TRANSIENT -> {
                        getAlbums(mAlbumCallback)
                        return
                    }
                    FacebookRequestError.Category.OTHER -> {}
                }
                if (mAlbumCallback != null) mAlbumCallback!!.facOnError(error.exception)
                return
            }


            // Check for data
            val responseJSONObject = graphResponse.getJSONObject()
            if (responseJSONObject != null) {
//                Log.d(LOG_TAG, "Response object: " + responseJSONObject.toString());
                val dataJSONArray = responseJSONObject.optJSONArray(JSON_NAME_DATA)
                if (dataJSONArray != null) {
                    val fbAlumsTask = HandleFbAlumsTask(graphResponse, mAlbumCallback)
                    fbAlumsTask.execute(dataJSONArray)
                } else {
                    Log.e(LOG_TAG, "No data found in JSON response: $responseJSONObject")
                }
            } else {
                Log.e(LOG_TAG, "No JSON found in graph response")
            }
        }
    }

    /*****************************************************
     *
     * A user email callback
     *
     */
    private inner class UserEmailRequestCallback internal constructor() : GraphRequest.Callback {
        override fun onCompleted(graphResponse: GraphResponse) {
//            Log.d(LOG_TAG, "Graph response: " + graphResponse);


            // Check for error
            val error = graphResponse.error
            if (error != null) {
                Log.e(LOG_TAG, "Received Facebook server error: $error")
                when (error.category) {
                    FacebookRequestError.Category.LOGIN_RECOVERABLE -> {
                        Log.e(LOG_TAG, "Attempting to resolve LOGIN_RECOVERABLE error")

                        // mPendingRequest = new AlbumsRequest(mAlbumCallback);
                        LoginManager.getInstance().resolveError(mActivity, graphResponse)
                        return
                    }
                    FacebookRequestError.Category.TRANSIENT ->
                        //getAlbums(mAlbumCallback);
                        return
                    FacebookRequestError.Category.OTHER -> {}
                }
            }


            // Check for data
            val responseJSONObject = graphResponse.getJSONObject()
            if (responseJSONObject != null) {
//                Log.d(LOG_TAG, "Response object: " + responseJSONObject.toString());
                try {
                    val email = responseJSONObject.getString(JSON_NAME_NAME)
                    //                    Log.d("### EMAIL : ", " "+ email);
                    saveFbUserNameToPreferences(mActivity!!, email)
                    saveFbUserIdToPreferences(
                        mActivity!!, responseJSONObject.getString(
                            JSON_NAME_ID
                        )
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                Log.e(LOG_TAG, "No JSON found in graph response")
            }
        }
    }

    /*****************************************************
     *
     * A graph request callback for photos.
     *
     */
    private inner class PhotosGraphRequestCallback internal constructor(
        var mPhotosCallback: IPhotosCallback?,
        private val id: Long
    ) : GraphRequest.Callback {
        override fun onCompleted(graphResponse: GraphResponse) {
//            Log.d(LOG_TAG, "Graph response: " + graphResponse);


            // Check for error
            val error = graphResponse.error
            if (error != null) {
                Log.e(LOG_TAG, "Received Facebook server error: $error")
                when (error.category) {
                    FacebookRequestError.Category.LOGIN_RECOVERABLE -> {
                        Log.e(LOG_TAG, "Attempting to resolve LOGIN_RECOVERABLE error")
                        mPendingRequest = PhotosRequest(mPhotosCallback, id)
                        LoginManager.getInstance().resolveError(mActivity, graphResponse)
                        return
                    }
                    FacebookRequestError.Category.TRANSIENT -> {
                        getPhotos(mPhotosCallback, id, false, null)
                        return
                    }
                    FacebookRequestError.Category.OTHER -> {}
                }
                if (mPhotosCallback != null) mPhotosCallback!!.facOnError(error.exception)
                return
            }


            // Check for data
            val responseJSONObject = graphResponse.getJSONObject()
            if (responseJSONObject != null) {
//                Log.d(LOG_TAG, "Response object: " + responseJSONObject.toString());
                val dataJSONArray = responseJSONObject.optJSONArray(JSON_NAME_DATA)
                if (dataJSONArray != null) {
                    val photoArrayList = ArrayList<Photo>(dataJSONArray.length())
                    val downloadImages = DownloadImages(graphResponse, mPhotosCallback)
                    downloadImages.execute(dataJSONArray)
                } else {
                    Log.e(LOG_TAG, "No data found in JSON response: $responseJSONObject")
                }
            } else {
                Log.e(LOG_TAG, "No JSON found in graph response")
            }
        }

        inner class DownloadImages     // private Dialog dialog;
            (private val response: GraphResponse, private val callback: IPhotosCallback?) :
            AsyncTask<JSONArray?, ArrayList<Photo?>?, ArrayList<Photo?>?>() {
            private val picture: String? = null
            private val directory: String? = null
            override fun onPreExecute() {
                // dialog = UIUtil.showProgressDialog(mActivity);
            }

            override fun onPostExecute(result: ArrayList<Photo?>?) {
                if (dialog != null) {
                    dismissDialog(dialog)
                }
                if (callback != null && exception == null) {
                    callback.facOnPhotosSuccess(result, mNextPhotosPageGraphRequest != null, id)
                } else {
                    callback?.facOnError(exception)
                }
            }

            override fun doInBackground(vararg jsonArrays: JSONArray?): ArrayList<Photo?>? {
                val dataJSONArray = jsonArrays[0]
                val photoArrayList = ArrayList<Photo?>()
                val responseJSONObject = response.getJSONObject()
                for (photoIndex in 0 until dataJSONArray!!.length()) {
                    try {
                        val photoJSONObject = dataJSONArray.getJSONObject(photoIndex)
                        val id = photoJSONObject.getString(JSON_NAME_ID)
                        val picture = photoJSONObject.getString(JSON_NAME_PICTURE)
                        val albumName = photoJSONObject.getJSONObject(JSON_NAME_ALBUM).getString(
                            JSON_NAME_NAME
                        )
                        val imageJSONArray = photoJSONObject.getJSONArray(JSON_NAME_IMAGES)

                        // The images are supplied in an array of different formats, so pick the
                        // largest one.
                        val largestImageSource = getLargestImageSource(imageJSONArray)

//                        Log.d(LOG_TAG, "-- Photo --");
//                        Log.d(LOG_TAG, "Id                   : " + id);
//                        Log.d(LOG_TAG, "Picture              : " + picture);
//                        Log.d(LOG_TAG, "AlbumName              : " + picture);
//                        Log.d(LOG_TAG, "Largest image source : " + largestImageSource);
                        val sdPathThumbnail = adjustedURL(
                            picture,
                            "/" + albumName + Constant.FACEBOOK_THUMBNAIL,
                            callback
                        )
                        val sdPathImage = adjustedURL(largestImageSource, "/$albumName", callback)
                        val photo = Photo(
                            sdPathThumbnail!!, sdPathImage!!
                        )
                        photoArrayList.add(photo)
                    } catch (je: JSONException) {
                        Log.e(
                            LOG_TAG,
                            "Unable to extract photo data from JSON: " + responseJSONObject.toString(),
                            je
                        )
                    } catch (mue: Exception) {
                        Log.e(LOG_TAG, "Invalid URL in JSON: " + responseJSONObject.toString(), mue)
                        exception = mue
                        break
                    }
                }
                mNextPhotosPageGraphRequest =
                    response.getRequestForPagedResults(PagingDirection.NEXT)
                return photoArrayList
            }
        }
    }

    inner class HandleFbAlumsTask     // private Dialog dialog;
        (private val response: GraphResponse, private val callback: IAlbumsCallback?) :
        AsyncTask<JSONArray?, Void?, String?>() {
        private val picture: String? = null
        private val directory: String? = null
        override fun onPreExecute() {
            // dialog = UIUtil.showProgressDialog(mActivity);
        }

        override fun onPostExecute(result: String?) {
            // UIUtil.dismissDialog(dialog);
        }


        override fun onProgressUpdate(vararg values: Void?) {}
        override fun doInBackground(vararg jsonArrays: JSONArray?): String? {
            val dataJSONArray = jsonArrays[0]
            val albumArrayList = ArrayList<Album>()
            val responseJSONObject = response.getJSONObject()
            for (photoIndex in 0 until dataJSONArray!!.length()) {
                try {
                    val photoJSONObject = dataJSONArray.getJSONObject(photoIndex)
                    val id = photoJSONObject.getString(JSON_NAME_ID)
                    val albumName = photoJSONObject.getString(JSON_NAME_NAME)
                    val count = photoJSONObject.getInt(JSON_NAME_PHOTO_COUNT)
                    val url = photoJSONObject.getJSONObject(JSON_NAME_PICTURE).getJSONObject(
                        JSON_NAME_DATA
                    ).getString(JSON_NAME_URL)
                    val sdUrl =
                        adjustedURL(url, "/" + albumName + Constant.FACEBOOK_THUMBNAIL, callback)
                    /*JSONArray imageJSONArray = photoJSONObject.getJSONArray(JSON_NAME_IMAGES);

                            // The images are supplied in an array of different formats, so pick the
                            // largest one.
                            String largestImageSource = getLargestImageSource(imageJSONArray);*/Log.d(
                        LOG_TAG, "-- Photo --"
                    )
                    Log.d(LOG_TAG, "Id                   : $id")
                    Log.d(LOG_TAG, "Name              : $albumName")
                    Log.d(LOG_TAG, "count : $count")
                    val album = Album(id, albumName, count, sdUrl!!)
                    if (count > 0) {
                        albumArrayList.add(album)
                    }
                } catch (je: JSONException) {
                    Log.e(
                        LOG_TAG,
                        "Unable to extract photo data from JSON: " + responseJSONObject.toString(),
                        je
                    )
                } catch (mue: Exception) {
                    Log.e(LOG_TAG, "Invalid URL in JSON: " + responseJSONObject.toString(), mue)
                }
            }
            mNextPhotosPageGraphRequest = response.getRequestForPagedResults(PagingDirection.NEXT)
            saveToShredPreference(albumArrayList, Constant.SOCIAL_MEDIA_ALBUM_NAME)
            callback?.facOnAlbumsSuccess(albumArrayList, mNextPhotosPageGraphRequest != null)
            return null
        }
    }

    private fun saveToShredPreference(albumArrayList: ArrayList<Album>, key: String) {
        val prefs: SharedPreferences =
            mActivity!!.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val jsonAlbums = gson.toJson(albumArrayList)
        editor.putString(key, jsonAlbums)
        editor.commit()
    }

    @Throws(IOException::class)
    fun adjustedURL(originalURL: String?, subDirectory: String?, callback: ICallback?): String? {

//        String adjustedUrl = null;
//
//        URL url = new URL(originalURL);
//        URLConnection conn = url.openConnection();
//        Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());

//        String fileName = originalURL.substring(originalURL.lastIndexOf("/"), originalURL.indexOf("?"));
//        adjustedUrl = saveImageIntoSD(bitmap, Constant.FACEBOOK_DIRECTORY_NAME + subDirectory, fileName, mActivity);

//    if ( originalURL.startsWith( "http://" ) ) return ( originalURL.replace( "http://", "https://" ) );
        return originalURL

//        return adjustedUrl;
    }

    /*****************************************************
     *
     * Iterates through the images in a JSON array, and returns
     * the source of the largest one.
     *
     */
    @Throws(JSONException::class)
    fun getLargestImageSource(imageJSONArray: JSONArray?): String? {
        if (imageJSONArray == null) return null
        val imageCount = imageJSONArray.length()
        var largestImageWidth = 0
        var largestImageSource: String? = null
        for (imageIndex in 0 until imageCount) {
            val imageJSONObject = imageJSONArray.getJSONObject(imageIndex)
            val width = imageJSONObject.getInt(JSON_NAME_WIDTH)
            if (width > largestImageWidth) {
                largestImageWidth = width
                largestImageSource = imageJSONObject.getString(JSON_NAME_SOURCE)
            }
        }
        return largestImageSource
    }

    fun cancelFacebookAgentRequest() {
        if (photosRequest != null) photosRequest!!.onCancel()
        if (albumsRequest != null) {
            albumsRequest!!.onCancel()
        }
    }
    private fun FacebookAgent(activity: Activity) {
        mActivity = activity
        sdkInitialize(activity.applicationContext)
        mCallbackManager = create()
    }
    companion object {
        private var mActivity: Activity? = null
        private const val LOG_TAG = "FacebookAgent"
        private const val PERMISSION_USER_PHOTOS = "user_photos"
        private const val GRAPH_PATH_MY_ALBUMS = "/me/albums"
        private const val GRAPH_PATH_ROOT = "/me"
        private const val GRAPH_PATH_MY_PHOTOS = "/photos"
        private const val PARAMETER_NAME_TYPE = "type"
        private const val PARAMETER_VALUE_TYPE = "uploaded"
        private const val PARAMETER_NAME_LIMIT = "limit"
        private const val PARAMETER_VALUE_LIMIT = "100"
        private const val PARAMETER_NAME_FIELDS = "fields"
        private const val PARAMETER_VALUE_FIELDS = "id,picture,images,album"
        private const val ALBUM_PARAM_VALUE_FIELDS = "id,name,picture,photo_count"
        private const val EMAIl_PARAM_VALUE_FIELDS = "email"
        private const val FULLNAME_PARAM_VALUE_FIELDS = "fullname"
        private const val NAME_PARAM_VALUE_FIELDS = "name"
        private const val JSON_NAME_DATA = "data"
        private const val JSON_NAME_ID = "id"
        private const val JSON_NAME_NAME = "name"
        private const val JSON_NAME_ALBUM = "album"
        private const val JSON_NAME_PHOTO_COUNT = "photo_count"
        private const val JSON_NAME_PICTURE = "picture"
        private const val JSON_NAME_IMAGES = "images"
        private const val JSON_NAME_URL = "url"
        private const val JSON_NAME_EMAIL = "email"
        private const val JSON_NAME_WIDTH = "width"
        private const val JSON_NAME_HEIGHT = "height"
        private const val JSON_NAME_SOURCE = "source"
        private const val HTTP_HEADER_NAME_AUTHORISATION = "Authorization"
        private const val HTTP_AUTHORISATION_FORMAT_STRING = "Bearer %s"

        ////////// Static Variable(s) //////////
        var sFacebookAgent: FacebookAgent? = null
        var mNextPhotosPageGraphRequest: GraphRequest? = null
        ////////// Static Initialiser(s) //////////
        ////////// Static Method(s) //////////
        /*****************************************************
         *
         * Returns an instance of this agent.
         *
         */
        ////////// Static Initialiser(s) //////////
        ////////// Static Method(s) //////////
//        open fun getInstance(activity: Activity?): FacebookAgent? {
//            if (sFacebookAgent == null) {
//                sFacebookAgent = FacebookAgent()
//            }
//            return sFacebookAgent
//        }

        ////////// Static Initialiser(s) //////////
        ////////// Static Method(s) //////////
        fun getInstance(activity: Activity): FacebookAgent? {
            mActivity = activity
            if (sFacebookAgent == null) {
                sFacebookAgent = FacebookAgent(mActivity!!)
            }
            return sFacebookAgent
        }

        /*****************************************************
         *
         * Returns a string representation of an access token.
         *
         */
        private fun stringFrom(accessToken: AccessToken?): String {
            if (accessToken == null) return "<null>"
            val stringBuilder = StringBuilder()
            stringBuilder.append("Token          : ").append(accessToken.token).append('\n')
            stringBuilder.append("Application Id : ").append(accessToken.applicationId).append('\n')
            stringBuilder.append("Expires        : ").append(accessToken.expires).append('\n')
            stringBuilder.append("Last Refresh   : ").append(accessToken.lastRefresh).append('\n')
            stringBuilder.append("Source         : ").append(accessToken.source).append('\n')
            stringBuilder.append("Permissions    : ").append(accessToken.permissions).append('\n')
            stringBuilder.append("User Id        : ").append(accessToken.userId).append('\n')
            stringBuilder.append("Email          : ").append(accessToken.userId).append('\n')
            return stringBuilder.toString()
        }

        ////////// Member Variable(s) //////////

        ////////// Constructor(s) //////////

        fun logout() {
            disconnectFromFacebook()
            sFacebookAgent = null
        }

        fun disconnectFromFacebook() {
            if (AccessToken.getCurrentAccessToken() == null) {
                return  // already logged out
            }
            GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/permissions/",
                null,
                HttpMethod.DELETE,
                object : GraphRequest.Callback {
                    override fun onCompleted(graphResponse: GraphResponse) {
                        LoginManager.getInstance().logOut()
                    }
                }).executeAsync()
        }
    }
}