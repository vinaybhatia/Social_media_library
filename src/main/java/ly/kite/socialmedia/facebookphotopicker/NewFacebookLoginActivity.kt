package ly.kite.socialmedia.facebookphotopicker
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.CallbackManager.Factory.create
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.saveFbTokenToPreferences

import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.getFbAccessToken
import android.app.Activity
import com.facebook.login.LoginManager
import android.content.Intent
import android.os.Bundle
import com.facebook.login.LoginResult
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker
import ly.kite.socialmedia.common.DeviceManager
import org.json.JSONObject
import org.json.JSONArray
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent.HandleFbAlumsTask
import org.json.JSONException
import android.os.AsyncTask
import com.facebook.GraphResponse.PagingDirection
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.Throws
import android.annotation.SuppressLint
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPickerActivity
import android.os.Parcelable
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPicker
import ly.kite.socialmedia.R
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity.facebookLoginListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.DialogInterface
import android.util.Log
import com.facebook.*
import java.util.*

class NewFacebookLoginActivity : Activity() {
    private val TAG = "FacebookTAG"
    private val JSON_NAME_ID = "id"
    private val JSON_NAME_NAME = "name"
    private val JSON_NAME_EMAIL = "email"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sdkInitialize(applicationContext)
        setContentView(R.layout.activity_main_socialmedia)
        userDetails
    }

    interface facebookLoginListener {
        fun facebookLoginSuccess(fb_id: String?, fb_email: String?, fb_name: String?)
        fun facebookLoginError(e: JSONException?)
    }//                                        else {

    //                                            str_email = str_name+"@facebook.com";
//                                        }
// handle error
    // Set permissions
    protected val userDetails: Unit
        protected get() {
            callbackmanager = create()

            // Set permissions
            LoginManager.getInstance().logInWithReadPermissions(
                this@NewFacebookLoginActivity,
                Arrays.asList("email", "user_photos", "public_profile")
            )
            LoginManager.getInstance()
                .registerCallback(callbackmanager, object : FacebookCallback<LoginResult?> {


                    override fun onCancel() {
                        Log.d(TAG, "cancel")
                    }

                    override fun onError(e: FacebookException) {
                        Log.d(TAG, "Error")
                    }

                    override fun onSuccess(loginResult: LoginResult?) {
                        val accessToken: AccessToken? = AccessToken?.getCurrentAccessToken()
                        Log.d(TAG, "Current access token = " + accessToken!!.token)
                        saveFbTokenToPreferences(baseContext, accessToken.token)
                        val data_request = GraphRequest.newMeRequest(
                            loginResult!!.accessToken, object : GraphRequest.GraphJSONObjectCallback {
                                override fun onCompleted(
                                    json: JSONObject?,
                                    response: GraphResponse?
                                ) {
                                    if (response!!.error != null) {
                                        // handle error
                                        println("ERROR")
                                    } else {
                                        println("Success")
                                        try {
                                            val jsonresult = json.toString()
                                            println("JSON Result$jsonresult")
                                            val fb_name = json!!.getString(JSON_NAME_NAME)
                                            val fb_id = json.getString(JSON_NAME_ID)
                                            var fb_email: String? = ""
                                            if (json.has(JSON_NAME_EMAIL)) {
                                                fb_email = json.getString(JSON_NAME_EMAIL)
                                            }
                                            //                                        else {
//                                            str_email = str_name+"@facebook.com";
//                                        }
                                            finish()
                                            facebookLoginListener!!.facebookLoginSuccess(
                                                fb_id,
                                                fb_email,
                                                fb_name
                                            )
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            facebookLoginListener!!.facebookLoginError(e)
                                        }
                                    }
                                }
                            })
                        val permission_param = Bundle()
                        permission_param.putString(
                            "fields",
                            "id,name,first_name, last_name,email,gender"
                        )
                        data_request.parameters = permission_param
                        data_request.executeAsync()
                    }
                })
        }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackmanager!!.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        var mFacebookAgent: FacebookAgent? = null

        // Creating Facebook CallbackManager Value
        var callbackmanager: CallbackManager? = null
        var facebookLoginListener: facebookLoginListener? = null
        fun startFacebookActivity(
            activity: Activity,
            facebookLoginListener1: facebookLoginListener?
        ) {
            val intent = Intent(activity, NewFacebookLoginActivity::class.java)
            activity.startActivity(intent)
            facebookLoginListener = facebookLoginListener1
            //        selectedAccountName = acntName;
        }

        fun logout() {
            if (LoginManager.getInstance() != null) {
                LoginManager.getInstance().logOut()
            }
        }
    }
}