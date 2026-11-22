package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {
    @Test
    fun providerLogosMapToDistinctDrawables() {
        val expected = mapOf(
            ProviderId.Routin to R.drawable.share_provider_routin_logo,
            ProviderId.DeepSeek to R.drawable.share_provider_deepseek_logo,
            ProviderId.Glm to R.drawable.share_provider_glm_logo,
            ProviderId.Volcengine to R.drawable.share_provider_volcengine_logo,
            ProviderId.NewAPI to R.drawable.share_provider_newapi_logo,
            ProviderId.CommandCode to R.drawable.share_provider_commandcode_logo,
            ProviderId.Xiaomi to R.drawable.share_provider_xiaomi_logo,
            ProviderId.OpenCode to R.drawable.share_provider_opencode_logo,
        )

        assertEquals(expected, ProviderId.entries.associateWith(ProviderCatalog::logoRes))
        assertEquals(expected.size, expected.values.toSet().size)
    }

    @Test
    fun accentColorIsRemovedFromCatalog() {
        // 设计规范 P0：颜色只来自 token，供应商主题色已废弃（编译期即可发现引用）。
        val source = sourceText("ProviderCatalog.kt")
        assertFalse("ProviderCatalog 不应再提供 accentColor", source.contains("accentColor"))
    }

}

private fun sourceText(name: String): String {
    val workingDir = File(System.getProperty("user.dir") ?: "/tmp")
    val candidates = listOf(
        workingDir.resolve("src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
        workingDir.resolve("android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
    )
    val file = candidates.firstOrNull(File::isFile) ?: throw AssertionError("找不到源文件：$name")
    return file.readText()
}
