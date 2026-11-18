package com.renqing.zhang.ui.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.renqing.zhang.domain.Snapshot
import com.renqing.zhang.domain.directionLabel
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

class DeleteRequest {
    var recordId by mutableStateOf<Long?>(null)
    var message by mutableStateOf("")
}

@Composable
fun rememberDeleteRequest(): DeleteHandle {
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    val request = remember { DeleteRequest() }
    return DeleteHandle(
        visible = request.recordId != null,
        message = request.message,
        ask = { snapshot, id ->
            val detail = snapshot.recordDetail(id) ?: return@DeleteHandle
            if (detail.bookId?.let { snapshot.book(it)?.archived } == true) {
                session.toast("这本礼簿已封存，不能删除")
                return@DeleteHandle
            }
            val money = formatYuan(detail.amount, withSymbol = true)
            val action = directionLabel(detail.direction)
            request.message = if (detail.bookTitle != null) {
                "${detail.contactName}在「${detail.bookTitle}」的$action $money 会去掉。亲友差额和礼簿合计会按剩余记录重算。"
            } else {
                "${detail.contactName}的$action $money 会去掉。亲友差额会按剩余记录重算。"
            }
            request.recordId = id
        },
        dismiss = { request.recordId = null },
        confirm = {
            val id = request.recordId ?: return@DeleteHandle
            request.recordId = null
            scope.launch {
                runCatching { repo.deleteRecord(id) }
                    .onSuccess { session.toast("已删除") }
                    .onFailure { session.toast(it.message ?: "删除失败，这条记录还在") }
            }
        },
    )
}

class DeleteHandle(
    private val visible: Boolean,
    private val message: String,
    val ask: (Snapshot, Long) -> Unit,
    private val dismiss: () -> Unit,
    private val confirm: () -> Unit,
) {
    @Composable
    fun Dialog() {
        if (!visible) return
        ConfirmDialog(
            title = "删除这条记录？",
            body = message,
            confirm = "删除",
            onConfirm = confirm,
            onDismiss = dismiss,
        )
    }
}
