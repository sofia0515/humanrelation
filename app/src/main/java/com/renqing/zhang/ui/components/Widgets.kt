package com.renqing.zhang.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.renqing.zhang.domain.BalanceTone
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.toneOf
import com.renqing.zhang.ui.theme.Amber
import com.renqing.zhang.ui.theme.AmberSoft
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.CinnabarSoft
import com.renqing.zhang.ui.theme.DisabledBg
import com.renqing.zhang.ui.theme.DisabledText
import com.renqing.zhang.ui.theme.Hint
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Pine
import com.renqing.zhang.ui.theme.PineSoft
import com.renqing.zhang.ui.theme.Slate
import com.renqing.zhang.ui.theme.SlateSoft

private data class AvatarTone(val background: Color, val foreground: Color)

private val avatarTones = listOf(
    AvatarTone(Color(0xFFF8E8E4), Color(0xFFC0392B)),
    AvatarTone(Color(0xFFF6EFE6), Amber),
    AvatarTone(Color(0xFFE8EEF5), Slate),
    AvatarTone(Color(0xFFE7F5EE), Pine),
)

private fun avatarTone(name: String): AvatarTone = avatarTones[kotlin.math.abs(name.hashCode()) % avatarTones.size]

@Composable
fun Avatar(letter: String, name: String, size: Int = 40) {
    val tone = avatarTone(name)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(tone.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            color = tone.foreground,
            fontFamily = AmountFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = (size / 2.6f).sp,
        )
    }
}

@Composable
fun ToneChip(net: Long, hasRecords: Boolean) {
    val tone = toneOf(net, hasRecords)
    val (bg, fg, label, amount) = when (tone) {
        BalanceTone.Return -> ToneParts(AmberSoft, Amber, "待回", formatYuan(net))
        BalanceTone.Out -> ToneParts(SlateSoft, Slate, "在外", formatYuan(-net))
        BalanceTone.Even -> ToneParts(PineSoft, Pine, "持平", null)
        BalanceTone.None -> ToneParts(Honey, InkSoft, "还没有往来", null)
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(bg)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        if (amount != null) {
            Text(
                amount,
                color = fg,
                fontFamily = AmountFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private data class ToneParts(val background: Color, val foreground: Color, val label: String, val amount: String?)

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cinnabar,
            contentColor = Color.White,
            disabledContainerColor = DisabledBg,
            disabledContentColor = DisabledText,
        ),
    ) {
        Text(
            text,
            color = if (enabled) Color.White else DisabledText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Ink,
    borderColor: Color = Line,
    height: Dp = 44.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(height / 2),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite, contentColor = contentColor),
    ) {
        Text(text, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PaperDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = "取消",
    onDismissAction: () -> Unit = onDismiss,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(dismissText, onDismissAction, Modifier.weight(1f))
                    PrimaryButton(confirmText, confirmEnabled, onConfirm, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun paperFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Line,
    unfocusedBorderColor = Line,
    disabledBorderColor = Line,
    errorBorderColor = Cinnabar,
    focusedLabelColor = InkSoft,
    unfocusedLabelColor = InkSoft,
    cursorColor = Cinnabar,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

@Composable
fun InlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = InkSoft, fontSize = 16.sp, modifier = Modifier.widthIn(min = 72.dp).padding(end = 12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cinnabar),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (value.isEmpty() && placeholder.isNotEmpty()) onValueChange(placeholder)
                    },
                ),
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) onValueChange(placeholder)
                        true
                    } else {
                        false
                    }
                },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = Hint, fontSize = 16.sp)
                        }
                        inner()
                    }
                },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Honey)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = InkSoft)
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Cinnabar),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = Hint, fontSize = 15.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
fun NoticeBanner(message: String) {
    val accent = if (message.contains("失败") || message.contains("无法") || message.contains("不能") || message.contains("已删除")) {
        Cinnabar
    } else {
        Pine
    }
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(10.dp))
            Text(message, color = Ink, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Cinnabar),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite, contentColor = Cinnabar),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回$title", tint = Ink)
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun SectionLabel(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                color = InkSoft,
                fontSize = 13.sp,
                modifier = Modifier.clickable(enabled = onTrailing != null) { onTrailing?.invoke() },
            )
        }
    }
}

@Composable
fun AmountText(fen: Long, color: Color, size: Int = 28, withSign: Boolean = false, withSymbol: Boolean = true) {
    Text(
        formatYuan(fen, withSign = withSign, withSymbol = withSymbol),
        color = color,
        fontSize = size.sp,
        fontFamily = AmountFamily,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    primary: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(CinnabarSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("礼", color = Cinnabar, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(body, color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (primary != null && onPrimary != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(primary, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
        }
        if (secondary != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(secondary, onSecondary, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ErrorPane(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = Ink, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        GhostButton("重试", onClick = onRetry)
    }
}

@Composable
fun SkeletonList() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Line.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    danger: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(body, color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("取消", onDismiss, Modifier.weight(1f))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (danger) Cinnabar else Pine),
                    ) { Text(confirm, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDelete(
    onDelete: () -> Unit,
    surface: Color = CardWhite,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Cinnabar)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.White)
            }
        },
    ) {
        Box(Modifier.background(surface)) { content() }
    }
}

@Composable
fun Keypad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(Honey)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardWhite)
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "⌫") {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "删除", tint = Ink)
                        } else {
                            Text(key, fontSize = 22.sp, color = Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldRow(label: String, value: String, placeholder: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, color = InkSoft, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardWhite)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                color = if (placeholder) Hint else Ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("›", color = InkSoft, fontSize = 18.sp)
        }
    }
}

@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    selectedColor: Color = Ink,
    onClick: () -> Unit,
) {
    Text(
        text,
        color = if (selected) Color.White else Ink,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) selectedColor else CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
