package com.example.sokotz.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerBusiness_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("daudi@sokotz.tz")
        val repo = FirestoreMarketplaceRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerBusinessProfile(
                name = "Kariakoo Electronics Hub",
                categorySlug = "elektroniki",
                city = "Dar es Salaam",
                district = "Ilala",
                phoneNumber = "+255712345678",
                whatsappNumber = "+255712345678",
                description = "Duka la vifaa bora vya kielektroniki Kariakoo"
            )
        }

        assertTrue(result.isSuccess)
        val businessId = result.getOrNull()
        assertNotNull(businessId)

        val business = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getBusiness(businessId!!)
        }
        assertNotNull(business)
        assertEquals("Kariakoo Electronics Hub", business?.name)
        assertEquals(uid, business?.ownerId)
    }

    @Test
    fun createListing_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("daudi@sokotz.tz")
        val repo = FirestoreMarketplaceRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createListing(
                businessId = "biz_kariakoo_1",
                businessName = "Kariakoo Electronics Hub",
                title = "Samsung Galaxy S24 Ultra",
                priceTzs = 2400000L,
                categorySlug = "elektroniki",
                description = "Simu mpya kabisa yenye warranty ya mwaka mmoja",
                type = "PRODUCT",
                city = "Dar es Salaam",
                phoneNumber = "+255712345678",
                whatsappNumber = "+255712345678"
            )
        }

        assertTrue(result.isSuccess)
        val listingId = result.getOrNull()
        assertNotNull(listingId)

        val listing = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getListing(listingId!!)
        }
        assertNotNull(listing)
        assertEquals("Samsung Galaxy S24 Ultra", listing?.titleSw)
        assertEquals(uid, listing?.ownerId)
    }

    @Test
    fun createListing_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repo = FirestoreMarketplaceRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createListing(
                businessId = "biz_test",
                businessName = "Test Biz",
                title = "Bidhaa ya Jaribio",
                priceTzs = 10000L,
                categorySlug = "mitindo",
                description = "Maelezo",
                type = "PRODUCT",
                city = "Dar es Salaam",
                phoneNumber = "+255712345678",
                whatsappNumber = "+255712345678"
            )
        }

        assertTrue(result.isFailure)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
