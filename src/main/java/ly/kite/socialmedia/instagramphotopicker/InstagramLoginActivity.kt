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
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import kotlin.Throws
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import ly.kite.socialmedia.instagramphotopicker.InstagramMediaRequest.InstagramMediaIdRequestListener
import org.json.JSONArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ProgressBar
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import ly.kite.socialmedia.R
import ly.kite.socialmedia.instagramphotopicker.InstagramLoginActivity
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

// FB dev console activity ly.kite.instagramphotopicker.FacebookPhotoPickerActivity 03/04/2017
class InstagramLoginActivity : Activity() {
    var TOKEN_URL = "https://api.instagram.com/oauth/access_token"
    private var mAccessToken = ""
    private var userId: String? = null
    private var mProgressBar: ProgressBar? = null
    private var webview: WebView? = null
    private var clientId: String? = null
    private var clientSecret: String? = null
    private var redirectUri: String? = null
    private var dialog: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instagram_login)
        dialog = showProgressDialogWithText(this)
        clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)
        clientSecret = intent.getStringExtra(EXTRA_CLIENT_SECRET)
        mProgressBar = findViewById<View>(R.id.progress_bar) as ProgressBar
        webview = findViewById<View>(R.id.webview) as WebView
        val webSettings = webview!!.settings
        webSettings.javaScriptEnabled = true
        webview!!.webViewClient = webViewClient
        loadLoginPage()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLoginPage() {
        val instagramAuthURL =
            ("https://api.instagram.com/oauth/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUri
                    + "&scope=user_profile,user_media" + "&response_type=code")
        webview!!.loadUrl(instagramAuthURL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_CLIENT_ID, clientId)
        outState.putString(EXTRA_REDIRECT_URI, redirectUri)
        outState.putString(EXTRA_CLIENT_SECRET, clientSecret)
        webview!!.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        clientId = savedInstanceState.getString(EXTRA_CLIENT_ID)
        redirectUri = savedInstanceState.getString(EXTRA_REDIRECT_URI)
        clientSecret = savedInstanceState.getString(EXTRA_CLIENT_SECRET)
        webview!!.restoreState(savedInstanceState)
    }

    private fun gotAccessToken(instagramAccessToken: String) {
        InstagramPhotoPicker.Companion.saveInstagramPreferences(
            this,
            instagramAccessToken,
            clientId,
            redirectUri
        )

        // DeviceManager.deleteFileAndFolderFromInternalStorage(Constant.INSTAGRAM_DIRECTORY_NAME);
        val i = Intent()
        i.putExtra("AccessToken", instagramAccessToken)
        setResult(RESULT_OK, i)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // bubble result up to calling/starting activity
        setResult(resultCode, data)
        finish()
    }

    private fun getLoginErrorMessage(uri: Uri): String? {
        val errorReason = uri.getQueryParameter("error_reason")
        var errorMessage: String? = "An unknown error occurred. Please try again."
        if (errorReason != null) {
            if (errorReason.equals("user_denied", ignoreCase = true)) {
                errorMessage = GENERIC_LOGIN_ERROR_MESSAGE
            } else {
                errorMessage = uri.getQueryParameter("error_description")
                errorMessage = if (errorMessage == null) {
                    GENERIC_LOGIN_ERROR_MESSAGE
                } else {
                    try {
                        URLDecoder.decode(errorMessage, "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        GENERIC_LOGIN_ERROR_MESSAGE
                    }
                }
            }
        }
        return errorMessage
    }

    private fun showErrorDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private val webViewClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (DEBUGGING_ENABLED) Log.d(LOG_TAG, "shouldOverrideUrlLoading( view, url = $url )")
            if (url != null && url.startsWith(redirectUri!!)) {
                val uri = Uri.parse(url)
                val error = uri.getQueryParameter("error")
                if (error != null) {
                    val errorMessage = getLoginErrorMessage(uri)
                    webview!!.stopLoading()
                    loadLoginPage()
                    showErrorDialog(errorMessage)
                } else {
                    mProgressBar!!.visibility = View.VISIBLE
                    val code = url.replace("$redirectUri?code=", "").replace("#_", "")
                    object : Thread() {
                        override fun run() {
                            try {
                                val url = URL(TOKEN_URL)
                                val urlConnection = url.openConnection() as HttpURLConnection
                                urlConnection.requestMethod = "POST"
                                urlConnection.doInput = true
                                urlConnection.doOutput = true
                                val writer = OutputStreamWriter(urlConnection.outputStream)
                                writer.write(
                                    "client_id=" + clientId +
                                            "&client_secret=" + clientSecret +
                                            "&grant_type=authorization_code" +
                                            "&redirect_uri=" + redirectUri +
                                            "&code=" + code
                                )
                                writer.flush()
                                val response = streamToString(urlConnection.inputStream)
                                if (response != null) {
                                    val jsonObj = JSONTokener(response).nextValue() as JSONObject
                                    if (jsonObj != null) {
                                        mAccessToken = jsonObj.getString("access_token")
                                        userId = jsonObj.getString("user_id")
                                        getLongLivedAccessToken(clientSecret)
                                        instaUsername
                                        gotAccessToken(mAccessToken)
                                    }
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }.start()
                }
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            if (DEBUGGING_ENABLED) Log.d(LOG_TAG, "onPageStarted( view, url = $url, favicon )")
        }

        override fun onPageFinished(view: WebView, url: String) {
            dismissDialog(dialog)
            if (DEBUGGING_ENABLED) Log.d(LOG_TAG, "onPageFinished( view, url = $url )")
        }

        override fun onLoadResource(view: WebView, url: String) {
            if (DEBUGGING_ENABLED) Log.d(LOG_TAG, "onLoadResources( view, url = $url )")
        }
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

    @Throws(IOException::class, JSONException::class)
    private fun getLongLivedAccessToken(clientScrete: String?) {
        val url: URL
        val response: String
        val jsonObj: JSONObject
        url =
            URL("https://graph.instagram.com/access_token?grant_type=ig_exchange_token&client_secret=$clientScrete&access_token=$mAccessToken")
        response = getInstagramUrlResponse(url)
        jsonObj = JSONTokener(response).nextValue() as JSONObject
        mAccessToken = jsonObj.getString("access_token")
    }

    @get:Throws(IOException::class, JSONException::class)
    private val instaUsername: Unit
        private get() {
            val url: URL
            val response: String
            val jsonObj: JSONObject
            url =
                URL("https://graph.instagram.com/$userId?fields=username&access_token=$mAccessToken")
            response = getInstagramUrlResponse(url)
            jsonObj = JSONTokener(response).nextValue() as JSONObject
            saveUserNameToPreferences(this@InstagramLoginActivity, jsonObj.getString("username"))
            saveUserIdToPreferences(this@InstagramLoginActivity, jsonObj.getString("id"))
        }

    @Throws(IOException::class)
    private fun getInstagramUrlResponse(urlImage: URL): String {
        val urlConnectionImage = urlImage.openConnection() as HttpURLConnection
        urlConnectionImage.requestMethod = "GET"
        urlConnectionImage.doInput = true
        urlConnectionImage.connect()
        return streamToString(urlConnectionImage.inputStream)
    }

    companion object {
        private const val LOG_TAG = "InstagramLoginActivity"
        private const val DEBUGGING_ENABLED = true
        private const val GENERIC_LOGIN_ERROR_MESSAGE =
            "You need to authorise the application to allow photo picking. Please try again."
        private const val EXTRA_CLIENT_ID = "ly.kite.instagramimagepicker.EXTRA_CLIENT_ID"
        private const val EXTRA_REDIRECT_URI = "ly.kite.instagramimagepicker.EXTRA_REDIRECT_URI"
        private const val EXTRA_CLIENT_SECRET = "ly.kite.instagramimagepicker.EXTRA_CLIENT_SECRET"
        private const val REQUEST_CODE_GALLERY = 99
        fun startLoginForResult(
            activity: Activity,
            clientId: String?,
            clientSecret: String?,
            redirectUri: String?,
            requestCode: Int
        ) {
            val i = Intent(activity, InstagramLoginActivity::class.java)
            i.putExtra(EXTRA_CLIENT_ID, clientId)
            i.putExtra(EXTRA_REDIRECT_URI, redirectUri)
            i.putExtra(EXTRA_CLIENT_SECRET, clientSecret)
            activity.startActivityForResult(i, requestCode)
        }

        fun startLoginForResult(
            fragment: Fragment,
            clientId: String?,
            clientSecret: String?,
            redirectUri: String?,
            requestCode: Int
        ) {
            val i = Intent(fragment.activity, InstagramLoginActivity::class.java)
            i.putExtra(EXTRA_CLIENT_ID, clientId)
            i.putExtra(EXTRA_REDIRECT_URI, redirectUri)
            i.putExtra(EXTRA_CLIENT_SECRET, clientSecret)
            fragment.startActivityForResult(i, requestCode)
        }

        @Throws(JSONException::class)
        private fun saveUserNameToPreferences(context: Context, username: String?) {
            if (username != null) {
                val preferences = context.getSharedPreferences(
                    InstagramPhotoPicker.Companion.PREFERENCE_FILE,
                    MODE_PRIVATE
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

        @Throws(JSONException::class)
        private fun saveUserIdToPreferences(context: Context, id: String?) {
            if (id != null) {
                val preferences = context.getSharedPreferences(
                    InstagramPhotoPicker.Companion.PREFERENCE_FILE,
                    MODE_PRIVATE
                )
                val userName = preferences.getString(
                    InstagramPhotoPicker.Companion.PREFERENCE_INSTA_USER_ID,
                    null
                )
                if (userName == null) {
                    val editor = preferences.edit()
                    editor.putString(InstagramPhotoPicker.Companion.PREFERENCE_INSTA_USER_ID, id)
                    editor.commit()
                }
            }
        }
    }
}