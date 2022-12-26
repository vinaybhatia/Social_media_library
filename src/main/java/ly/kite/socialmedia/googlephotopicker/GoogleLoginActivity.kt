package ly.kite.socialmedia.googlephotopicker

import android.accounts.Account
import ly.kite.socialmedia.common.UIUtil.showProgressDialogWithText
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveGoogleTokenIDToPreferences
import ly.kite.socialmedia.common.UIUtil.dismissDialog
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveGoogleUserNameToPreferences
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveGoogleUserIdToPreferences
import ly.kite.socialmedia.googlephotopicker.model.GoogleAlbumEntry
import ly.kite.socialmedia.googlephotopicker.model.GooglePhotoEntry
import ly.kite.socialmedia.googlephotopicker.model.IPhotoEntry
import com.google.gson.TypeAdapterFactory
import kotlin.jvm.JvmOverloads
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import kotlin.Throws
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import android.app.Activity
import android.accounts.AccountManager
import ly.kite.socialmedia.googlephotopicker.model.GoogleUserFeed
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import ly.kite.socialmedia.googlephotopicker.GoogleLoginActivity.AlbumFetchingListener
import ly.kite.socialmedia.googlephotopicker.GoogleLoginActivity
import android.os.Bundle
import ly.kite.socialmedia.R
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import ly.kite.socialmedia.common.UIUtil
import android.content.Intent
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.GoogleAuthException
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker
import ly.kite.socialmedia.googlephotopicker.GoogleLoginActivity.FetchAlbumsInfo
import ly.kite.socialmedia.googlephotopicker.GoogleLoginActivity.HttpPostAsyncTask
import ly.kite.socialmedia.googlephotopicker.GoogleLoginActivity.FetchWarsInfo
import org.json.JSONArray
import org.json.JSONObject
import ly.kite.socialmedia.googlephotopicker.GooglePhotoResponse
import org.json.JSONException
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import ly.kite.socialmedia.common.Constant
import org.slf4j.LoggerFactory
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

class GoogleLoginActivity : Activity() {
    //public static PicasawebService picasaService;
    private val am: AccountManager? = null
    private val list: Array<Account> ?=null
    private val selectedAccount: Account? = null

    // private UserFeed userFeed;
    private val googleUserFeed = GoogleUserFeed()
    private var userId: String? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var albumUrl: String? = null
    private var userName: String? = null
    private var nextPageToken = ""
    private var dialog: Dialog? = null
    private var dialog1: Dialog? = null
    private var eventAction: String? = null

    interface AlbumFetchingListener {
        fun onAlbumFetchSuccessFromServer(
            acntName: String?,
            userId: String?,
            userName: String?,
            albumEntryList: ArrayList<GooglePhotoEntry>?
        )

        fun onAlbumFetchSuccessFromLocal(
            selectedAccountName: String?,
            albumEntryList: List<GoogleAlbumEntry>?
        )

        fun onLoginSuccess(accountName: String?, tokenId: String?, displayName: String?)
        fun onAlbumFetchError(e: Exception?)
        fun onAlbumHasNextToken(tokenId: String?, albumName: String?)
    }

    fun setAlbumFetchingListener(listener: AlbumFetchingListener?) {
        albumFetchingListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setAlbumFetching(this)
    }

    private fun setAlbumFetching(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary"))
            .requestIdToken("129929426917-444vvsga93hq9aeg8ssbfuprnqic63c7.apps.googleusercontent.com")
            .requestServerAuthCode("129929426917-444vvsga93hq9aeg8ssbfuprnqic63c7.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        if (googleLoginLogoutkey == "googleLogout") {
            signOut()
            finish()
        } else {
            val alreadyloggedAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (alreadyloggedAccount != null) {
                dialog = showProgressDialogWithText(this)
                selectedAccountName = alreadyloggedAccount.givenName
                userName = alreadyloggedAccount.displayName
                eventAction = "alreadyloggedAccount"
                getAccessToken(context, alreadyloggedAccount, eventAction!!)
            } else {
                val signInIntent = googleSignInClient!!.signInIntent
                startActivityForResult(signInIntent, 101)
            }
        }
    }

    private fun getAccessToken(
        context: Context,
        alreadyloggedAccount: GoogleSignInAccount,
        action: String
    ) {
        @SuppressLint("StaticFieldLeak") val task1: AsyncTask<Void?, Void?, String?> =
            object : AsyncTask<Void?, Void?, String?>() {
                @SuppressLint("WrongThread")

                override fun onPostExecute(token: String?) {
                    saveGoogleTokenIDToPreferences(context, token)

                    // albumUrl = "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + token;
                    if (isAlbumOpens) {
                        albumUrl =
                            "https://photoslibrary.googleapis.com/v1/albums?alt=json&access_token=$token"
                        FetchAlbumsInfo().execute()
                    } else {
                        FetchGooglePhotos(token, albumIds, context)
                    }
                }

                override fun doInBackground(vararg p0: Void?): String? {
                    accessToken = null
                    val SCOPES = "https://www.googleapis.com/auth/photoslibrary"
                    try {
                        accessToken = GoogleAuthUtil.getToken(
                            applicationContext,
                            alreadyloggedAccount.account!!,
                            "oauth2:$SCOPES"
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: GoogleAuthException) {
                        e.printStackTrace()
                    }
                    return accessToken
                }
            }
        task1.execute()
    }

    override fun onStart() {
        super.onStart()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            try {
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                dialog = showProgressDialogWithText(this)
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(
                    ApiException::class.java
                )
                selectedAccountName = account.givenName
                userName = account.displayName
                userId = account.id
                onLoggedIn(this, account)
            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                e.printStackTrace()
                dismissDialog(dialog)
                finish()
                //Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            }
        }
    }

    private fun onLoggedIn(context: Context, googleSignInAccount: GoogleSignInAccount) {
//        InstagramPhotoPicker.saveGoogleTokenIDToPreferences(getApplicationContext(), googleSignInAccount.getIdToken());
        saveGoogleUserNameToPreferences(context, googleSignInAccount.displayName)
        saveGoogleUserIdToPreferences(context, googleSignInAccount.id)
        if (googleLoginLogoutkey == "googleSignIN") {
            albumFetchingListener!!.onLoginSuccess(userName, userId, selectedAccountName)
            finish()
        } else {
            eventAction = "googleSignInAccount"
            albumFetchingListener!!.onLoginSuccess(userName, userId, selectedAccountName)
            getAccessToken(context, googleSignInAccount, eventAction!!)
        }
    }

    fun FetchGooglePhotos(googleAccessTokenId: String?, mAlbumUrl: String?, context: Context?) {
        accessToken = googleAccessTokenId
        albumUrl = mAlbumUrl
        dialog1 = showProgressDialogWithText(context)
        eventAction = "alreadyloggedAccount"
        Log.e(
            "MainGalleryFragment",
            "FetchGooglePhotos accessToken: " + accessToken + " albumUrl: " + albumUrl
        )
        //https://photoslibrary.googleapis.com/v1/mediaItems:search?pageSize=100&albumId=AF2sCKlCquk6SaaRMusewNQNfTYESN5AN1Hp3QfAmx7n9jE4kDYHj98mQ6y6xuf6TTPAtYkvJ8aE
        if (mAlbumUrl != "") {
            val postData: MutableMap<String?, String?> = HashMap()
            postData["pageSize"] = "100"
            postData["albumId"] = mAlbumUrl
            postData["pageToken"] = nextPageToken
            val task = HttpPostAsyncTask(postData)
            task.execute("https://photoslibrary.googleapis.com/v1/mediaItems:search")
        } else {
            albumUrl =
                "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + accessToken + "&pageToken=" + nextPageToken
            FetchWarsInfo().execute()
        }
        //
    }

    inner class FetchWarsInfo : AsyncTask<Void?, Void?, Void?>() {
        var response: String? = null
        override fun onPreExecute() {
            super.onPreExecute()
        }


        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            try {
                if (response != null && response != "") {
                    var responseArray: JSONArray? = null
                    try {
                        val data = JSONObject(response)
                        responseArray = data.getJSONArray("mediaItems")
                        if (responseArray.length() > 0) {
                            /*
                            Iterating JSON object from JSON Array one by one
                            */
                            val googleAlbumEntries: MutableList<GoogleAlbumEntry> = ArrayList()
                            val photo = GoogleAlbumEntry()
                            photo.albumId = "1"
                            photo.albumTitle = "Google"
                            //                            ArrayList<GooglePhotoEntry> photos = new ArrayList<>();
                            for (i in 0 until responseArray.length()) {
                                val battleObj = responseArray.getJSONObject(i)
                                if (battleObj.getString("mimeType") == "image/jpeg" || battleObj.getString(
                                        "mimeType"
                                    ) == "image/png" || battleObj.getString("mimeType") == "image/jpg" || battleObj.getString(
                                        "mimeType"
                                    ) == "image/heif"
                                ) {
                                    val googlePhotoEntry = GooglePhotoEntry()
                                    googlePhotoEntry.photoId = battleObj.getString("id")
                                    googlePhotoEntry.photoFileName = battleObj.getString("filename")
                                    googlePhotoEntry.photoUrl = battleObj.getString("baseUrl")
                                    photos.add(googlePhotoEntry)
                                } else {
                                    val googlePhotoEntry = GooglePhotoEntry()
                                    googlePhotoEntry.photoId = battleObj.getString("id")
                                    googlePhotoEntry.photoFileName =
                                        battleObj.getString("productUrl")
                                    googlePhotoEntry.photoUrl = battleObj.getString("baseUrl")
                                    photos.add(googlePhotoEntry)
                                }
                            }
                            photo.googlePhotoEntries = photos
                            googleAlbumEntries.add(photo)
                            GooglePhotoResponse.setResponse(response)
                            userName = "Google"
                            albumFetchingListener!!.onAlbumFetchSuccessFromServer(
                                userName,
                                userId,
                                selectedAccountName,
                                photos
                            )
                            if (data.has("nextPageToken")) {
                                nextPageToken = data.getString("nextPageToken")
                                albumUrl =
                                    "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + accessToken + "&pageToken=" + nextPageToken
                                //                                Log.d("url",albumUrl);
                                albumFetchingListener!!.onAlbumHasNextToken(
                                    accessToken,
                                    nextPageToken
                                )
                            }
                            if (dialog1 != null) {
                                dismissDialog(dialog1)
                            }
                            finish()

                            //calling RecyclerViewAdapter constructor by passing context and list
                        }
                    } catch (e: JSONException) {
                        if (dialog1 != null) {
                            dismissDialog(dialog1)
                        }
                        albumFetchingListener!!.onAlbumFetchError(e)
                        finish()
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (dialog1 != null) {
                    dismissDialog(dialog1)
                }
                albumFetchingListener!!.onAlbumFetchError(e)
                finish()
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg p0: Void?): Void? {
            response = creatingURLConnection(albumUrl)
            return null
        }
    }

    inner class FetchAlbumsInfo : AsyncTask<Void?, Void?, Void?>() {
        var response: String? = null
        override fun onPreExecute() {
            super.onPreExecute()
        }



        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            try {
                if (response != null && response != "") {
                    var responseArray: JSONArray? = null
                    try {
                        val data = JSONObject(response)
                        if (data.has("albums")) {
                            responseArray = data.getJSONArray("albums")
                            if (responseArray.length() > 0) {
                                /*
                            Iterating JSON object from JSON Array one by one
                            */
                                val googleAlbumEntries: MutableList<GoogleAlbumEntry> = ArrayList()
                                val photo = GoogleAlbumEntry()
                                photo.albumId = "1"
                                photo.albumTitle = "All"
                                googleAlbumEntries.add(photo)
                                //                            ArrayList<GooglePhotoEntry> photos = new ArrayList<>();
                                for (i in 0 until responseArray.length()) {
                                    val battleObj = responseArray.getJSONObject(i)
                                    val googlePhotoEntry = GoogleAlbumEntry()
                                    googlePhotoEntry.albumId = battleObj.getString("id")
                                    googlePhotoEntry.albumTitle = battleObj.getString("title")
                                    if (battleObj.has("mediaItemsCount")) googlePhotoEntry.mediaItemsCount =
                                        battleObj.getString("mediaItemsCount")
                                    if (battleObj.has("coverPhotoBaseUrl")) googlePhotoEntry.coverPhotoBaseUrl =
                                        battleObj.getString("coverPhotoBaseUrl")
                                    //                                    googlePhotoEntry.setMediaItemsCount(battleObj.getString("mediaItemsCount"));
//                                    googlePhotoEntry.setCoverPhotoBaseUrl(battleObj.getString("coverPhotoBaseUrl"));
                                    googleAlbumEntries.add(googlePhotoEntry)
                                }
                                //  photo.setGooglePhotoEntries(photos);
                                // googleAlbumEntries.add(photo);
                                GooglePhotoResponse.setResponse(response)
                                userName = "Google"
                                albumFetchingListener!!.onAlbumFetchSuccessFromLocal(
                                    selectedAccountName, googleAlbumEntries
                                )
                                albumFetchingListener!!.onAlbumHasNextToken(accessToken, albumUrl)
                                /*  if (data.has("nextPageToken")){
                                nextPageToken = data.getString("nextPageToken");
                                albumUrl = "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + accessToken+"&pageToken="+nextPageToken;
//                                Log.d("url",albumUrl);

                                albumFetchingListener.onAlbumHasNextToken(accessToken, albumUrl);

                            }*/if (dialog != null) {
                                    dialog!!.dismiss()
                                }
                                finish()

                                //calling RecyclerViewAdapter constructor by passing context and list
                            }
                        } else {
                            albumUrl =
                                "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + accessToken + "&pageToken=" + nextPageToken
                            FetchWarsInfo().execute()
                        }
                    } catch (e: JSONException) {
                        if (dialog1 != null) {
                            dismissDialog(dialog1)
                        }
                        albumFetchingListener!!.onAlbumFetchError(e)
                        finish()
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (dialog1 != null) {
                    dismissDialog(dialog1)
                }
                albumFetchingListener!!.onAlbumFetchError(e)
                finish()
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg p0: Void?): Void? {
            response = creatingURLConnection(albumUrl)
            return null
        }
    }

    //https://medium.com/@lewisjkl/android-httpurlconnection-with-asynctask-tutorial-7ce5bf0245cd
    //https://photoslibrary.googleapis.com/v1/mediaItems:search?pageSize=100&albumId=AF2sCKlCquk6SaaRMusewNQNfTYESN5AN1Hp3QfAmx7n9jE4kDYHj98mQ6y6xuf6TTPAtYkvJ8aE
    // ya29.a0ARrdaM9NyuQ6yGbFk4FABLb1BQGEzXIvbIUxqdh0plReVxu47DYi2vP25pPjk-SMM8t9qbJyUg9hMow_gNLi6upokQyzR6lSDj0YamnclxhYzPpiA6wmgDkQkJwsNzHl9MWVkb8Lsuk_BuppEJ8d3aHgF_ZXUQ
    fun creatingURLConnection(GET_URL: String?): String {
        var response = ""
        val conn: HttpURLConnection
        val jsonResults = StringBuilder()
        try {
            //setting URL to connect with
            val url = URL(GET_URL)
            //creating connection
            conn = url.openConnection() as HttpURLConnection
            /*
            converting response into String
            */
            val `in` = InputStreamReader(conn.inputStream)
            var read: Int
            val buff = CharArray(1024)
            while (`in`.read(buff).also { read = it } != -1) {
                jsonResults.append(buff, 0, read)
            }
            response = jsonResults.toString()
        } catch (e: Exception) {
            if (dialog1 != null) {
                dismissDialog(dialog1)
            }
            e.printStackTrace()
        }
        return response
    }

    override fun onResume() {
        super.onResume()
    }

    fun signOut() {
        val alreadyloggedAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (alreadyloggedAccount != null) {
            googleSignInClient!!.signOut()
                .addOnCompleteListener(this) {
                    // ...
                }
        }
    }

    inner class HttpPostAsyncTask(postData: Map<String?, String?>?) :
        AsyncTask<String?, Void?, Void?>() {
        // This is the JSON body of the post
        var postData: JSONObject? = null
        var response: String? = null

        // This is a constructor that allows you to pass in the JSON body
        init {
            if (postData != null) {
                this.postData = JSONObject(postData)
            }
        }

        // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            try {
                if (response != null && response != "") {
                    var responseArray: JSONArray? = null
                    try {
                        val data = JSONObject(response)
                        responseArray = data.getJSONArray("mediaItems")
                        if (responseArray.length() > 0) {
                            /*
                            Iterating JSON object from JSON Array one by one
                            */
                            val googleAlbumEntries: MutableList<GoogleAlbumEntry> = ArrayList()
                            val photo = GoogleAlbumEntry()
                            photo.albumId = "1"
                            photo.albumTitle = "Google"
                            //                            ArrayList<GooglePhotoEntry> photos = new ArrayList<>();
                            for (i in 0 until responseArray.length()) {
                                val battleObj = responseArray.getJSONObject(i)
                                if (battleObj.getString("mimeType") == "image/jpeg" || battleObj.getString(
                                        "mimeType"
                                    ) == "image/png" || battleObj.getString("mimeType") == "image/jpg" || battleObj.getString(
                                        "mimeType"
                                    ) == "image/heif"
                                ) {
                                    val googlePhotoEntry = GooglePhotoEntry()
                                    googlePhotoEntry.photoId = battleObj.getString("id")
                                    googlePhotoEntry.photoFileName = battleObj.getString("filename")
                                    googlePhotoEntry.photoUrl = battleObj.getString("baseUrl")
                                    photos.add(googlePhotoEntry)
                                } else {
                                    val googlePhotoEntry = GooglePhotoEntry()
                                    googlePhotoEntry.photoId = battleObj.getString("id")
                                    googlePhotoEntry.photoFileName =
                                        battleObj.getString("productUrl")
                                    googlePhotoEntry.photoUrl = battleObj.getString("baseUrl")
                                    photos.add(googlePhotoEntry)
                                }
                            }
                            photo.googlePhotoEntries = photos
                            googleAlbumEntries.add(photo)
                            GooglePhotoResponse.setResponse(response)
                            userName = "Google"
                            albumFetchingListener!!.onAlbumFetchSuccessFromServer(
                                userName,
                                userId,
                                selectedAccountName,
                                photos
                            )
                            if (data.has("nextPageToken")) {
                                nextPageToken = data.getString("nextPageToken")
                                albumUrl =
                                    "https://photoslibrary.googleapis.com/v1/mediaItems?alt=json&pageSize=100&access_token=" + accessToken + "&pageToken=" + nextPageToken
                                //                                Log.d("url",albumUrl);
                                albumFetchingListener!!.onAlbumHasNextToken(
                                    accessToken,
                                    nextPageToken
                                )
                            }
                            if (dialog1 != null) {
                                dismissDialog(dialog1)
                            }
                            finish()

                            //calling RecyclerViewAdapter constructor by passing context and list
                        }
                    } catch (e: JSONException) {
                        if (dialog1 != null) {
                            dismissDialog(dialog1)
                        }
                        albumFetchingListener!!.onAlbumFetchError(e)
                        finish()
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (dialog1 != null) {
                    dismissDialog(dialog1)
                }
                albumFetchingListener!!.onAlbumFetchError(e)
                finish()
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            try {
                // This is getting the url from the string we passed in
                val url = URL(params[0])

                // Create the urlConnection
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.doInput = true
                urlConnection.doOutput = true
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.requestMethod = "POST"


                // OPTIONAL - Sets an authorization header
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken)

                // Send the post body
                if (postData != null) {
                    val writer = OutputStreamWriter(urlConnection.outputStream)
                    writer.write(postData.toString())
                    writer.flush()
                }
                val statusCode = urlConnection.responseCode
                if (statusCode == 200) {
                    val inputStream: InputStream = BufferedInputStream(urlConnection.inputStream)
                    response = convertInputStreamToString(inputStream)

                    // From here you can convert the string to JSON with whatever JSON parser you like to use
                    // After converting the string to JSON, I call my custom callback. You can follow this process too, or you can implement the onPostExecute(Result) method
                } else {
                    // Status code is not 200
                    // Do something to handle the error
                }
            } catch (e: Exception) {
                if (dialog1 != null) {
                    dismissDialog(dialog1)
                }
                Log.d("googleLogin", e.localizedMessage)
            }
            return null
        }
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GoogleLoginActivity::class.java)
        const val REQUEST_ID_MULTIPLE_PERMISSIONS = 121
        const val API_PREFIX = "https://picasaweb.google.com/data/feed/api/user/"
        private var albumIds: String? = null
        private var isAlbumOpens = false
        private var selectedAccountName: String? = null
        private var googleLoginLogoutkey: String? = null

        // List<AlbumEntry> mAlbumEntries = new ArrayList<>();
        private var albumFetchingListener: AlbumFetchingListener? = null
        private var accessToken: String? = null
        private val photos = ArrayList<GooglePhotoEntry>()
        private const val nextPageData = false
        private val mcontext: Context? = null
        fun startGoogleActivity(
            activity: Activity,
            albumId: String?,
            isAlbumOpen: Boolean,
            acntName: String?,
            googleLoginLogout: String?,
            listener: AlbumFetchingListener?
        ) {
            val intent = Intent(activity, GoogleLoginActivity::class.java)
            activity.startActivityForResult(intent, Constant.REQUEST_GOOGLE_PHOTOS)
            albumIds = albumId
            isAlbumOpens = isAlbumOpen
            albumFetchingListener = listener
            selectedAccountName = acntName
            googleLoginLogoutkey = googleLoginLogout
            photos.clear()
        }

        fun putdata(ct: Context, key: String?, value: String?) {
            val myPrefs = ct.getSharedPreferences("myPrefs", MODE_PRIVATE)
            val prefsEditor = myPrefs.edit()
            prefsEditor.putString(key, value)
            prefsEditor.commit()
        }

        fun getdata(ct: Context, key: String?): String? {
            val myPrefs =
                ct.getSharedPreferences("myPrefs", MODE_PRIVATE)
            return myPrefs.getString(key, "")
        }
    }
}