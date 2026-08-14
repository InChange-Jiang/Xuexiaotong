package com.xuexiaotong.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xuexiaotong.data.Themes
import com.xuexiaotong.ui.home.HomeScreen
import com.xuexiaotong.ui.login.LoginScreen
import com.xuexiaotong.ui.theme.XuexiaotongTheme

/** 根容器：根据登录态切换登录页/主页（主题跟随状态响应式更新） */
@Composable
fun XuexiaotongApp(viewModel: AppViewModel) {
    val loggedIn by viewModel.loggedIn.collectAsState()
    val dark by viewModel.dark.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val theme = Themes.byId(themeId)

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    // 主题包裹在响应式状态之下：切换主题/深色模式时 MaterialTheme 即时重组
    XuexiaotongTheme(theme = theme, dark = dark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            // 页面内部通过 WindowInsets 自行处理状态栏/导航栏避让，背景渐变全屏延伸
            if (loggedIn) {
                HomeScreen(
                    viewModel = viewModel,
                    theme = theme,
                    dark = dark
                )
            } else {
                LoginScreen(
                    api = viewModel.api,
                    theme = theme,
                    dark = dark,
                    onLoginSuccess = { viewModel.onLoginSuccess() }
                )
            }
        }
    }
}
