package com.example.data.model

data class CurrencyItem(
    val code: String,
    val symbol: String,
    val country: String,
    val flag: String = "",
    val defaultRateToUsd: Double = 1.0
)

object WorldCurrencies {
    val ALL: List<CurrencyItem> = listOf(
        CurrencyItem("USD", "$", "Estados Unidos / Dólar Base", "🇺🇸", 1.0),
        CurrencyItem("NIO", "C$", "Nicaragua (Córdoba)", "🇳🇮", 36.6243),
        CurrencyItem("EUR", "€", "Unión Europea (Euro)", "🇪🇺", 0.92),
        CurrencyItem("MXN", "$", "México (Peso)", "🇲🇽", 18.20),
        CurrencyItem("GTQ", "Q", "Guatemala (Quetzal)", "🇬🇹", 7.75),
        CurrencyItem("HNL", "L", "Honduras (Lempira)", "🇭🇳", 24.70),
        CurrencyItem("CRC", "₡", "Costa Rica (Colón)", "🇨🇷", 525.0),
        CurrencyItem("ARS", "$", "Argentina (Peso)", "🇦🇷", 920.0),
        CurrencyItem("COP", "$", "Colombia (Peso)", "🇨🇴", 4000.0),
        CurrencyItem("PEN", "S/.", "Perú (Sol)", "🇵🇪", 3.75),
        CurrencyItem("CLP", "$", "Chile (Peso)", "🇨🇱", 940.0),
        CurrencyItem("BRL", "R$", "Brasil (Real)", "🇧🇷", 5.50),
        CurrencyItem("DOP", "RD$", "República Dominicana (Peso)", "🇩🇴", 59.0),
        CurrencyItem("PAB", "B/.", "Panamá (Balboa)", "🇵🇦", 1.0),
        CurrencyItem("PYG", "₲", "Paraguay (Guaraní)", "🇵🇾", 7500.0),
        CurrencyItem("UYU", "\$U", "Uruguay (Peso)", "🇺🇾", 40.0),
        CurrencyItem("BOB", "Bs.", "Bolivia (Boliviano)", "🇧🇴", 6.91),
        CurrencyItem("VES", "Bs.S", "Venezuela (Bolívar)", "🇻🇪", 36.5),
        CurrencyItem("GBP", "£", "Reino Unido (Libra)", "🇬🇧", 0.78),
        CurrencyItem("JPY", "¥", "Japón (Yen)", "🇯🇵", 155.0),
        CurrencyItem("CAD", "C$", "Canadá (Dólar)", "🇨🇦", 1.36),
        CurrencyItem("AUD", "A$", "Australia (Dólar)", "🇦🇺", 1.50),
        CurrencyItem("CHF", "Fr.", "Suiza (Franco)", "🇨🇭", 0.90),
        CurrencyItem("EGP", "E£", "Egipto (Libra)", "🇪🇬", 48.0),
        CurrencyItem("INR", "₹", "India (Rupia)", "🇮🇳", 83.5),
        CurrencyItem("CNY", "¥", "China (Yuan)", "🇨🇳", 7.25)
    )

    val DEFAULT_3: List<CurrencyItem> = listOf(
        CurrencyItem("USD", "$", "Estados Unidos (Dólar)", "🇺🇸", 1.0),
        CurrencyItem("NIO", "C$", "Nicaragua (Córdoba)", "🇳🇮", 36.6243),
        CurrencyItem("EUR", "€", "Unión Europea (Euro)", "🇪🇺", 0.92)
    )
}

