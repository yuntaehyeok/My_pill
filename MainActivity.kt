package com.example.pill2024

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.example.pill2024.Presentation.BottomNavigationBar
import com.example.pill2024.Presentation.DrugDetailView
import com.example.pill2024.Presentation.DrugListView
import com.example.pill2024.Presentation.ImageDetectView
import com.example.pill2024.Presentation.PharmacyListView
import com.example.pill2024.Presentation.destinations.DrugDetailViewDestination
import com.example.pill2024.Presentation.destinations.DrugListViewDestination
import com.example.pill2024.Presentation.destinations.ImageDetectViewDestination
import com.example.pill2024.Presentation.destinations.PharmacyListViewDestination
import com.example.pill2024.ui.theme.Pill2024Theme
import com.example.pill2024.viewModels.AlarmViewModel
import com.example.pill2024.viewModels.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Calendar

class MainActivity : ComponentActivity() {

    val viewModel : MainViewModel by viewModel()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            doCloseApps()
        }
    }

    private fun doCloseApps() {

        MaterialDialog(this@MainActivity).show {
            icon(R.mipmap.ic_launcher)
            title(R.string.app_name)
            message(R.string.back_message)
            positiveButton (R.string.OK){
                finish()
            }
            negativeButton(R.string.CANCEL) {
                this.dismiss()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.onBackPressedDispatcher.addCallback(callback)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.e("", "KeyHash: ${getKeyHash(this)}")

        checkLocationPermission()

        setContent {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val navigator = navController.toDestinationsNavigator()

            Pill2024Theme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false,
                    drawerContent = {
                        Column(
                            modifier = Modifier.background(color = Color.White)
                                .padding(15.dp)
                                .width(170.dp)
                                .fillMaxHeight()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_v1),
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp)
                                        .offset(y = 13.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 메뉴 항목들
                            HorizontalDivider(thickness = 2.dp, color = Color.DarkGray)
                            MenuItem(icon = Icons.Filled.Alarm, label = "알람") {
                               navController.navigate("alarm")
                            }
                            MenuItem(icon = Icons.Filled.CalendarToday, label = "캘린더") {
                                val intent = Intent(this@MainActivity, CalendarView::class.java)
                                startActivity(intent)
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row ( modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {coroutineScope.launch{drawerState.close()}}) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.KeyboardBackspace,
                                        contentDescription = "취소",
                                        modifier = Modifier.size(25.dp),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold (modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF6B66FF),
                                    titleContentColor = Color.White
                                ),
                                title = { Text("My_Medicine", fontSize = 20.sp) },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isClosed) {
                                                drawerState.open()
                                            } else {
                                                drawerState.close()
                                            }
                                        }
                                    }) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Menu",
                                            tint = Color.White
                                        )
                                    }
                                }
                            )
                    }, bottomBar = {
                        BottomNavigationBar(navController)
                    }) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = ImageDetectViewDestination.route,
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable(ImageDetectViewDestination.route) {
                                ImageDetectView(doPharmacyInfo = { item ->
                                    viewModel.viewDetail.value = item
                                    Log.e("", "ImageDetectView ${viewModel.viewDetail.value.drugName}")
                                    navigator.navigate(DrugDetailViewDestination)
                                })
                            }
                            composable(DrugListViewDestination.route) {
                                DrugListView( doShowDetail = { item ->
                                    viewModel.viewDetail.value = item
                                    Log.e("", "DrugListView ${viewModel.viewDetail.value.drugName}")
                                    navigator.navigateUp()
                                    navigator.navigate(DrugDetailViewDestination)
                                })
                            }
                            composable(DrugDetailViewDestination.route) {
                                DrugDetailView(viewModel.viewDetail.value, doBack = {
                                    navigator.navigateUp()
                                })
                            }
                            composable(PharmacyListViewDestination.route) {
                                PharmacyListView(doKakaoNaviCall = { name, xPos, yPos ->
                                    startActivity(
                                        NaviClient.instance.shareDestinationIntent(
                                            Location(name, xPos, yPos),
                                            NaviOption(coordType = CoordType.WGS84)
                                        )
                                    )
                                }, doGetUrlOpen = { url ->
                                    openUrl(url)
                                })
                            }
                            composable("alarm") {
                                AlarmFragment() }
                        }
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        Log.e("", "openUrl $url")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun checkLocationPermission() {
        Log.e("", "checkLocationPermission ...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getLastKnownLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastKnownLocation()
        } else {
            MaterialDialog(this).show {
                icon(R.drawable.ic_app_v1)
                title(R.string.app_name)
                message(R.string.location_permission_message)
                positiveButton(R.string.OK) {
                    finish()
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission()
            return
        }
    }

    fun getKeyHash(context: Context): String? {
        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in packageInfo.signatures!!) {
                val messageDigest =
                    MessageDigest.getInstance("SHA")
                messageDigest.update (signature.toByteArray())
                return Base64.encodeToString(
                    messageDigest.digest(),
                    Base64.NO_WRAP
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("KeyHash", "NameNotFoundException: ${e.message}")
        } catch (e: NoSuchAlgorithmException) {
            Log.e("KeyHash", "NoSuchAlgorithmException: ${e.message}")
        }
        return null
    }
    @Composable
    fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontSize = 16.sp)
        }
    }
}


