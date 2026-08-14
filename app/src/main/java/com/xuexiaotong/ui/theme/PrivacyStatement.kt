package com.xuexiaotong.ui.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 学小通软件隐私声明正文（登录页与软件详情页共用，保证两处内容一致）
 */
@Composable
fun SoftwarePrivacyContent() {
    PrivacyP("本软件（\"学小通\"）致力于保护你的个人信息安全。本声明简要说明我们如何处理你的数据与所申请的权限。")
    PrivacyH("一、我们收集的信息")
    PrivacyP("1. 登录凭证：仅在你主动登录时，收集你输入的学习通账号与密码，用于完成学习通官方登录。若你开启\"记住登录\"功能，凭证将以加密形式仅存储于本机，绝不传输至第三方。")
    PrivacyP("2. 课程与作业数据：包括课程名称、作业标题、完成状态与截止时间，全部来自你账号名下的学习通数据。")
    PrivacyP("3. 照片笔记：你使用拍照、相册导入功能产生的课件照片与科目分类信息，仅保存在本机。")
    PrivacyH("二、信息的使用")
    PrivacyP("1. 登录凭证仅用于登录学习通官方服务；2. 课程与作业数据仅用于在日历中向你展示，帮助你管理作业进度；3. 照片笔记仅用于课件归档与本地查看。")
    PrivacyH("三、信息的存储与保护")
    PrivacyP("所有数据仅存储在你的设备本地，不会上传至任何除学习通官方之外的服务器。登录凭证采用加密存储；照片存储于应用私有目录，系统相册或其他应用无法访问。")
    PrivacyH("四、权限使用说明")
    PrivacyP("1. 相机权限：仅在你主动进入拍照功能时请求，用于取景与拍摄课件照片；软件不会在后台调用相机，也不会录制或上传任何影像。")
    PrivacyP("2. 照片导出：仅在你主动选择\"导出到系统相册\"时，通过系统媒体库（MediaStore）写入你指定的照片；除你主动导出外，软件不访问或修改系统相册内容。")
    PrivacyP("3. 通知权限：用于按你的设置准时推送日程与作业提醒。")
    PrivacyP("4. 精确闹钟权限：用于在应用被清理后台后仍能准时发出提醒。")
    PrivacyP("5. 网络权限：仅用于与学习通官方服务器通信完成登录与数据获取。")
    PrivacyH("五、信息的共享")
    PrivacyP("我们不会向任何第三方出售、出租或共享你的个人信息。本软件与学习通相互独立，非学习通官方产品。")
    PrivacyH("六、你的权利")
    PrivacyP("你可以随时在菜单栏中退出登录，清除本机保存的全部数据；卸载软件亦将彻底删除本地数据（包括照片笔记）。")
    PrivacyH("七、政策更新")
    PrivacyP("本声明如有更新，将在软件内明示。继续使用即视为同意更新后的声明。")
    PrivacyP("最后更新：2026年8月")
    Spacer(Modifier.height(10.dp))
}

@Composable
fun PrivacyH(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
fun PrivacyP(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
}
