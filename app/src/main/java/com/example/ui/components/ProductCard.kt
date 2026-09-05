package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.ProductEntity
import com.example.data.model.ProductPriority
import com.example.data.model.ProductStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.PriorityHighBg
import com.example.ui.theme.PriorityHighBgDark
import com.example.ui.theme.PriorityHighText
import com.example.ui.theme.PriorityHighTextDark
import com.example.ui.theme.PriorityLowBg
import com.example.ui.theme.PriorityLowBgDark
import com.example.ui.theme.PriorityLowText
import com.example.ui.theme.PriorityLowTextDark
import com.example.ui.theme.PriorityMidBg
import com.example.ui.theme.PriorityMidBgDark
import com.example.ui.theme.PriorityMidText
import com.example.ui.theme.PriorityMidTextDark
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.ProfessionalBlueContainer
import com.example.ui.theme.ProfessionalNavyDeep
import com.example.ui.theme.RoseError
import com.example.util.CurrencyFormatter

@Composable
fun ProductCard(
    product: ProductEntity,
    onCardClick: () -> Unit,
    onAllocateClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFunded = product.isFunded
    val isPurchased = product.status == ProductStatus.PURCHASED.name
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val imageRequest = remember(product.imageUrl) {
        if (!product.imageUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(product.imageUrl)
                .size(192)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        } else {
            null
        }
    }

    var isCircularMode by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isFunded && !isPurchased) EmeraldAccent.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Store & Priority Badges + Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Store badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Priority indicator badge
                    val (badgeBg, badgeColor) = when (ProductPriority.fromString(product.priority)) {
                        ProductPriority.HIGH -> if (isDark) PriorityHighBgDark to PriorityHighTextDark else PriorityHighBg to PriorityHighText
                        ProductPriority.MEDIUM -> if (isDark) PriorityMidBgDark to PriorityMidTextDark else PriorityMidBg to PriorityMidText
                        ProductPriority.LOW -> if (isDark) PriorityLowBgDark to PriorityLowTextDark else PriorityLowBg to PriorityLowText
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = product.priority.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Category badge
                    if (product.category.isNotBlank()) {
                        Text(
                            text = "• ${product.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Options Menu button
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(32.dp).testTag("product_options_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Thumbnail + Title & Target Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = ProfessionalBlue.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Price Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.format(product.targetPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Section - Tap to toggle between Progress Bar and Circular Progress Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .clickable { isCircularMode = !isCircularMode }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                WishlistItemProgressIndicator(
                    product = product,
                    style = if (isCircularMode) WishlistProgressStyle.CIRCULAR else WishlistProgressStyle.BAR,
                    showLabels = true,
                    size = 64.dp,
                    strokeWidth = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Open Link + Add Money Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open shopping link if exists
                if (!product.originalUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val rawUrl = product.originalUrl.trim()
                                    val safeUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
                                        "https://$rawUrl"
                                    } else {
                                        rawUrl
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            tint = ProfessionalBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "View Store",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfessionalBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Allocate / Add Money action
                if (!isPurchased) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ProfessionalBlueContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onAllocateClick)
                            .testTag("allocate_btn_${product.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = null,
                                tint = ProfessionalNavyDeep,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isFunded) "Adjust Funds" else "+ Add Money",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ProfessionalNavyDeep
                            )
                        }
                    }
                }
            }
        }
    }
}
