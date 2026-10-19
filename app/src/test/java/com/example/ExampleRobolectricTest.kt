package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.remote.GeminiContent
import com.example.sokotz.data.remote.GeminiPart
import com.example.sokotz.data.remote.GeminiRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies SokoTZ app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SokoTZ", appName)
    }

    @Test
    fun `firebase firestore database id is configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbId = context.getString(R.string.firestore_database_id)
        assertNotNull(dbId)
        assertTrue(dbId.contains("sokotz"))
    }

    @Test
    fun `google sign in web client id resource is generated`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val webClientId = context.getString(R.string.default_web_client_id)
        assertNotNull(webClientId)
        assertTrue(webClientId.endsWith(".apps.googleusercontent.com"))
    }

    @Test
    fun `gemini request structure formats multi-turn messages correctly`() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Habari"))),
                GeminiContent(role = "model", parts = listOf(GeminiPart(text = "Habari za SokoTZ"))),
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Nahitaji mafundi")))
            ),
            systemInstruction = GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = "SokoTZ Expert"))
            )
        )
        assertEquals(3, request.contents.size)
        assertEquals("SokoTZ Expert", request.systemInstruction?.parts?.firstOrNull()?.text)
    }

    @Test
    fun `listing model supports product and service distinction`() {
        val productListing = Listing(
            id = "prod_1",
            titleSw = "Solar Panel 200W",
            priceTzs = 150000,
            type = "PRODUCT"
        )
        assertTrue(productListing.isProduct)

        val serviceListing = Listing(
            id = "serv_1",
            titleSw = "Ufungaji Umeme",
            priceTzs = 80000,
            type = "SERVICE"
        )
        assertTrue(!serviceListing.isProduct)
    }
}
