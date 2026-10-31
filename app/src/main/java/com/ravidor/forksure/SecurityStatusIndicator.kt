package com.ravidor.forksure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

// Constants moved to centralized Constants.kt file

@Composable
fun SecurityStatusIndicator(
    viewModel: BakingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Use reactive StateFlow from ViewModel (no more disk I/O on main thread!)
    val securityStatus by viewModel.securityStatus.collectAsState()
    var rateLimitStatus by remember { mutableStateOf<RateLimitResult?>(null) }
    var requestCount by remember { mutableIntStateOf(0) }

    // Initial setup - trigger security check and get other status
    LaunchedEffect(viewModel) {
        delay(SecurityConstants.INITIAL_DELAY_MS.milliseconds)
        viewModel.refreshSecurityStatus() // Triggers background check
        rateLimitStatus = SecurityManager.getRateLimitStatus(context, "ai_requests")
        requestCount = viewModel.getRequestCount()
    }

    // Periodic updates for rate limit and request count only
    LaunchedEffect(viewModel) {
        while (isActive) {
            delay(SecurityConstants.UPDATE_INTERVAL_MS.milliseconds)
            if (isActive) {
                try {
                    // Security status updates automatically via StateFlow
                    rateLimitStatus = SecurityManager.getRateLimitStatus(context, "ai_requests")
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
                ThemeColors.successColor(),
                "Secure"
            )
            is SecurityEnvironmentResult.Insecure -> Triple(
                Icons.Default.Warning,
                ThemeColors.warningColor(),
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
                    remaining > 5 -> ThemeColors.successColor()
                    remaining > 2 -> ThemeColors.warningColor()
                    else -> ThemeColors.errorColor()
                }
                color to "$remaining left"
            }
            is RateLimitResult.Blocked -> {
                ThemeColors.errorColor() to "Blocked"
            }
            null -> {
                MaterialTheme.colorScheme.onSurfaceVariant to "..."
            }
        }

        Text(
            text = stringResource(R.string.security_requests_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (requestCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.security_requests_used, requestCount, requestCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
