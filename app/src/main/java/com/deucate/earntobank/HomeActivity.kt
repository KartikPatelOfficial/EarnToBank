@file:Suppress("DEPRECATION")

package com.deucate.earntobank

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.net.Uri
import android.widget.LinearLayout
import android.widget.Toast
import com.deucate.earntobank.alert.AlertFragment
import com.deucate.earntobank.auth.LoginActivity
import com.deucate.earntobank.group.GroupFragment
import com.deucate.earntobank.history.HistoryFragment
import com.deucate.earntobank.home.HomeFragment
import com.deucate.earntobank.redeem.RedeemFragment
import com.deucate.earntobank.pocket.PocketFragment
import com.deucate.earntobank.task.TaskFragment
import com.deucate.earntobank.telegram.TelegramFragment
import com.google.android.gms.ads.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.app_bar_home.*
import timber.log.Timber
import java.lang.NullPointerException


class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: DocumentReference

    private val currentFragment = MutableLiveData<Fragment>()
    private val currentTitle = MutableLiveData<String>()
    private var currentFragmentID = 8080

    private lateinit var util: Util
    private lateinit var progressDialog: ProgressDialog

    companion object {
        var interstitialAdID = "ca-app-pub-9733597647251062/5987553963"
        var bannerAdID = "ca-app-pub-8086732239748075/8627405545"
        var impressionPoints = 1000L
        var pointsPerRupee = 10L
        var totalPoints = 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))

        MobileAds.initialize(this, getString(R.string.app_id))
        db = FirebaseFirestore.getInstance().collection("Apps")
            .document(getString(R.string.app_name))
        var name = getString(R.string.app_name)
        name = name.replace(" ", "")
        Timber.d("---> $name")

        util = Util(this)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading.")
        progressDialog.setMessage("Please wait we are working for you.")
        progressDialog.show()

        db.collection(getString(R.string.important)).document("Value").get().addOnCompleteListener {
            if (it.isSuccessful) {
                try {
                    bannerAdID = it.result!!.getString("BannerAdID")!!
                } catch (e: KotlinNullPointerException) {
                    e.printStackTrace()
                }
                try {

                    interstitialAdID = it.result!!.getString("InterstitialAdID")!!
                } catch (e: KotlinNullPointerException) {
                    e.printStackTrace()
                }
                try {

                    impressionPoints = it.result!!.getLong("ImpressionPoint")!!
                } catch (e: KotlinNullPointerException) {
                    e.printStackTrace()
                }
                try {

                    pointsPerRupee = it.result!!.getLong("PointsPerRupee")!!
                } catch (e: KotlinNullPointerException) {
                    e.printStackTrace()
                }
            } else {
                util.showAlertDialog("Error", it.exception!!.localizedMessage)
            }
            progressDialog.dismiss()
            loadBannerAd()
        }

        db.collection(getString(R.string.users)).document(auth.uid!!).get().addOnCompleteListener {
            if (it.isSuccessful) {
                try {
                    totalPoints = it.result!!.getLong("TotalPoints")!!
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    val data = HashMap<String, Any>()
                    data["TotalPoints"] = 0L
                    db.collection(getString(R.string.users)).document(auth.uid!!).update(data)
                }
            } else {
                util.showAlertDialog("Error", it.exception!!.localizedMessage)
            }
            progressDialog.dismiss()
            loadBannerAd()
        }

        @Suppress("DEPRECATION")
        val token = FirebaseInstanceId.getInstance().token
        Timber.d("Token -> $token")

        //Navigation Bat
        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        val titleTv = nav_view.getHeaderView(0).findViewById(R.id.navHeaderTitle) as TextView
        val subTitleTv = nav_view.getHeaderView(0).findViewById(R.id.navHeaderSubTitle) as TextView
        val profilePictureView =
            nav_view.getHeaderView(0).findViewById(R.id.profilePictureIV) as CircleImageView
        titleTv.text = auth.currentUser!!.displayName
        subTitleTv.text = auth.currentUser!!.email
        Picasso.get().load(auth.currentUser!!.photoUrl).fit().into(profilePictureView)

        currentTitle.value = "Home"
        currentFragment.value = HomeFragment()

        currentTitle.observe(this, Observer { root ->
            root.let {
                toolbar.title = it!!
            }
        })

        currentFragment.observe(this, Observer { rootIt ->
            rootIt?.let {
                if (it.id != currentFragmentID) {
                    supportFragmentManager.beginTransaction().replace(R.id.container, it, null)
                        .commit()
                    currentFragmentID = it.id
                }
            }
        })

    }

    @SuppressLint("TimberArgCount")
    private fun loadBannerAd() {
        val adContainer = homeBannerAd

        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = bannerAdID

        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                when (p0) {
                    AdRequest.ERROR_CODE_INTERNAL_ERROR -> {
                        Toast.makeText(
                            this@HomeActivity,
                            "Something happened internally; for instance, an invalid response was received from the ad server.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    AdRequest.ERROR_CODE_INVALID_REQUEST -> {
                        Toast.makeText(
                            this@HomeActivity,
                            " The ad request was invalid; for instance, the ad unit ID was incorrect.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                        Toast.makeText(
                            this@HomeActivity,
                            "The ad request was unsuccessful due to network connectivity.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    AdRequest.ERROR_CODE_NO_FILL -> {
                        Toast.makeText(
                            this@HomeActivity,
                            "The ad request was successful, but no ad was returned due to lack of ad inventory.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                super.onAdFailedToLoad(p0)
            }

            override fun onAdLoaded() {
                Timber.d("--->", "Ad loded")
                super.onAdLoaded()
            }
        }

        val adRequest = AdRequest.Builder()
            /**.addTestDevice("5B7898C326E02838085FDF1DF2A6D711")**/
            .build()
        adView.loadAd(adRequest)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        adContainer.addView(adView, params)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_logout -> {
                AlertDialog.Builder(this).setTitle("Warning!!")
                    .setMessage("Are you sure you want to logout from this app?")
                    .setPositiveButton("YES") { _, _ ->
                        auth.signOut()
                        startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                        finish()
                    }.show()
            }

            R.id.nav_share -> {
                val shareBody = "I found amazing app. Download to earn money"
                val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here")
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
                startActivity(Intent.createChooser(sharingIntent, "Share With"))
            }

            R.id.nav_rate_us -> {
                val uri = Uri.parse("market://details?id=$packageName")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                        )
                    )
                }

            }

            R.id.nav_home -> {
                currentFragment.value = HomeFragment()
                currentTitle.value = "Home"
            }
            R.id.nav_task -> {
                currentFragment.value = TaskFragment()
                currentTitle.value = "Task"

            }
            R.id.nav_pocket -> {
                currentFragment.value = PocketFragment()
                currentTitle.value = "Redeem"

            }
            R.id.nav_redeem -> {
                currentFragment.value = RedeemFragment()
                currentTitle.value = "Pocket"

            }
            R.id.nav_history -> {
                currentFragment.value = HistoryFragment()
                currentTitle.value = "History"

            }
            R.id.nav_group -> {
                currentFragment.value = GroupFragment()
                currentTitle.value = "Group"

            }
            R.id.nav_alert -> {
                currentFragment.value = AlertFragment()
                currentTitle.value = "Alert"

            }
            R.id.nav_telegram -> {
                currentFragment.value = TelegramFragment()
                currentTitle.value = "Telegram"

            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
