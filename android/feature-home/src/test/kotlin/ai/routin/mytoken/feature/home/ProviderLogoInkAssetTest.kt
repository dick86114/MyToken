package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.ProviderLogoInk
import ai.routin.mytoken.domain.model.ProviderId
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 真实 drawable 像素的明暗分类基线：
 * 纯黑商标需要浅色底盘，蓝色/彩色商标继续用深色卡片 surface。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProviderLogoInkAssetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `pure black provider logos are dark`() {
        listOf(ProviderId.Xiaomi, ProviderId.CommandCode, ProviderId.OpenCode).forEach { provider ->
            assertTrue(
                "${provider.rawValue} 的纯黑 logo 应判定为深色",
                ProviderLogoInk.isDarkLogo(context, ProviderCatalog.logoRes(provider)),
            )
        }
    }

    @Test
    fun `colored provider logos keep the dark surface plate`() {
        listOf(
            ProviderId.Routin,
            ProviderId.DeepSeek,
            ProviderId.Glm,
            ProviderId.NewAPI,
            ProviderId.Volcengine,
        ).forEach { provider ->
            assertFalse(
                "${provider.rawValue} 的彩色 logo 不应触发浅色底盘",
                ProviderLogoInk.isDarkLogo(context, ProviderCatalog.logoRes(provider)),
            )
        }
    }
}
