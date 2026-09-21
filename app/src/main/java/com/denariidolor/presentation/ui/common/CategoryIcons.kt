/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CategoryIconOption(val key: String, val label: String, val vector: ImageVector)

object CategoryIcons {
    const val DEFAULT_KEY = "ic_category_default"

    val options: List<CategoryIconOption> = listOf(
        CategoryIconOption(DEFAULT_KEY, "General", Icons.Filled.Category),
        CategoryIconOption("income", "Income", Icons.Outlined.Payments),
        CategoryIconOption("transfer", "Transfer", Icons.Outlined.SwapHoriz),
        CategoryIconOption("groceries", "Groceries", Icons.Outlined.ShoppingCart),
        CategoryIconOption("dining", "Dining", Icons.Outlined.Fastfood),
        CategoryIconOption("coffee", "Coffee", Icons.Outlined.LocalCafe),
        CategoryIconOption("housing", "Housing", Icons.Outlined.Home),
        CategoryIconOption("utilities", "Utilities", Icons.Outlined.ElectricBolt),
        CategoryIconOption("internet", "Internet", Icons.Outlined.Wifi),
        CategoryIconOption("phone", "Phone", Icons.Outlined.PhoneAndroid),
        CategoryIconOption("car", "Car", Icons.Outlined.DirectionsCar),
        CategoryIconOption("fuel", "Fuel", Icons.Outlined.LocalGasStation),
        CategoryIconOption("transit", "Transit", Icons.Outlined.DirectionsBus),
        CategoryIconOption("travel", "Travel", Icons.Outlined.Flight),
        CategoryIconOption("health", "Health", Icons.Outlined.LocalHospital),
        CategoryIconOption("fitness", "Fitness", Icons.Outlined.FitnessCenter),
        CategoryIconOption("shopping", "Shopping", Icons.Outlined.ShoppingBag),
        CategoryIconOption("clothing", "Clothing", Icons.Outlined.Checkroom),
        CategoryIconOption("entertainment", "Entertainment", Icons.Outlined.Movie),
        CategoryIconOption("education", "Education", Icons.Outlined.School),
        CategoryIconOption("pets", "Pets", Icons.Outlined.Pets),
        CategoryIconOption("gifts", "Gifts", Icons.Outlined.Redeem),
        CategoryIconOption("savings", "Savings", Icons.Outlined.Savings),
        CategoryIconOption("bank", "Bank & fees", Icons.Outlined.AccountBalance)
    )

    private val byKey = options.associateBy { it.key }

    fun isKnown(key: String): Boolean = key in byKey

    fun forKey(key: String?): CategoryIconOption = byKey[key] ?: byKey.getValue(DEFAULT_KEY)
}

@Composable
fun CategoryIconBadge(iconKey: String?, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val option = CategoryIcons.forKey(iconKey)
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(size / 5),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = option.vector, contentDescription = option.label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
