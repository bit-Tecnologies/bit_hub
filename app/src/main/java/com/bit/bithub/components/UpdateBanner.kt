package com.bit.bithub.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bit.bithub.data.UpdateInfo

@Composable
fun UpdateBanner(
    updateInfo: UpdateInfo?,
    downloadProgress: Float?,
    isUpdateDownloaded: Boolean = false,
    onUpdateClick: () -> Unit
) {
    AnimatedVisibility(
        visible = updateInfo != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        if (updateInfo != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onUpdateClick() }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(Modifier.width(10.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Доступно обновление bit Hub",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Версия ${updateInfo.versionName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (downloadProgress == null) {
                            Button(
                                onClick = onUpdateClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (isUpdateDownloaded) "Установить" else "Обновить",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (downloadProgress != null) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "Загрузка обновления: ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
