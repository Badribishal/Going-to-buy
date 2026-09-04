package com.example

import com.example.data.model.ProductEntity
import com.example.ui.components.NavTab
import com.example.ui.components.WishlistProgressStyle
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun navTabs_supportSwipeIndicesCorrectly() {
    assertEquals(4, NavTab.entries.size)
    assertEquals(NavTab.HOME, NavTab.entries[0])
    assertEquals(NavTab.WISHLIST, NavTab.entries[1])
    assertEquals(NavTab.SAVINGS, NavTab.entries[2])
    assertEquals(NavTab.ACTIVITY, NavTab.entries[3])
  }

  @Test
  fun wishlistProgress_fractionCalculations() {
    val partialProduct = ProductEntity(
      name = "Kindle Paperwhite",
      targetPrice = 14000.0,
      savedAmount = 7000.0
    )
    assertEquals(0.5f, partialProduct.progress, 0.001f)
    assertEquals(50, partialProduct.progressPercent)
    assertFalse(partialProduct.isFunded)

    val fundedProduct = ProductEntity(
      name = "Mechanical Keyboard",
      targetPrice = 8000.0,
      savedAmount = 8000.0
    )
    assertEquals(1.0f, fundedProduct.progress, 0.001f)
    assertEquals(100, fundedProduct.progressPercent)
    assertTrue(fundedProduct.isFunded)
    assertEquals(0.0, fundedProduct.remainingAmount, 0.001)
  }

  @Test
  fun wishlistProgressStyles_exist() {
    val styles = WishlistProgressStyle.entries
    assertTrue(styles.contains(WishlistProgressStyle.CIRCULAR))
    assertTrue(styles.contains(WishlistProgressStyle.BAR))
    assertTrue(styles.contains(WishlistProgressStyle.CIRCULAR_COMPACT))
  }
}
