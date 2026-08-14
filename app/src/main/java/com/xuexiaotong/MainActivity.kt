package com.xuexiaotong

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.xuexiaotong.data.ChaoxingApi
import com.xuexiaotong.ui.AppViewModel
import com.xuexiaotong.ui.XuexiaotongApp

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(ChaoxingApi(applicationContext), applicationContext)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** 相机权限请求（供拍照页调用；回调一次性） */
    var cameraPermissionCallback: ((Boolean) -> Unit)? = null
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraPermissionCallback?.invoke(granted)
            cameraPermissionCallback = null
        }

    fun requestCameraPermission(callback: (Boolean) -> Unit) {
        cameraPermissionCallback = callback
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            val has = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (has != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            XuexiaotongApp(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }
}
