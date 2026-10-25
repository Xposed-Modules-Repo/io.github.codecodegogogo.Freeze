package com.aistra.hail.xposed

import android.app.Application
import android.content.Intent
import com.aistra.hail.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE, application = Application::class)
class MiPushReferencePortTest {
    private val hook = MiPushNotificationHook()

    @Test
    fun receiveAndClickMethodsMatchTheOriginalJavaSource() {
        val fixture = javaClass.classLoader!!.getResourceAsStream("mipush-reference.json")!!
            .bufferedReader().use { JSONObject(it.readText()) }
        val expected = fixture.getJSONObject("directlyCopiedMethods")
        val source = sourceFile("java/com/aistra/hail/xposed/MiPushNotificationHook.java")
            .readText().replace("\r\n", "\n")
        val methodPattern = Regex(
            """(?ms)^    (?:private|public|protected) (?:static )?[\w<>, ?\[\]]+ (\w+)\([^{};]*\)\s*(?:throws [^{]+)?\{.*?^    \}""",
        )
        val counts = mutableMapOf<String, Int>()
        val actual = methodPattern.findAll(source).associate { match ->
            val name = match.groupValues[1]
            val index = (counts[name] ?: 0) + 1
            counts[name] = index
            val normalized = match.value
                .replace("new MiPushToggleHook()", "new XC_MethodHook()")
                .replace("void beforeEnabled(", "void beforeHookedMethod(")
                .replace("void afterEnabled(", "void afterHookedMethod(")
                .trim()
            "$name#$index" to sha256(normalized)
        }
        assertEquals(115, expected.length())
        expected.keys().forEach { key ->
            assertEquals("Reference method changed: $key", expected.getString(key), actual[key])
        }
        val removed = fixture.getJSONArray("removedAutoFreezeMethods")
        repeat(removed.length()) { assertFalse(counts.containsKey(removed.getString(it))) }
    }

    @Test
    fun onlyTheClassicXposedEntryIsPackaged() {
        assertEquals("com.aistra.hail.xposed.LaunchAppHook", sourceFile("assets/xposed_init").readText().trim())
        assertFalse(sourceFile("resources/META-INF/xposed/java_init.list").exists())
        assertFalse(sourceFile("resources/META-INF/xposed/module.prop").exists())
        assertTrue(IXposedHookLoadPackage::class.java.isAssignableFrom(LaunchAppHook::class.java))
        assertTrue(IXposedHookLoadPackage::class.java.isAssignableFrom(MiPushNotificationHook::class.java))
        val scopes = sourceFile("res/values/xposed.xml").readText()
        assertTrue(scopes.contains("<item>android</item>"))
        assertTrue(scopes.contains("<item>com.xiaomi.xmsf</item>"))
        assertFalse(scopes.contains("com.miui.securitycenter"))
    }

    @Test
    fun selfPackageIsAdaptedButOriginalTimingIsPreserved() {
        assertEquals(BuildConfig.APPLICATION_ID, field("MODULE_PACKAGE"))
        assertEquals(1600L, field("PENDING_INTENT_RETRY_DELAY_MS"))
        assertEquals(900L, field("PENDING_INTENT_SECOND_RETRY_DELAY_MS"))
        assertEquals(2200L, field("PACKAGE_READY_WAIT_MS"))
        assertEquals(2, field("MAX_PENDING_INTENT_RETRY_COUNT"))
    }

    @Test
    fun packageNameComesFromThriftFieldSix() {
        val payload = thrift {
            stringField(5, "com.unrelated.application")
            stringField(6, "com.example.frozen")
            stringField(7, "www.example.com")
        }
        val packages = hashSetOf<String>()
        call("collectPackageNamesFromBytes", payload, packages)
        assertEquals(setOf("com.example.frozen"), packages)
    }

    @Test
    fun lengthByteDoesNotPrefixThriftPackageName() {
        val packageName = "com.example." + "a".repeat(36)
        val packages = hashSetOf<String>()
        call("collectPackageNamesFromBytes", thrift { stringField(6, packageName) }, packages)
        assertEquals(setOf(packageName), packages)
    }

    @Test
    fun originalIntentAndWebJumpParametersAreRetained() {
        val intent = call("buildMipushIntentJump", "com.example.frozen", null,
            ".DetailActivity", "268435456") as Intent
        assertEquals("com.example.frozen.DetailActivity", intent.component!!.className)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags)
        val web = call("buildMipushWebJump", "example.com/message/42") as Intent
        assertEquals(Intent.ACTION_VIEW, web.action)
        assertEquals("http://example.com/message/42", web.dataString)
    }

    @Test
    fun notificationRequestStacksKeepTheirOriginalNesting() {
        val requestClass = MiPushNotificationHook::class.java.declaredClasses.single { it.simpleName == "WakeRequest" }
        val constructor = requestClass.declaredConstructors.single().apply { isAccessible = true }
        val request = constructor.newInstance(true, setOf("com.example.frozen"), 0)
        call("pushPendingIntentWakeRequest", request)
        call("pushPendingIntentWakeRequest", null)
        assertNull(call("popPendingIntentWakeRequest"))
        assertSame(request, call("popPendingIntentWakeRequest"))
        assertNull(call("popPendingIntentWakeRequest"))
    }

    private fun sourceFile(path: String): File {
        val moduleRoot = if (File("src/main").isDirectory) File(".") else File("app")
        return File(moduleRoot, "src/main/$path")
    }

    private fun field(name: String): Any? = MiPushNotificationHook::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.get(null)

    private fun call(name: String, vararg args: Any?): Any? {
        val method = MiPushNotificationHook::class.java.declaredMethods.single {
            it.name == name && it.parameterCount == args.size
        }.apply { isAccessible = true }
        return try { method.invoke(hook, *args) } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    }

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun thrift(write: DataOutputStream.() -> Unit): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output -> output.write(); output.writeByte(0) }
    }.toByteArray()

    private fun DataOutputStream.stringField(id: Int, value: String) {
        writeByte(11)
        writeShort(id)
        val bytes = value.toByteArray()
        writeInt(bytes.size)
        write(bytes)
    }
}
