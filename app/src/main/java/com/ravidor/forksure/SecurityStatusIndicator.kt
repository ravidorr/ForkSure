package com.ravidor.forksure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Centralized constants imports
import com.ravidor.forksure.SecurityConstants
import com.ravidor.forksure.AppColors
import com.ravidor.forksure.Dimensions

// Constants moved to centralized Constants.kt file

@Composable
fun SecurityStatusIndicator(
    viewModel: BakingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var securityStatus by remember { mutableStateOf<SecurityEnvironmentResult?>(null) }
    var rateLimitStatus by remember { mutableStateOf<RateLimitResult?>(null) }
    var requestCount by remember { mutableStateOf(0) }

    // Initial status check
    LaunchedEffect(viewModel) {
        delay(SecurityConstants.INITIAL_DELAY_MS)
        securityStatus = viewModel.getSecurityStatus()
        rateLimitStatus = SecurityManager.checkRateLimit(context, "ai_requests")
        requestCount = viewModel.getRequestCount()
    }

    // Periodic status updates with proper lifecycle management
    LaunchedEffect(viewModel) {
        while (isActive) {
            delay(SecurityConstants.UPDATE_INTERVAL_MS)
            if (isActive) {
                try {
                    securityStatus = viewModel.getSecurityStatus()
                    rateLimitStatus = SecurityManager.checkRateLimit(context, "ai_requests")
                    requestCount = viewModel.getRequestCount()
                } catch (e: Exception) {
                    // Handle potential errors gracefully
                    android.util.Log.w("SecurityStatusIndicator", "Error updating status", e)
                }
            }
        }
    }

    // Only show when there are issues or important information
    val shouldShow = when {
        // Show if security is insecure
        securityStatus is SecurityEnvironmentResult.Insecure -> true
        // Show if rate limited/blocked
        rateLimitStatus is RateLimitResult.Blocked -> true
        // Show if low on requests (less than threshold remaining)
        rateLimitStatus is RateLimitResult.Allowed && 
            (rateLimitStatus as RateLimitResult.Allowed).requestsRemaining < SecurityConstants.LOW_REQUESTS_THRESHOLD -> true
        // Hide when everything is normal
        else -> false
    }

    if (shouldShow) {
        Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD, vertical = Dimensions.PADDING_EXTRA_SMALL / 2)
            .semantics {
                contentDescription = "Security and rate limit status indicator"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
                  shape = RoundedCornerShape(Dimensions.CORNER_RADIUS_SMALL)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.PADDING_MEDIUM, vertical = Dimensions.CORNER_RADIUS_SMALL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Security Status
            SecurityStatusItem(
                status = securityStatus,
                modifier = Modifier.weight(1f)
            )

            // Rate Limit Status
            RateLimitStatusItem(
                rateLimitStatus = rateLimitStatus,
                requestCount = requestCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
    }
}

@Composable
private fun SecurityStatusItem(
    status: SecurityEnvironmentResult?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = when (status) {
                is SecurityEnvironmentResult.Secure -> "Security status: Secure environment"
                is SecurityEnvironmentResult.Insecure -> "Security status: Insecure environment detected"
                null -> "Security status: Checking"
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color, text) = when (status) {
            is SecurityEnvironmentResult.Secure -> Triple(
                Icons.Default.Check,
                AppColors.SUCCESS_COLOR,
                "Secure"
            )
            is SecurityEnvironmentResult.Insecure -> Triple(
                Icons.Default.Warning,
                AppColors.WARNING_COLOR,
                "Warning"
            )
            null -> Triple(
                Icons.Default.Info,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Checking..."
            )
        }

        Box(
            modifier = Modifier
                .size(Dimensions.STATUS_DOT_SIZE)
                .background(color, CircleShape)
        )

        Icon(
            imageVector = icon,
            contentDescription = "",
            modifier = Modifier
                            .size(Dimensions.ICON_SIZE_SMALL)
            .padding(start = Dimensions.PADDING_EXTRA_SMALL - 1.dp),
            tint = color
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = Dimensions.PADDING_EXTRA_SMALL - 1.dp)
        )
    }
}

@Composable
private fun RateLimitStatusItem(
    rateLimitStatus: RateLimitResult?,
    requestCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = when (rateLimitStatus) {
                is RateLimitResult.Allowed -> "Rate limit status: ${rateLimitStatus.requestsRemaining} requests remaining"
                is RateLimitResult.Blocked -> "Rate limit status: Blocked, ${rateLimitStatus.reason}"
                null -> "Rate limit status: Checking"
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val (color, text) = when (rateLimitStatus) {
            is RateLimitResult.Allowed -> {
                val remaining = rateLimitStatus.requestsRemaining
                val color = when {
                    remaining > 5 -> AppColors.SUCCESS_COLOR
                    remaining > 2 -> AppColors.WARNING_COLOR
                    else -> AppColors.ERROR_COLOR
                }
                color to "$remaining left"
            }
            is RateLimitResult.Blocked -> {
                AppColors.ERROR_COLOR to "Blocked"
            }
            null -> {
                MaterialTheme.colorScheme.onSurfaceVariant to "..."
            }
        }

        Text(
            text = "Requests: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )

        if (requestCount > 0) {
            Text(
                text = " ($requestCount used)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SecurityWarningBanner(
    issues: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "Security warning: ${issues.joinToString(", ")}"
            },
        colors = CardDefaults.cardColors(
            containerColor = AppColors.ERROR_COLOR.copy(alpha = AppColors.ALPHA_LOW)
        ),
                  shape = RoundedCornerShape(Dimensions.CORNER_RADIUS_STANDARD)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.PADDING_STANDARD)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "",
                    tint = AppColors.ERROR_COLOR,
                    modifier = Modifier.size(Dimensions.ICON_SIZE_LARGE)
                )
                Text(
                    text = "Security Warning",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.ERROR_COLOR,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = Dimensions.PADDING_SMALL)
                )
            }

            Text(
                text = "The following security issues were detected:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Dimensions.PADDING_SMALL)
            )

            issues.forEach { issue ->
                Text(
                    text = "• $issue",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = Dimensions.PADDING_STANDARD, top = Dimensions.PADDING_EXTRA_SMALL)
                )
            }

            Text(
                text = "For your security, some features may be limited. Please use the app in a secure environment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimensions.PADDING_SMALL)
            )
        }
    }
} 