package com.example.data.model

data class CurrencyItem(
    val code: String,
    val symbol: String,
    val country: String,
    val flag: String = "",
    val defaultRateToUsd: Double = 1.0,
    val defaultRateToNio: Double = 36.6243
)

object WorldCurrencies {
    val ALL: List<CurrencyItem> = listOf(
        CurrencyItem("NIO", "C$", "Nicaragua (Córdoba - Moneda Principal)", "🇳🇮", 36.6243, 1.0),
        CurrencyItem("USD", "$", "Estados Unidos (Dólar)", "🇺🇸", 1.0, 36.6243),
        CurrencyItem("EUR", "€", "Unión Europea (Euro)", "🇪🇺", 0.92, 39.809),
        CurrencyItem("MXN", "$", "México (Peso)", "🇲🇽", 18.20, 2.012),
        CurrencyItem("GTQ", "Q", "Guatemala (Quetzal)", "🇬🇹", 7.75, 4.725),
        CurrencyItem("HNL", "L", "Honduras (Lempira)", "🇭🇳", 24.70, 1.482),
        CurrencyItem("CRC", "₡", "Costa Rica (Colón)", "🇨🇷", 525.0, 0.0697),
        CurrencyItem("ARS", "$", "Argentina (Peso)", "🇦🇷", 920.0, 0.0398),
        CurrencyItem("COP", "$", "Colombia (Peso)", "🇨🇴", 4000.0, 0.00915),
        CurrencyItem("PEN", "S/.", "Perú (Sol)", "🇵🇪", 3.75, 9.766),
        CurrencyItem("CLP", "$", "Chile (Peso)", "🇨🇱", 940.0, 0.0389),
        CurrencyItem("BRL", "R$", "Brasil (Real)", "🇧🇷", 5.50, 6.658),
        CurrencyItem("DOP", "RD$", "República Dominicana (Peso)", "🇩🇴", 59.0, 0.620),
        CurrencyItem("PAB", "B/.", "Panamá (Balboa)", "🇵🇦", 1.0, 36.6243),
        CurrencyItem("PYG", "₲", "Paraguay (Guaraní)", "🇵🇾", 7500.0, 0.00488),
        CurrencyItem("UYU", "\$U", "Uruguay (Peso)", "🇺🇾", 40.0, 0.915),
        CurrencyItem("BOB", "Bs.", "Bolivia (Boliviano)", "🇧🇴", 6.91, 5.299),
        CurrencyItem("VES", "Bs.S", "Venezuela (Bolívar)", "🇻🇪", 36.5, 1.003),
        CurrencyItem("GBP", "£", "Reino Unido (Libra)", "🇬🇧", 0.78, 46.954),
        CurrencyItem("JPY", "¥", "Japón (Yen)", "🇯🇵", 155.0, 0.236),
        CurrencyItem("CAD", "C$", "Canadá (Dólar)", "🇨🇦", 1.36, 26.929),
        CurrencyItem("AUD", "A$", "Australia (Dólar)", "🇦🇺", 1.50, 24.416),
        CurrencyItem("CHF", "Fr.", "Suiza (Franco)", "🇨🇭", 0.90, 40.693),
        CurrencyItem("EGP", "E£", "Egipto (Libra)", "🇪🇬", 48.0, 0.763),
        CurrencyItem("INR", "₹", "India (Rupia)", "🇮🇳", 83.5, 0.438),
        CurrencyItem("CNY", "¥", "China (Yuan)", "🇨🇳", 7.25, 5.051)
    )

    val DEFAULT_3: List<CurrencyItem> = listOf(
        CurrencyItem("NIO", "C$", "Nicaragua (Córdoba - Principal)", "🇳🇮", 36.6243, 1.0),
        CurrencyItem("USD", "$", "Estados Unidos (Dólar)", "🇺🇸", 1.0, 36.6243),
        CurrencyItem("EUR", "€", "Unión Europea (Euro)", "🇪🇺", 0.92, 39.809)
    )
}
