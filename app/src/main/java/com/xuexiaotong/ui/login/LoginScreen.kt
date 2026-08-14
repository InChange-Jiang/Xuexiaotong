package com.xuexiaotong.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.xuexiaotong.R
import com.xuexiaotong.data.ChaoxingApi
import com.xuexiaotong.data.Store
import com.xuexiaotong.data.ThemeColor
import com.xuexiaotong.ui.theme.SoftwarePrivacyContent
import com.xuexiaotong.ui.theme.GlassBackdropBox
import com.xuexiaotong.ui.theme.GlassDialog
import com.xuexiaotong.ui.theme.GlassDialogButton
import com.xuexiaotong.ui.theme.glassCard
import com.xuexiaotong.ui.theme.gaussianShadow
import com.xuexiaotong.ui.theme.glassSheet
import com.xuexiaotong.ui.theme.glassPill
import com.xuexiaotong.ui.theme.noRippleClickable
import com.xuexiaotong.ui.theme.parseHex
import com.xuexiaotong.ui.theme.rememberGlassBackdrop
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    api: ChaoxingApi,
    theme: ThemeColor,
    dark: Boolean,
    onLoginSuccess: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val primary = parseHex(if (dark) theme.darkPrimary else theme.lightPrimary)
    val primaryLight = parseHex(if (dark) theme.darkPrimaryLight else theme.lightPrimaryLight)
    // 最底层：白/黑基础色 + 半透明主题渐变（柔和且保留主题感）
    val gradTop = parseHex(if (dark) theme.bgGradDarkStart else theme.bgGradLightStart).copy(alpha = 0.42f)
    val gradBottom = parseHex(if (dark) theme.bgGradDarkEnd else theme.bgGradLightEnd).copy(alpha = 0.42f)

    // 液态玻璃 backdrop：页面与隐私弹窗共享，与登录卡片同款折射逻辑
    val glassBackdrop = rememberGlassBackdrop()

    GlassBackdropBox(
        gradientStart = gradTop,
        gradientEnd = gradBottom,
        baseColor = if (dark) Color(0xFF17171A) else Color(0xFFFDFDFE),
        backdrop = glassBackdrop
    ) { backdrop ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(104.dp))   // 去掉 logo 后补回高度，文案位置与之前一致

            Text("登录学小通", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(
                "记得写作业啊喂o(≧口≦)o",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            // 登录表单（层内玻璃：glassSheet 模拟液态玻璃，防自折射崩溃）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSheet(tint = if (dark) Color(0xFF33333B) else Color.White, radius = 20.dp, dark = dark)
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("账号") },
                    placeholder = { Text("手机号 / 超星号 / 邮箱") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    singleLine = true,
                    // 标准密码输入法：唤起系统安全密码键盘（数字+符号）
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPwd = !showPwd }) {
                            Text(if (showPwd) "隐藏" else "显示", fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (errText.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(errText, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(Modifier.height(24.dp))
                // 玻璃登录按钮（替换原生 Button）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .noRippleClickable(enabled = !loading) {
                            errText = ""
                            if (phone.isBlank()) { errText = "请输入账号"; return@noRippleClickable }
                            if (pwd.isBlank()) { errText = "请输入密码"; return@noRippleClickable }
                            if (!agreed) { showPrivacy = true; return@noRippleClickable }
                            loading = true
                            scope.launch {
                                try {
                                    api.loginByPassword(phone, pwd)
                                    // 加密保存账号密码，供同步前静默重登刷新登录态
                                    Store.saveCredential(phone, pwd)
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    errText = e.message ?: "登录失败"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                        // 层内不折射 backdrop（防自折射崩溃）；primary 底色随后覆盖，视觉不变
                        .glassPill(null, blurRadius = 8.dp, tintAlpha = 0.47f)
                        .then(
                            if (!loading) Modifier.background(primary, RoundedCornerShape(24.dp)) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("登录中...")
                    } else {
                        Text("登录", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 隐私条款（勾选 + 点击打开）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable { showPrivacy = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .noRippleClickable { agreed = !agreed }
                        .background(
                            if (agreed) primary else Color.Transparent,
                            CircleShape
                        )
                        // 未选中时显示轮廓，便于发现
                        .border(
                            1.5.dp,
                            if (agreed) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            CircleShape
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (agreed) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "登录即代表同意《学习通用户协议》与《本软件隐私声明》",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    // 隐私协议弹窗（登录页直接打开，同款液态玻璃卡片）
    if (showPrivacy) {
        LoginPrivacyDialog(
            backdrop = glassBackdrop,
            agreed = agreed,
            onDismiss = { showPrivacy = false },
            onConfirm = {
                agreed = true
                showPrivacy = false
            }
        )
    }
}

/**
 * 登录页隐私协议弹窗：包含学习通条款链接 + 本软件隐私声明 + 同意按钮
 */
@Composable
private fun LoginPrivacyDialog(
    backdrop: Backdrop,
    agreed: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // 声明页/条款列表 双视图切换
    var showStatement by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) { /* 无浏览器时忽略 */ }
    }

    // 背景遮罩：打开时从完全透明渐暗到 0.34
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val maskAlpha by animateFloatAsState(
        targetValue = if (entered) 0.34f else 0f,
        animationSpec = tween(240),
        label = "privacyMask"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = maskAlpha))
            .noRippleClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // 与登录卡片同款液态玻璃：glassCard 折射 backdrop + 硬边高光 + 高斯软阴影
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .gaussianShadow(blur = 11.dp, offsetY = 2.5.dp)
                .glassCard(
                    backdrop = backdrop,
                    radius = 20.dp,
                    blurRadius = 16.dp,
                    tintAlpha = 0.16f
                )
                .noRippleClickable(onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showStatement) {
                // 本软件隐私声明（与软件详情页内容一致，共用 SoftwarePrivacyContent）
                Text(
                    "学小通 · 隐私声明",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    SoftwarePrivacyContent()
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassDialogButton(backdrop, "返回", isPrimary = true) { showStatement = false }
                }
            } else {
                Text(
                    "服务条款与隐私声明",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("学习通服务条款", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    // 学习通官方隐私政策（外部浏览器打开）
                    Text(
                        "学习通《隐私政策》",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable {
                                openUrl("https://homewh.chaoxing.com/agree/agreement?appId=0&type=1")
                            }
                    )
                    Spacer(Modifier.height(4.dp))
                    // 学习通官方用户协议（外部浏览器打开）
                    Text(
                        "学习通《用户协议》",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable {
                                openUrl("https://homewh.chaoxing.com/agree/agreement?appId=1000&type=2")
                            }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("本软件《隐私声明》", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    // 本软件隐私声明：弹窗内直接查看（与软件详情页一致）
                    Text(
                        "学小通《隐私声明》",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { showStatement = true }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(backdrop, "取消", isPrimary = false) { onDismiss() }
                    GlassDialogButton(backdrop, "同意并登录", isPrimary = true) { onConfirm() }
                }
            }
        }
    }
}
