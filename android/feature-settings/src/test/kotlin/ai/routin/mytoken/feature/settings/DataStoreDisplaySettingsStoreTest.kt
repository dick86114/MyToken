package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.domain.model.UsageCardDensity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreDisplaySettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = DataStoreDisplaySettingsStore(context)

    @Test
    fun freshInstallDefaultsToCompactUpgradeUsersKeepFullAndDensityRoundTrips() = runTest {
        // 全新安装：没有任何旧显示设置，默认简洁。
        assertEquals(UsageCardDensity.COMPACT, store.settings.first().usageCardDensity)

        // 升级用户：已有其他显示设置但从未写过密度键，维持完整。
        store.setThemeMode(AppThemeMode.DARK)
        assertEquals(UsageCardDensity.FULL, store.settings.first().usageCardDensity)

        // 用户显式选择后写回并持久化。
        store.setUsageCardDensity(UsageCardDensity.COMPACT)
        assertEquals(UsageCardDensity.COMPACT, store.settings.first().usageCardDensity)
    }
}
