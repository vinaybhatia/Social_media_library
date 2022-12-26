package ly.kite.socialmedia.googlephotopicker

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

object GoogleConstant {
    var isNextPageData = ""
}