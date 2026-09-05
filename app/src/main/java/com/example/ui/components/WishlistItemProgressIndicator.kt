package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ProfessionalBlue
import com.example.util.CurrencyFormatter

enum class WishlistProgressStyle {
    CIRCULAR,
    BAR,
    CIRCULAR_COMPACT
}

/**
 * High-craft Composable displaying progress towards purchasing a specific wishlist item.
 * Supports both Circular Progress Indicator and Progress Bar representations.
 */
@Composable
fun WishlistItemProgressIndicator(
    product: ProductEntity,
    modifier: Modifier = Modifier,
    style: WishlistProgressStyle = WishlistProgressStyle.CIRCULAR,
    showLabels: Boolean = true,
    size: Dp = 80.dp,
    strokeWidth: Dp = 7.dp,
    accentColor: Color = if (product.isFunded) EmeraldAccent else ProfessionalBlue
) {
    WishlistItemProgressIndicator(
        savedAmount = product.savedAmount,
        targetPrice = product.targetPrice,
        modifier = modifier,
        itemName = product.name,
        style = style,
        showLabels = showLabels,
        size = size,
        strokeWidth = strokeWidth,
        isPurchased = product.status == "PURCHASED",
        accentColor = accentColor
    )
}

/**
 * Core Composable that displays a circular progress indicator or a progress bar showing
 * how much money has been saved toward a specific item in the wishlist.
 */
@Composable
fun WishlistItemProgressIndicator(
    savedAmount: Double,
    targetPrice: Double,
    modifier: Modifier = Modifier,
    itemName: String? = null,
    style: WishlistProgressStyle = WishlistProgressStyle.CIRCULAR,
    showLabels: Boolean = true,
    size: Dp = 80.dp,
    strokeWidth: Dp = 7.dp,
    isPurchased: Boolean = false,
    accentColor: Color = EmeraldAccent
) {
    when (style) {
        WishlistProgressStyle.CIRCULAR -> {
            WishlistItemCircularProgressIndicator(
                savedAmount = savedAmount,
                targetPrice = targetPrice,
                modifier = modifier,
                itemName = itemName,
                size = size,
                strokeWidth = strokeWidth,
                showLabels = showLabels,
                isPurchased = isPurchased,
                accentColor = accentColor
            )
        }
        WishlistProgressStyle.BAR -> {
            WishlistItemProgressBar(
                savedAmount = savedAmount,
                targetPrice = targetPrice,
                modifier = modifier,
                itemName = itemName,
                showLabels = showLabels,
                isPurchased = isPurchased,
                accentColor = accentColor
            )
        }
        WishlistProgressStyle.CIRCULAR_COMPACT -> {
            WishlistItemCircularProgressIndicator(
                savedAmount = savedAmount,
                targetPrice = targetPrice,
                modifier = modifier,
                itemName = null,
                size = 48.dp,
                strokeWidth = 5.dp,
                showLabels = false,
                isPurchased = isPurchased,
                accentColor = accentColor
            )
        }
    }
}

/**
 * Circular progress indicator showing how much money has been saved toward a wishlist item.
 */
@Composable
fun WishlistItemCircularProgressIndicator(
    savedAmount: Double,
    targetPrice: Double,
    modifier: Modifier = Modifier,
    itemName: String? = null,
    size: Dp = 80.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    accentColor: Color = EmeraldAccent,
    showLabels: Boolean = true,
    isPurchased: Boolean = false
) {
    val progressFraction = if (targetPrice > 0.0) {
        (savedAmount / targetPrice).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentage = (progressFraction * 100).toInt()
    val isFunded = targetPrice > 0.0 && savedAmount >= targetPrice
    val remaining = (targetPrice - savedAmount).coerceAtLeast(0.0)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "wishlist_circular_progress"
    )

    val activeColor = when {
        isPurchased -> MaterialTheme.colorScheme.outline
        isFunded -> EmeraldAccent
        else -> accentColor
    }

    Row(
        modifier = modifier
            .testTag("wishlist_item_circular_progress"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular Gauge
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                // Background track ring
                drawCircle(
                    color = trackColor,
                    style = stroke
                )
                // Active progress arc
                if (animatedProgress > 0f) {
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = stroke
                    )
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isFunded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Funded",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(size * 0.36f)
                    )
                } else {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (size.value * 0.22f).sp
                    )
                }
            }
        }

        // Informative Labels
        if (showLabels) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                if (!itemName.isNullOrBlank()) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Saved ${CurrencyFormatter.format(savedAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "of ${CurrencyFormatter.format(targetPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    isPurchased -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Purchased",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    isFunded -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Ready to buy! 🎉",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Need ${CurrencyFormatter.format(remaining)} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Progress bar showing how much money has been saved toward a wishlist item.
 */
@Composable
fun WishlistItemProgressBar(
    savedAmount: Double,
    targetPrice: Double,
    modifier: Modifier = Modifier,
    itemName: String? = null,
    barHeight: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    accentColor: Color = EmeraldAccent,
    showLabels: Boolean = true,
    isPurchased: Boolean = false
) {
    val progressFraction = if (targetPrice > 0.0) {
        (savedAmount / targetPrice).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentage = (progressFraction * 100).toInt()
    val isFunded = targetPrice > 0.0 && savedAmount >= targetPrice
    val remaining = (targetPrice - savedAmount).coerceAtLeast(0.0)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "wishlist_linear_progress"
    )

    val activeColor = when {
        isPurchased -> MaterialTheme.colorScheme.outline
        isFunded -> EmeraldAccent
        else -> accentColor
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wishlist_item_progress_bar")
    ) {
        if (showLabels) {
            if (!itemName.isNullOrBlank()) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Saved ${CurrencyFormatter.format(savedAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "($percentage%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isFunded) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (isPurchased) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Purchased",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isFunded) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldAccent.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Ready to buy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Need ${CurrencyFormatter.format(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = trackColor)
                if (animatedProgress > 0f) {
                    drawRect(
                        color = activeColor,
                        size = size.copy(width = size.width * animatedProgress)
                    )
                }
            }
        }
    }
}
