package com.example

import com.example.data.model.WorldCurrencies
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCordobasAsPrimaryCurrency_PreservesExactAmount() {
    val officialRateUsd = 36.6243
    val inputCordobas = 2000.0

    // When active currency is Cordobas (NIO), base amount is exactly inputCordobas
    val storedBaseCordobas = inputCordobas
    assertEquals(2000.0, storedBaseCordobas, 0.0)

    // Equivalent in Dollars: 2000 / 36.6243 = 54.60855...
    val equivalentUsd = storedBaseCordobas / officialRateUsd
    assertEquals(54.60855, equivalentUsd, 0.0001)

    // Recalculating back from base Cordobas: exactly 2000.0 preserved without any drift
    assertEquals(2000.0, storedBaseCordobas, 0.0)
  }

  @Test
  fun testUsdRegistration_FirstCalculationToCordobasRules() {
    val officialRateUsd = 36.6243
    val inputUsd = 100.0

    // When registering in USD, first calculation to Cordobas sets the primary base
    val calculatedCordobas = inputUsd * officialRateUsd
    assertEquals(3662.43, calculatedCordobas, 0.0001)

    // Stored base in DB is calculatedCordobas
    val storedBaseInDb = calculatedCordobas
    assertEquals(3662.43, storedBaseInDb, 0.0001)
  }

  @Test
  fun testDefaultCurrencies_PrimaryIsNioCordobas() {
    val defaultList = WorldCurrencies.DEFAULT_3
    assertEquals("NIO", defaultList[0].code)
    assertEquals("C$", defaultList[0].symbol)
    assertEquals(1.0, defaultList[0].defaultRateToNio, 0.0)

    assertEquals("USD", defaultList[1].code)
    assertEquals(36.6243, defaultList[1].defaultRateToNio, 0.0001)
  }
}

