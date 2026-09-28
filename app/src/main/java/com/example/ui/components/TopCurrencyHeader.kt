package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyItem
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer

@Composable
fun TopCurrencyHeader(
    userName: String,
    userEmail: String,
    isDriveConnected: Boolean,
    activeCurrencySymbol: String,
    activeCurrencyIndex: Int = 1,
    exchangeRate2: Double = 36.6243,
    exchangeRate3: Double = 0.92,
    favoriteCurrencies: List<CurrencyItem>,
    onCurrencyIndexSelected: (Int) -> Unit = {},
    onCurrencySelected: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Gmail Profile Info (Clickable to open Profile Screen)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SleekPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario Gmail",
                        tint = SleekOnPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hola, $userName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isDriveConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Drive",
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Ver Perfil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3 Active Parameter Currencies Selector Bar using sleek Segmented Chips
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Moneda de Visualización",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    Text(
                        text = "Base: C$ Córdobas Principal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    favoriteCurrencies.take(3).forEachIndexed { idx, currency ->
                        val isSelected = idx == activeCurrencyIndex || currency.symbol == activeCurrencySymbol

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onCurrencyIndexSelected(idx)
                                    onCurrencySelected(currency.symbol)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 3.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currency.flag.isNotEmpty()) {
                                        Text(text = currency.flag, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = currency.code,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (idx == 0) "${currency.symbol} (Principal)" else currency.symbol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rate Conversion Badge Indicator
                val activeCurr = favoriteCurrencies.getOrNull(activeCurrencyIndex)
                val baseCurr = favoriteCurrencies.getOrNull(0) ?: CurrencyItem("NIO", "C$", "Nicaragua (Córdoba)", "🇳🇮", 36.6243, 1.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = SleekPrimaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Tasa de cambio",
                            tint = SleekPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (activeCurrencyIndex) {
                                0 -> "Moneda Principal activa: Córdobas (${baseCurr.symbol}) • Tasa Oficial: 1 USD = ${String.format(java.util.Locale.US, "%.4f", exchangeRate2)} C$"
                                1 -> "Equivalencia en Dólares ($): 1 USD = ${String.format(java.util.Locale.US, "%.4f", exchangeRate2)} C$ • Base C$ Inmutable"
                                else -> "Equivalencia en Euros (€): 1 EUR = ${String.format(java.util.Locale.US, "%.2f", exchangeRate3)} C$ • Base C$ Inmutable"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekOnPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

