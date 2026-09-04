package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BuySpaceDatabase
import com.example.data.model.ProductEntity
import com.example.data.repository.BuySpaceRepository
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BuySpace", appName)
  }

  @Test
  fun `currency formatter formats INR properly`() {
    val formatted = CurrencyFormatter.format(18500.0)
    assertTrue(formatted.contains("18,500"))
    assertTrue(formatted.startsWith("₹"))
  }

  @Test
  fun `product entity progress and remaining amount calculation`() {
    val product = ProductEntity(
      name = "Sony WH-1000XM5",
      targetPrice = 20000.0,
      savedAmount = 5000.0
    )
    assertEquals(15000.0, product.remainingAmount, 0.01)
    assertEquals(0.25f, product.progress, 0.001f)
    assertEquals(25, product.progressPercent)
    assertEquals(false, product.isFunded)
  }
}
