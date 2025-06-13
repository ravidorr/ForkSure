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

@Composable
fun SecurityStatusIndicator(
    viewModel: BakingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var securityStatus by remember { mutableStateOf<SecurityEnvironmentResult?>(null) }
    var rateLimitStatus by remember { mutableStateOf<RateLimitResult?>(null) }
    var requestCount by remember { mutableStateOf(0) }

    // Update status periodically
    LaunchedEffect(Unit) {
        while (true) {
            securityStatus = viewModel.getSecurityStatus(context)
            rateLimitStatus = SecurityManager.checkRateLimit(context, "ai_requests")
            requestCount = viewModel.getRequestCount()
            delay(5000) // Update every 5 seconds
        }
    }

    // Only show when there are issues or important information
    val shouldShow = when {
        // Show if security is insecure
        securityStatus is SecurityEnvironmentResult.Insecure -> true
        // Show if rate limited/blocked
        rateLimitStatus is RateLimitResult.Blocked -> true
        // Show if low on requests (less than 3 remaining)
        rateLimitStatus is RateLimitResult.Allowed && (rateLimitStatus as RateLimitResult.Allowed).requestsRemaining < 3 -> true
        // Hide when everything is normal
        else -> false
    }

    if (shouldShow) {
        Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .semantics {
                contentDescription = "Security and rate limit status indicator"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                Color(0xFF4CAF50), // Green
                "Secure"
            )
            is SecurityEnvironmentResult.Insecure -> Triple(
                Icons.Default.Warning,
                Color(0xFFFF9800), // Orange
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
                .size(6.dp)
                .background(color, CircleShape)
        )

        Icon(
            imageVector = icon,
            contentDescription = "",
            modifier = Modifier
                .size(12.dp)
                .padding(start = 3.dp),
            tint = color
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 3.dp)
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
                    remaining > 5 -> Color(0xFF4CAF50) // Green
                    remaining > 2 -> Color(0xFFFF9800) // Orange
                    else -> Color(0xFFF44336) // Red
                }
                color to "$remaining left"
            }
            is RateLimitResult.Blocked -> {
                Color(0xFFF44336) to "Blocked"
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
            .padding(16.dp)
            .semantics {
                contentDescription = "Security warning: ${issues.joinToString(", ")}"
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // Light red background
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Security Warning",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "The following security issues were detected:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            issues.forEach { issue ->
                Text(
                    text = "• $issue",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Text(
                text = "For your security, some features may be limited. Please use the app in a secure environment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 