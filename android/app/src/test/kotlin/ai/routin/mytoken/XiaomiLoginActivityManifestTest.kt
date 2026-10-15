package ai.routin.mytoken

import ai.routin.mytoken.feature.credentials.XiaomiLoginActivity
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 小米登录页是独立 Activity，必须在合并清单里注册到真实类名，
 * 否则点击重试时 startActivity 会抛 ActivityNotFoundException 直接闪退。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class XiaomiLoginActivityManifestTest {

    @Test
    fun `xiaomi login activity is declared with its real class name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val component = ComponentName(context, XiaomiLoginActivity::class.java)

        // 未在清单中声明时 getActivityInfo 会抛 NameNotFoundException。
        val info = context.packageManager.getActivityInfo(component, 0)

        assertFalse("登录页只应由应用内部拉起，不应导出", info.exported)
    }
}
