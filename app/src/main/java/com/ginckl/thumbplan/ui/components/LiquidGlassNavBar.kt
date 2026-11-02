package com.ginckl.thumbplan.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.AppDestinations
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.PrimaryAccent
import com.ginckl.thumbplan.ui.theme.SurfaceCard
import com.ginckl.thumbplan.ui.theme.TextSecondary

@Composable
fun LiquidGlassNavBar(
    items: Array<AppDestinations>,
    currentDestination: AppDestinations,
    isDarkTheme: Boolean,
    onNavigate: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    var navBarWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.navBarHorizontalMargin,
                vertical = Dimens.navBarBottomMargin
            ),
        contentAlignment = Alignment.Center
    ) {
        // The Glass Background Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.navBarHeight),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { navBarWidth = it.width }
            ) {
                if (navBarWidth > 0) {
                    val itemWidthPx = navBarWidth / items.size.toFloat()
                    val itemWidthDp = with(density) { itemWidthPx.toDp() }
                    
                    val selectedIndex = items.indexOf(currentDestination).coerceAtLeast(0)
                    
                    // The animated floating bubble indicator
                    val bubbleOffset by animateDpAsState(
                        targetValue = itemWidthDp * selectedIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bubbleOffset"
                    )

                    // Draw the bubble background
                    Box(
                        modifier = Modifier
                            .offset(x = bubbleOffset)
                            .width(itemWidthDp)
                            .fillMaxHeight()
                            .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingSm)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }

                    // Draw the icons evenly spaced
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { destination ->
                            val isSelected = destination == currentDestination
                            NavBarItem(
                                icon = destination.icon,
                                label = destination.label,
                                isDarkTheme = isDarkTheme,
                                isSelected = isSelected,
                                onClick = { onNavigate(destination) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isDarkTheme: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Disable default ripple for liquid effect
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.navBarIconSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = com.ginckl.thumbplan.ui.theme.Typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
