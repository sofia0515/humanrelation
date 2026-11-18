package com.renqing.zhang.data

import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.KIND_GIFT
import com.renqing.zhang.domain.Snapshot
import com.renqing.zhang.domain.directionLabel
import com.renqing.zhang.domain.roleLabel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Transfer {
    fun workbook(snapshot: Snapshot, bookId: Long?): ByteArray {
        val sheets = if (bookId == null) {
            listOf(
                Sheet("亲友", contactRows(snapshot)),
                Sheet("礼簿", bookRows(snapshot)),
                Sheet("记录", recordRows(snapshot, null)),
            )
        } else {
            val title = snapshot.book(bookId)?.title ?: "礼簿"
            listOf(Sheet(safeSheet(title), recordRows(snapshot, bookId)))
        }
        return Xlsx.write(sheets)
    }

    fun backupJson(snapshot: Snapshot, settings: Settings): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put(
            "profile",
            JSONObject().put("name", settings.userName).put("phone", settings.userPhone),
        )
        root.put(
            "settings",
            JSONObject()
                .put("remindOn", settings.remindOn)
                .put("threshold", settings.thresholdYuan)
                .put("familyMute", settings.familyMute),
        )
        root.put("groups", JSONArray().apply {
            snapshot.groups.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("key", it.key).put("system", it.system).put("sort", it.sort))
            }
        })
        root.put("types", JSONArray().apply {
            snapshot.types.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("key", it.key).put("system", it.system).put("sort", it.sort))
            }
        })
        root.put("contacts", JSONArray().apply {
            snapshot.contacts.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("name", it.name).put("relation", it.relation)
                        .put("groupId", it.groupId).put("phone", it.phone).put("note", it.note),
                )
            }
        })
        root.put("books", JSONArray().apply {
            snapshot.books.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("title", it.title).put("typeId", it.typeId)
                        .put("role", it.role).put("date", it.date).put("place", it.place)
                        .put("note", it.note).put("archived", it.archived)
                        .put("contactId", it.contactId ?: JSONObject.NULL),
                )
            }
        })
        root.put("records", JSONArray().apply {
            snapshot.records.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("contactId", it.contactId).put("bookId", it.bookId ?: JSONObject.NULL)
                        .put("direction", it.direction).put("kind", it.kind).put("amount", it.amount)
                        .put("giftName", it.giftName).put("date", it.date).put("note", it.note),
                )
            }
        })
        return root.toString(2)
    }

    fun preview(json: String): BackupPreview {
        val root = JSONObject(json)
        return BackupPreview(
            contacts = root.optJSONArray("contacts")?.length() ?: 0,
            books = root.optJSONArray("books")?.length() ?: 0,
            records = root.optJSONArray("records")?.length() ?: 0,
        )
    }

    fun parse(json: String): ParsedBackup {
        val root = JSONObject(json)
        if (root.optInt("version", 1) != 1) error("不认识这份备份")
        val profile = root.optJSONObject("profile")
        val settings = root.optJSONObject("settings")
        return ParsedBackup(
            groups = root.optJSONArray("groups").toList().map {
                GroupEntity(it.getLong("id"), it.getString("name"), it.getString("key"), it.optBoolean("system"), it.optInt("sort"))
            },
            types = root.optJSONArray("types").toList().map {
                BookTypeEntity(it.getLong("id"), it.getString("name"), it.getString("key"), it.optBoolean("system"), it.optInt("sort"))
            },
            contacts = root.optJSONArray("contacts").toList().map {
                ContactEntity(
                    it.getLong("id"), it.getString("name"), it.optString("relation"),
                    it.getLong("groupId"), it.optString("phone"), it.optString("note"),
                )
            },
            books = root.optJSONArray("books").toList().map {
                val linked = it.opt("contactId")
                BookEntity(
                    it.getLong("id"), it.getString("title"), it.getLong("typeId"), it.optString("role", "host"),
                    it.getString("date"), it.optString("place"), it.optString("note"), it.optBoolean("archived"),
                    contactId = if (linked == null || linked == JSONObject.NULL) null else (linked as Number).toLong(),
                )
            },
            records = root.optJSONArray("records").toList().map {
                val book = it.opt("bookId")
                RecordEntity(
                    id = it.getLong("id"),
                    contactId = it.getLong("contactId"),
                    bookId = if (book == null || book == JSONObject.NULL) null else (book as Number).toLong(),
                    direction = it.getString("direction"),
                    kind = it.optString("kind", "cash"),
                    amount = it.getLong("amount"),
                    giftName = it.optString("giftName"),
                    date = it.getString("date"),
                    note = it.optString("note"),
                )
            },
            remindOn = settings?.optBoolean("remindOn", true) ?: true,
            threshold = settings?.optInt("threshold", 500) ?: 500,
            familyMute = settings?.optBoolean("familyMute", true) ?: true,
            userName = profile?.optString("name", "林晚") ?: "林晚",
            userPhone = profile?.optString("phone", "") ?: "",
        )
    }

    private fun contactRows(snapshot: Snapshot): List<List<String>> {
        val header = listOf("姓名", "关系", "分组", "手机", "备注", "收到", "送出", "净额")
        val body = snapshot.contacts.map { person ->
            val bal = snapshot.balance(person.id)
            listOf(
                person.name,
                person.relation,
                snapshot.group(person.groupId)?.name.orEmpty(),
                person.phone,
                person.note,
                yuan(bal.received),
                yuan(bal.given),
                yuan(bal.net),
            )
        }
        return listOf(header) + body
    }

    private fun bookRows(snapshot: Snapshot): List<List<String>> {
        val header = listOf("名称", "类型", "身份", "日期", "地点", "状态", "备注", "笔数", "合计")
        val today = java.time.LocalDate.now()
        val body = snapshot.books.map { book ->
            val rows = snapshot.recordsOfBook(book.id)
            val total = rows.sumOf { it.amount }
            listOf(
                book.title,
                snapshot.type(book.typeId)?.name.orEmpty(),
                roleLabel(book.role),
                book.date,
                book.place,
                if (snapshot.isPlanning(book, today)) "筹备中" else "已结束",
                book.note,
                rows.size.toString(),
                yuan(total),
            )
        }
        return listOf(header) + body
    }

    private fun recordRows(snapshot: Snapshot, bookId: Long?): List<List<String>> {
        val header = listOf("日期", "亲友", "关系", "礼簿", "方向", "形式", "礼品", "金额", "备注")
        val body = snapshot.records
            .filter { bookId == null || it.bookId == bookId }
            .sortedWith(compareBy({ it.date }, { it.id }))
            .map { row ->
                val person = snapshot.contact(row.contactId)
                listOf(
                    row.date,
                    person?.name.orEmpty(),
                    person?.relation.orEmpty(),
                    row.bookId?.let { snapshot.book(it)?.title }.orEmpty().ifBlank { "未归入礼簿" },
                    directionLabel(row.direction),
                    if (row.kind == KIND_GIFT) "礼品" else "现金",
                    row.giftName,
                    yuan(if (row.direction == DIR_IN) row.amount else -row.amount),
                    row.note,
                )
            }
        return listOf(header) + body
    }

    private fun safeSheet(name: String): String =
        name.replace(Regex("""[:\\/?*\[\]]"""), " ").trim().take(31).ifBlank { "礼簿" }

    private fun yuan(fen: Long): String {
        val sign = if (fen < 0) "-" else ""
        val abs = kotlin.math.abs(fen)
        val cent = (abs % 100).toInt()
        return if (cent == 0) "$sign${abs / 100}" else "$sign${abs / 100}.${cent.toString().padStart(2, '0')}"
    }
}

private fun JSONArray?.toList(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) add(getJSONObject(i))
    }
}

private data class Sheet(val name: String, val rows: List<List<String>>)

private object Xlsx {
    fun write(sheets: List<Sheet>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.put("［Content_Types］.xml".replace('［', '[').replace('］', ']'), contentTypes(sheets.size))
            zip.put("_rels/.rels", rels())
            zip.put("xl/workbook.xml", workbook(sheets))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", styles())
            sheets.forEachIndexed { index, sheet ->
                zip.put("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.put(name: String, xml: String) {
        putNextEntry(ZipEntry(name))
        write(xml.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(count: Int): String {
        val sheets = (1..count).joinToString("") {
            """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$sheets
</Types>"""
    }

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook(sheets: List<Sheet>): String {
        val items = sheets.mapIndexed { index, sheet ->
            """<sheet name="${xml(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$items</sheets>
</workbook>"""
    }

    private fun workbookRels(count: Int): String {
        val items = (1..count).joinToString("") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>"""
        } + """<Relationship Id="rIdStyle" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$items</Relationships>"""
    }

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""

    private fun sheetXml(sheet: Sheet): String {
        val rows = sheet.rows.mapIndexed { r, cells ->
            val cols = cells.mapIndexed { c, value ->
                val ref = columnName(c) + (r + 1)
                if (value.toDoubleOrNull() != null && r > 0) {
                    """<c r="$ref"><v>${xml(value)}</v></c>"""
                } else {
                    """<c r="$ref" t="inlineStr"><is><t>${xml(value)}</t></is></c>"""
                }
            }.joinToString("")
            """<row r="${r + 1}">$cols</row>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$rows</sheetData></worksheet>"""
    }

    private fun columnName(index: Int): String {
        var n = index
        val chars = StringBuilder()
        do {
            chars.append(('A'.code + n % 26).toChar())
            n = n / 26 - 1
        } while (n >= 0)
        return chars.reverse().toString()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
