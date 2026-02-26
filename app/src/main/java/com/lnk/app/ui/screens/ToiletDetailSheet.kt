package com.lnk.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.SOURCE_MASTER
import com.lnk.app.data.model.Toilet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToiletDetailSheet(
    toilet: Toilet,
    currentUserId: String?,
    comments: List<Comment>,
    isCommentLoading: Boolean,
    isCommentSaving: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onAddComment: (String) -> Unit,
    onUpdateComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onHideToilet: () -> Unit
) {
    var commentInput by remember(toilet.id) { mutableStateOf("") }
    var editingCommentId by remember(toilet.id) { mutableStateOf<String?>(null) }
    val isLikedByMe = !currentUserId.isNullOrBlank() && toilet.likedUserIds.contains(currentUserId)
    val isDislikedByMe = !currentUserId.isNullOrBlank() && toilet.dislikedUserIds.contains(currentUserId)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(comments) {
        if (editingCommentId != null && comments.none { it.id == editingCommentId }) {
            editingCommentId = null
            commentInput = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (toilet.source == SOURCE_MASTER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (toilet.source == SOURCE_MASTER) "공공 데이터" else "사용자 등록",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = toiletTypeLabel(toilet.type),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (toilet.description.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Info, text = toilet.description)
                    }
                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        text = "좌표: ${formatCoordinate(toilet.lat)}, ${formatCoordinate(toilet.lng)}"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReactionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ThumbUp,
                    label = "좋아요",
                    count = toilet.likeCount,
                    isSelected = isLikedByMe,
                    onClick = onToggleLike,
                    color = Color(0xFF4CAF50)
                )
                ReactionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ThumbUp,
                    label = "싫어요",
                    count = toilet.dislikeCount,
                    isSelected = isDislikedByMe,
                    onClick = onToggleDislike,
                    color = Color(0xFFF44336),
                    isMirrored = true
                )
            }

            TextButton(
                onClick = onHideToilet,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("내 지도에서 숨기기")
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "댓글 (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("방문 정보를 남겨 주세요.", fontSize = 14.sp) },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val targetId = editingCommentId
                                if (targetId == null) {
                                    onAddComment(commentInput)
                                } else {
                                    onUpdateComment(targetId, commentInput)
                                }
                                commentInput = ""
                                editingCommentId = null
                            },
                            enabled = !isCommentSaving && commentInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    isCommentSaving -> "처리 중..."
                                    editingCommentId == null -> "댓글 등록"
                                    else -> "수정 완료"
                                }
                            )
                        }
                        if (editingCommentId != null) {
                            TextButton(
                                onClick = {
                                    editingCommentId = null
                                    commentInput = ""
                                }
                            ) {
                                Text("취소")
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isCommentLoading) {
                        Text("댓글 불러오는 중...", style = MaterialTheme.typography.bodySmall)
                    } else if (comments.isEmpty()) {
                        Text(
                            text = "아직 댓글이 없습니다. 첫 댓글을 남겨 보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                isOwner = !currentUserId.isNullOrBlank() && comment.userId == currentUserId,
                                onEdit = {
                                    editingCommentId = comment.id
                                    commentInput = comment.content
                                },
                                onDelete = { onDeleteComment(comment.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ReactionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    isMirrored: Boolean = false
) {
    val contentColor = if (isSelected) Color.White else color
    val containerColor = if (isSelected) color else color.copy(alpha = 0.1f)

    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer(scaleY = if (isMirrored) -1f else 1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label $count", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayCommentAuthor(comment),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isOwner) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "댓글 수정",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "댓글 삭제",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun toiletTypeLabel(type: String): String {
    return when (type.lowercase(Locale.KOREA)) {
        "open" -> "개방 화장실"
        "public" -> "공중 화장실"
        "private" -> "사설 화장실"
        else -> type.ifBlank { "기타 화장실" }
    }
}

private fun displayCommentAuthor(comment: Comment): String {
    val nickname = comment.userNickname.trim()
    if (nickname.isNotBlank()) return nickname
    val userId = comment.userId.trim()
    if (userId.isBlank()) return "익명"
    return if (userId.length <= 8) userId else "${userId.take(8)}..."
}

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.6f", value)
}
