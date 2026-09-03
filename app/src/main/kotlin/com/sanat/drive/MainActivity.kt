package com.sanat.drive
import android.widget.FrameLayout


import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

import android.widget.ScrollView
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var store: DataStore

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var drawer: LinearLayout

    private val navy = 0xFF17233C.toInt()
    private val deepNavy = 0xFF101A2E.toInt()
    private val gold = 0xFFB9954B.toInt()
    private val ivory = 0xFFFAF8F3.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val text = 0xFF202638.toInt()
    private val secondary = 0xFF697184.toInt()
    private val border = 0xFFE3DED2.toInt()
    private val success = 0xFF287A58.toInt()
    private val danger = 0xFFA94442.toInt()

    private val inr: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }

    private var currentPage = "My Notes"
    private val REQUEST_PICK_FILE = 700
    
    private var pendingSelectedUri: Uri? = null
    
    private var pendingFileTitleInput: EditText? = null
    
    private var pendingSelectedFileText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = DataStore(this)

        showPinScreen()
    }

    // ========================================================
    // PIN
    // ========================================================

    private fun showPinScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(30), dp(30), dp(30))
            setBackgroundColor(ivory)
        }

        val title = TextView(this).apply {
            text = "SKD Data Drive"
            textSize = 30f
            typeface = Typeface.create("serif", Typeface.BOLD)
            setTextColor(navy)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Secure Personal Data Vault"
            textSize = 15f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(26))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(25), dp(24), dp(25))
            background = rounded(white, border, 18)
            elevation = dp(4).toFloat()
        }

        val pin = EditText(this).apply {
            hint = "Enter 6 digit PIN"
            textSize = 19f
            gravity = Gravity.CENTER
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        }

        val enter = button("Unlock", navy)

        val message = TextView(this).apply {
            setTextColor(danger)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }

        enter.setOnClickListener {
            val value = pin.text.toString()

            if (value.length != 6) {
                message.text = "Please enter exactly 6 digits."
            } else if (store.verifyPin(value)) {
                showMainApp()
            } else {
                message.text = "Incorrect PIN."
                pin.text.clear()
            }
        }

        card.addView(
            pin,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        card.addView(
            enter,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                topMargin = dp(16)
            }
        )

        card.addView(
            message,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        outer.addView(title)
        outer.addView(subtitle)
        outer.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(outer)
    }

    // ========================================================
    // Main shell
    // ========================================================

    private fun showMainApp() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ivory)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(15), 0)
            setBackgroundColor(navy)
            elevation = dp(5).toFloat()
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 28f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(12), 0)
            isClickable = true
        }

        menuButton.setOnClickListener {
            drawer.visibility =
                if (drawer.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }

        val title = TextView(this).apply {
            text = "SKD Data Drive"
            textSize = 21f
            typeface = Typeface.create("serif", Typeface.BOLD)
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            menuButton,
            LinearLayout.LayoutParams(
                dp(52),
                dp(64)
            )
        )

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(64),
                1f
            )
        )

        val shell = FrameLayout(this)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(ivory)
        }
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
        }
        
        scrollView.addView(content)

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(white, border, 18)
            elevation = dp(10).toFloat()
            visibility = View.GONE
        }

        val navItems = listOf(
            "My Notes",
            "My Portfolio",
            "My Credentials",
            "Upload Files",
            "Add Note",
            "Add SIP",
            "Add Credentials",
            "Export/Restore All",
            "Admin Panel"
        )

        navItems.forEach { name ->
            val item = TextView(this).apply {
                text = name
                textSize = 15f
                setTextColor(navy)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(10), 0)
                background = rounded(ivory, border, 10)
                isClickable = true
            }

            item.setOnClickListener {
                drawer.visibility = View.GONE
                currentPage = name
                renderPage(name)
            }

            drawer.addView(
                item,
                LinearLayout.LayoutParams(
                    dp(230),
                    dp(46)
                ).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        shell.addView(
            scrollView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        shell.addView(
            drawer,
            FrameLayout.LayoutParams(
                dp(250),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply {
                topMargin = dp(8)
                leftMargin = dp(8)
            }
        )

        val footer = TextView(this).apply {
            text = "@ 2026 Built & Developed by Sanat Dey"
            textSize = 12f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
            setBackgroundColor(deepNavy)
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        )

        root.addView(
            shell,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        root.addView(
            footer,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        setContentView(root)

        renderPage("My Notes")
    }

    // ========================================================
    // Page renderer
    // ========================================================

    private fun renderPage(page: String) {
        content.removeAllViews()

        when (page) {
            "My Notes" -> notesPage()
            "My Portfolio" -> portfolioPage()
            "My Credentials" -> credentialsPage()
            "Upload Files" -> uploadFilesPage()
            "Add Note" -> addNotePage()
            "Add SIP" -> addSipPage()
            "Add Credentials" -> addCredentialsPage()
            "Export/Restore All" -> exportRestorePage()
            "Admin Panel" -> adminPage()
        }
    }

    // ========================================================
    // My Notes
    // ========================================================

    private fun notesPage() {
        pageTitle("My Notes", "Recently saved notes")

        val notes = store.getNotes().asReversed()

        if (notes.isEmpty()) {
            emptyMessage("No notes saved yet.")
            return
        }

        notes.forEach { note ->
            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
                background = rounded(white, border, 14)
                isClickable = true
            }

            val pen = TextView(this).apply {
                text = "✎"
                textSize = 23f
                setTextColor(gold)
                gravity = Gravity.CENTER
            }

            val title = TextView(this).apply {
                text = note.title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(this@MainActivity.text)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, 0, 0)
            }

            titleRow.addView(
                pen,
                LinearLayout.LayoutParams(
                    dp(35),
                    dp(54)
                )
            )

            titleRow.addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    dp(54),
                    1f
                )
            )

            val details = TextView(this).apply {
                text = fromHtml(note.html)
                textSize = 15f
                setTextColor(this@MainActivity.text)
                setPadding(dp(18), dp(12), dp(18), dp(16))
                visibility = View.GONE
                background = rounded(white, border, 14)
                setLineSpacing(dp(3).toFloat(), 1f)
                setTextIsSelectable(true)
            }

            titleRow.setOnClickListener {
                details.visibility =
                    if (details.visibility == View.VISIBLE)
                        View.GONE
                    else
                        View.VISIBLE
            }

            content.addView(
                titleRow,
                LinearLayout.LayoutParams(
                    -1,
                    dp(54)
                ).apply {
                    bottomMargin = dp(3)
                }
            )

            content.addView(
                details,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )
        }
    }

    // ========================================================
    // Portfolio
    // ========================================================

    private fun portfolioPage() {
        pageTitle("My Portfolio", "Assets and investment history")

        val angel = store.getAngelValue()
        val total = store.getSips().sumOf { it.amount }

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val angelCard = card()
        val totalCard = card()

        val angelText = if (angel == null) {
            "₹0.00"
        } else {
            inr.format(angel.second)
        }

        val angelDate = angel?.first ?: "Not updated"

        angelCard.addView(
            label("Angel One Portfolio"),
            LinearLayout.LayoutParams(-1, dp(25))
        )

        angelCard.addView(
            value(angelText),
            LinearLayout.LayoutParams(-1, dp(35))
        )

        angelCard.addView(
            small("Updated: $angelDate"),
            LinearLayout.LayoutParams(-1, dp(30))
        )

        totalCard.addView(
            label("Total Invested"),
            LinearLayout.LayoutParams(-1, dp(25))
        )

        totalCard.addView(
            value(inr.format(total)),
            LinearLayout.LayoutParams(-1, dp(35))
        )

        summary.addView(
            angelCard,
            LinearLayout.LayoutParams(
                0,
                dp(125),
                1f
            ).apply {
                rightMargin = dp(5)
            }
        )

        summary.addView(
            totalCard,
            LinearLayout.LayoutParams(
                0,
                dp(125),
                1f
            ).apply {
                leftMargin = dp(5)
            }
        )

        content.addView(
            summary,
            LinearLayout.LayoutParams(
                -1,
                dp(125)
            )
        )

        spacer(10)

        val assets = store.getAssets()
        val sips = store.getSips()

        if (assets.isEmpty()) {
            emptyMessage("No assets have been created from Admin Panel.")
            return
        }

        assets.forEach { asset ->
            val assetTotal =
                sips.filter { it.assetId == asset.id }
                    .sumOf { it.amount }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(11), dp(15), dp(11))
                background = rounded(white, border, 14)
                isClickable = true
            }

            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val name = TextView(this).apply {
                text = asset.name
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(this@MainActivity.text)
            }

            val amount = TextView(this).apply {
                text = inr.format(assetTotal)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(navy)
                gravity = Gravity.END
            }

            top.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(35),
                    1f
                )
            )

            top.addView(
                amount,
                LinearLayout.LayoutParams(
                    dp(130),
                    dp(35)
                )
            )

            val category = small(asset.category)

            row.addView(
                top,
                LinearLayout.LayoutParams(
                    -1,
                    dp(35)
                )
            )

            row.addView(category)

            val entries = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dp(8), dp(8), dp(8), 0)
            }

            sips.filter { it.assetId == asset.id }
                .forEach { sip ->
                    entries.addView(
                        small(
                            "${sip.date}   •   ${inr.format(sip.amount)}"
                        )
                    )
                }

            row.setOnClickListener {
                entries.visibility =
                    if (entries.visibility == View.VISIBLE)
                        View.GONE
                    else
                        View.VISIBLE
            }

            content.addView(
                row,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )

            content.addView(entries)
        }
    }

    // ========================================================
    // Credentials
    // ========================================================

    private fun credentialsPage() {
        pageTitle("My Credentials", "Saved account information")

        val credentials = store.getCredentials()

        if (credentials.isEmpty()) {
            emptyMessage("No credentials saved yet.")
            return
        }

        credentials.forEach { c ->
            val box = card()

            box.addView(
                label(c.account),
                LinearLayout.LayoutParams(-1, dp(30))
            )

            addCopyField(box, "UserID", c.userId)
            addCopyField(box, "Password", c.password)
            addCopyField(box, "Other-1", c.other1)
            addCopyField(box, "Other-2", c.other2)

            content.addView(
                box,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            )
        }
    }

    private fun addCopyField(
        parent: LinearLayout,
        title: String,
        data: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tv = TextView(this).apply {
            text = "$title: $data"
            textSize = 14f
            setTextColor(this@MainActivity.text)
            setPadding(0, dp(5), 0, dp(5))
        }

        val copy = button("Copy", navy)

        copy.setOnClickListener {
            val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(title, data)
            )

            Toast.makeText(
                this,
                "$title copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        row.addView(
            tv,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(
            copy,
            LinearLayout.LayoutParams(
                dp(72),
                dp(38)
            )
        )

        parent.addView(row)
    }

    // ========================================================
    // Add Note
    // ========================================================

    private fun addNotePage() {
        pageTitle("Add Note", "Create and manage formatted notes")

        val title = input("Note Title")
        val details = EditText(this).apply {
            hint = "Note details"
            textSize = 16f
            gravity = Gravity.TOP or Gravity.START
            minLines = 8
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(white, border, 12)
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val bold = button("B", navy).apply {
            typeface = Typeface.DEFAULT_BOLD
            isFocusable = false
            isFocusableInTouchMode = false
        }
        
        val underline = button("U", navy).apply {
            paintFlags =
                paintFlags or
                    android.graphics.Paint.UNDERLINE_TEXT_FLAG
        
            isFocusable = false
            isFocusableInTouchMode = false
        }
        
        bold.setOnClickListener {
        
            val start = details.selectionStart
            val end = details.selectionEnd
        
            if (start == end) {
                toast("Please select text first.")
                return@setOnClickListener
            }
        
            details.text.setSpan(
                android.text.style.StyleSpan(Typeface.BOLD),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        
            details.requestFocus()
            details.setSelection(start, end)
        }
        
        underline.setOnClickListener {
        
            val start = details.selectionStart
            val end = details.selectionEnd
        
            if (start == end) {
                toast("Please select text first.")
                return@setOnClickListener
            }
        
            details.text.setSpan(
                android.text.style.UnderlineSpan(),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        
            details.requestFocus()
            details.setSelection(start, end)
        }

        toolbar.addView(
            bold,
            LinearLayout.LayoutParams(dp(55), dp(45)).apply {
                rightMargin = dp(6)
            }
        )

        toolbar.addView(
            underline,
            LinearLayout.LayoutParams(dp(55), dp(45))
        )

        val save = button("Add Note", navy)

        save.setOnClickListener {
            if (title.text.toString().trim().isEmpty()) {
                toast("Enter a note title.")
                return@setOnClickListener
            }

            store.addNote(
                title.text.toString(),
                Html.toHtml(
                    details.text,
                    Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
                )
            )

            title.text.clear()
            details.text.clear()

            toast("Note added successfully.")
        }

        content.addView(title)
        content.addView(toolbar)
        content.addView(
            details,
            LinearLayout.LayoutParams(
                -1,
                dp(230)
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(12)
            }
        )
        content.addView(save)

        spacer(18)

        val editDelete = button(
            "Edit / Delete Note",
            deepNavy
        )

        editDelete.setOnClickListener {
            noteEditorDialog()
        }

        content.addView(editDelete)
    }

    private fun noteEditorDialog() {
        val notes = store.getNotes()

        if (notes.isEmpty()) {
            toast("No notes available.")
            return
        }

        val names = notes.map { it.title }.toTypedArray()

        AlertDialogBuilder().setTitle("Choose Note")
            .setItems(names) { _, which ->
                editNoteDialog(notes[which])
            }
            .show()
    }

    private fun editNoteDialog(note: Note) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(5), dp(20), 0)
        }

        val title = input("Note Title").apply {
            setText(note.title)
        }

        val details = EditText(this).apply {
            setText(fromHtml(note.html))
            textSize = 16f
            minLines = 8
            gravity = Gravity.TOP
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val bold = button("B", navy).apply {
            typeface = Typeface.DEFAULT_BOLD
            isFocusable = false
            isFocusableInTouchMode = false
        }
        
        val underline = button("U", navy).apply {
            paintFlags =
                paintFlags or
                    android.graphics.Paint.UNDERLINE_TEXT_FLAG
        
            isFocusable = false
            isFocusableInTouchMode = false
        }
        
        bold.setOnClickListener {
        
            val start = details.selectionStart
            val end = details.selectionEnd
        
            if (start == end) {
                toast("Please select text first.")
                return@setOnClickListener
            }
        
            details.text.setSpan(
                android.text.style.StyleSpan(Typeface.BOLD),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        
            details.requestFocus()
            details.setSelection(start, end)
        }
        
        underline.setOnClickListener {
        
            val start = details.selectionStart
            val end = details.selectionEnd
        
            if (start == end) {
                toast("Please select text first.")
                return@setOnClickListener
            }
        
            details.text.setSpan(
                android.text.style.UnderlineSpan(),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        
            details.requestFocus()
            details.setSelection(start, end)
        }
        
        toolbar.addView(
            bold,
            LinearLayout.LayoutParams(dp(55), dp(45)).apply {
                rightMargin = dp(6)
            }
        )
        
        toolbar.addView(
            underline,
            LinearLayout.LayoutParams(dp(55), dp(45))
        )
        
        box.addView(title)
        box.addView(toolbar)
        box.addView(details)

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Note")
            .setView(box)
            .setPositiveButton("Update") { _, _ ->
                store.updateNote(
                    note.id,
                    title.text.toString(),
                    Html.toHtml(details.text)
                )
                renderPage("Add Note")
                toast("Note updated.")
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                store.deleteNote(note.id)
                renderPage("Add Note")
                toast("Note deleted.")
            }
            .show()
    }

    // ========================================================
    // Add SIP
    // ========================================================

    private fun addSipPage() {
        pageTitle("Add SIP", "Investment entries")

        val assets = store.getAssets()

        if (assets.isEmpty()) {
            emptyMessage("Create assets first from Admin Panel.")
            return
        }

        assets.forEach { asset ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), 0, dp(8), 0)
                background = rounded(white, border, 14)
            }

            val name = TextView(this).apply {
                text = "${asset.name}\n${asset.category}"
                textSize = 15f
                setTextColor(this@MainActivity.text)
                gravity = Gravity.CENTER_VERTICAL
            }

            val add = button("New Entry", navy)

            add.setOnClickListener {
                sipEntryDialog(asset)
            }

            row.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(68),
                    1f
                )
            )

            row.addView(
                add,
                LinearLayout.LayoutParams(
                    dp(110),
                    dp(45)
                )
            )

            content.addView(
                row,
                LinearLayout.LayoutParams(
                    -1,
                    dp(68)
                ).apply {
                    bottomMargin = dp(9)
                }
            )
        }

        spacer(12)

        val editSip = button(
            "Edit SIP Entry",
            deepNavy
        )

        editSip.setOnClickListener {
            sipEditorChooser()
        }

        content.addView(editSip)
    }

    private fun sipEntryDialog(asset: Asset) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }

        val date = input("Date (DD-MM-YYYY)")
        val amount = input("Amount in ₹").apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        box.addView(date)
        box.addView(amount)

        android.app.AlertDialog.Builder(this)
            .setTitle("New Entry • ${asset.name}")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val value = amount.text.toString().toDoubleOrNull()

                if (date.text.toString().trim().isEmpty() ||
                    value == null ||
                    value <= 0
                ) {
                    toast("Enter valid date and amount.")
                    return@setPositiveButton
                }

                store.addSip(
                    asset.id,
                    date.text.toString(),
                    value
                )

                /*
                 * Important:
                 * remain on Add SIP page after submission.
                 */
                renderPage("Add SIP")
                toast("Investment entry saved.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sipEditorChooser() {
        val sips = store.getSips()
        val assets = store.getAssets()

        if (sips.isEmpty()) {
            toast("No SIP entries available.")
            return
        }

        val choices = sips.map { sip ->
            val asset =
                assets.find { it.id == sip.assetId }?.name
                    ?: "Deleted Asset"

            "$asset • ${sip.date} • ${inr.format(sip.amount)}"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Select SIP Entry")
            .setItems(choices) { _, which ->
                editSipDialog(sips[which])
            }
            .show()
    }

    private fun editSipDialog(sip: SipEntry) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }

        val date = input("Date").apply {
            setText(sip.date)
        }

        val amount = input("Amount in ₹").apply {
            setText(sip.amount.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        box.addView(date)
        box.addView(amount)

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit SIP Entry")
            .setView(box)
            .setPositiveButton("Update") { _, _ ->
                val value = amount.text.toString().toDoubleOrNull()

                if (value == null || value <= 0) {
                    toast("Enter a valid amount.")
                    return@setPositiveButton
                }

                store.updateSip(
                    sip.id,
                    date.text.toString(),
                    value
                )

                renderPage("Add SIP")
                toast("SIP entry updated.")
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                store.deleteSip(sip.id)
                renderPage("Add SIP")
                toast("SIP entry deleted.")
            }
            .show()
    }

    // ========================================================
    // Add Credentials
    // ========================================================

    private fun addCredentialsPage() {
        pageTitle(
            "Add Credentials",
            "Store account information"
        )

        val account = input("For What Account")
        val user = input("UserID")
        val password = input("Password").apply {
            inputType =
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val other1 = input("Other-1")
        val other2 = input("Other-2")

        val save = button("Add Credentials", navy)

        save.setOnClickListener {
            if (account.text.toString().trim().isEmpty()) {
                toast("Enter account name.")
                return@setOnClickListener
            }

            store.addCredential(
                account.text.toString(),
                user.text.toString(),
                password.text.toString(),
                other1.text.toString(),
                other2.text.toString()
            )

            account.text.clear()
            user.text.clear()
            password.text.clear()
            other1.text.clear()
            other2.text.clear()

            toast("Credential saved.")
        }

        listOf(account, user, password, other1, other2)
            .forEach { content.addView(it) }

        content.addView(save)

        spacer(15)

        val edit = button(
            "Edit / Delete Credentials",
            deepNavy
        )

        edit.setOnClickListener {
            credentialChooser()
        }

        content.addView(edit)
    }

    private fun credentialChooser() {
        val list = store.getCredentials()

        if (list.isEmpty()) {
            toast("No credentials available.")
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Choose Account")
            .setItems(
                list.map { it.account }.toTypedArray()
            ) { _, which ->
                credentialEditor(list[which])
            }
            .show()
    }

    private fun credentialEditor(c: Credential) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }

        val account = input("For What Account").apply {
            setText(c.account)
        }

        val user = input("UserID").apply {
            setText(c.userId)
        }

        val password = input("Password").apply {
            setText(c.password)
        }

        val other1 = input("Other-1").apply {
            setText(c.other1)
        }

        val other2 = input("Other-2").apply {
            setText(c.other2)
        }

        listOf(account, user, password, other1, other2)
            .forEach { box.addView(it) }

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Credential")
            .setView(box)
            .setPositiveButton("Update") { _, _ ->
                store.updateCredential(
                    c.id,
                    account.text.toString(),
                    user.text.toString(),
                    password.text.toString(),
                    other1.text.toString(),
                    other2.text.toString()
                )
                renderPage("Add Credentials")
                toast("Credential updated.")
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                store.deleteCredential(c.id)
                renderPage("Add Credentials")
                toast("Credential deleted.")
            }
            .show()
    }



    // ========================================================
    // Upload Files
    // ========================================================

    private fun uploadFilesPage() {
        pageTitle(
            "Upload Files",
            "Store and open your important files"
        )

        val upload = button("Upload Your File", navy)

        upload.setOnClickListener {
            openUploadForm()
        }

        content.addView(
            upload,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        spacer(16)

        val files = store.getDriveFiles().asReversed()

        if (files.isEmpty()) {
            emptyMessage(
                "No files uploaded yet."
            )
            return
        }

        files.forEach { driveFile ->

            val box = card()

            val title = TextView(this).apply {
                text = driveFile.title
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(this@MainActivity.text)
            }

            val fileName = TextView(this).apply {
                text = driveFile.originalName
                textSize = 13f
                setTextColor(secondary)
                setPadding(0, dp(5), 0, dp(2))
            }

            val date = TextView(this).apply {
                text = "Uploaded: ${driveFile.date}"
                textSize = 12f
                setTextColor(secondary)
            }

            val buttons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val open = button("Open", navy)

            val delete = button("Delete", danger)

            open.setOnClickListener {
                openDriveFile(driveFile)
            }

            delete.setOnClickListener {

                AlertDialogBuilder()
                    .setTitle("Delete File")
                    .setMessage(
                        "Are you sure you want to delete '${driveFile.title}'?"
                    )
                    .setPositiveButton("Delete") { _, _ ->

                        val file = getStoredFile(
                            driveFile.storedName
                        )

                        if (file.exists()) {
                            file.delete()
                        }

                        store.deleteDriveFile(
                            driveFile.id
                        )

                        renderPage("Upload Files")

                        toast("File deleted.")
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            buttons.addView(
                open,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                ).apply {
                    rightMargin = dp(5)
                }
            )

            buttons.addView(
                delete,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                ).apply {
                    leftMargin = dp(5)
                }
            )

            box.addView(title)
            box.addView(fileName)
            box.addView(date)

            box.addView(
                buttons,
                LinearLayout.LayoutParams(
                    -1,
                    dp(55)
                ).apply {
                    topMargin = dp(10)
                }
            )

            content.addView(
                box,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            )
        }
    }


    private fun openUploadForm() {

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(22),
                dp(18),
                dp(22),
                dp(18)
            )
        }

        val titleInput = input("Enter File Title")

        val selectedFileText = TextView(this).apply {
            text = "No file selected"
            textSize = 14f
            setTextColor(secondary)
            setPadding(0, dp(8), 0, dp(8))
        }

        val selectFile = button(
            "Select File",
            deepNavy
        )

        var selectedUri: Uri? = null

        selectFile.setOnClickListener {

            pendingFileTitleInput = titleInput
            pendingSelectedFileText = selectedFileText

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                    type = "*/*"

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )
                }

            startActivityForResult(
                intent,
                REQUEST_PICK_FILE
            )
        }

        dialogContent.addView(titleInput)

        dialogContent.addView(
            selectFile,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        dialogContent.addView(
            selectedFileText
        )

        val dialog =
            AlertDialogBuilder()
                .setTitle("Upload Your File")
                .setView(dialogContent)
                .setPositiveButton(
                    "Upload",
                    null
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                selectedUri = pendingSelectedUri

                val title =
                    titleInput.text.toString().trim()

                if (title.isEmpty()) {
                    toast("Please enter a title.")
                    return@setOnClickListener
                }

                if (selectedUri == null) {
                    toast("Please select a file.")
                    return@setOnClickListener
                }

                try {

                    saveSelectedFile(
                        selectedUri!!,
                        title
                    )

                    pendingSelectedUri = null
                    pendingFileTitleInput = null
                    pendingSelectedFileText = null

                    dialog.dismiss()

                    renderPage("Upload Files")

                    toast(
                        "File uploaded successfully."
                    )

                } catch (e: Exception) {

                    toast(
                        "Unable to upload the selected file."
                    )
                }
            }
        }

        dialog.show()
    }


    private fun getUploadDirectory(): File {

        val directory =
            File(filesDir, "uploaded_files")

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    private fun getStoredFile(
        storedName: String
    ): File =
        File(
            getUploadDirectory(),
            storedName
        )
	private fun saveSelectedFile(
	    uri: Uri,
	    title: String
	) {

	    val originalName =
	        getFileName(uri)

	    val extension =
	        getFileExtension(originalName)

	    val storedName =
	        if (extension.isEmpty()) {
	            UUID.randomUUID().toString()
	        } else {
	            "${UUID.randomUUID()}.$extension"
	        }

	    val destination =
	        getStoredFile(storedName)

	    contentResolver
	        .openInputStream(uri)
	        ?.use { input ->

	            FileOutputStream(destination)
	                .use { output ->

	                    input.copyTo(output)
	                }
	        }
	        ?: throw Exception(
	            "Unable to read selected file"
	        )

	    val mimeType =
	        contentResolver.getType(uri)
	            ?: "application/octet-stream"

	    store.addDriveFile(
	        title = title,
	        originalName = originalName,
	        storedName = storedName,
	        mimeType = mimeType
	    )
	}

	private fun getFileName(
	    uri: Uri
	): String {

	    var result = "Unknown File"

	    if (uri.scheme == "content") {

	        contentResolver
	            .query(
	                uri,
	                null,
	                null,
	                null,
	                null
	            )
	            ?.use { cursor ->

	                val index =
	                    cursor.getColumnIndex(
	                        android.provider.OpenableColumns.DISPLAY_NAME
	                    )

	                if (
	                    index >= 0 &&
	                    cursor.moveToFirst()
	                ) {
	                    result =
	                        cursor.getString(index)
	                }
	            }
	    }

	    if (result == "Unknown File") {
	        uri.path
	            ?.substringAfterLast("/")
	            ?.let {
	                result = it
	            }
	    }

	    return result
	}

	private fun getFileExtension(
	    fileName: String
	): String {

	    val index =
	        fileName.lastIndexOf(".")

	    return if (
	        index >= 0 &&
	        index < fileName.length - 1
	    ) {
	        fileName.substring(index + 1)
	    } else {
	        ""
	    }
	}

	private fun openDriveFile(
	    driveFile: DriveFile
	) {

	    val file =
	        getStoredFile(
	            driveFile.storedName
	        )

	    if (!file.exists()) {

	        toast(
	            "The stored file could not be found."
	        )

	        return
	    }

	    try {

	        val uri =
	            FileProvider.getUriForFile(
	                this,
	                "$packageName.fileprovider",
	                file
	            )

	        val intent =
	            Intent(Intent.ACTION_VIEW).apply {

	                setDataAndType(
	                    uri,
	                    driveFile.mimeType
	                )

	                addFlags(
	                    Intent.FLAG_GRANT_READ_URI_PERMISSION
	                )
	            }

	        startActivity(
	            Intent.createChooser(
	                intent,
	                "Open with"
	            )
	        )

	    } catch (_: ActivityNotFoundException) {

	        toast(
	            "No compatible app is installed to open this file."
	        )

	    } catch (_: Exception) {

	        toast(
	            "Unable to open this file."
	        )
	    }
	}

	

    // ========================================================
    // Export / Restore
    // ========================================================

    private fun exportRestorePage() {
        pageTitle(
            "Export / Restore All",
            "Complete application backup"
        )

        val export = button("Export All Data", navy)
        val restore = button("Restore All Data", deepNavy)

        export.setOnClickListener {
            if (!isOnline()) {
                toast("You are offline, Connect to internet")
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/zip"
            
                putExtra(
                    Intent.EXTRA_TITLE,
                    "SKD_Data_Drive_Backup.zip"
                )
            
                addCategory(
                    Intent.CATEGORY_OPENABLE
                )
            }

            startActivityForResult(intent, 500)
        }

        restore.setOnClickListener {
            if (!isOnline()) {
                toast("You are offline, Connect to internet")
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/zip"
            
                addCategory(
                    Intent.CATEGORY_OPENABLE
                )
            }

            startActivityForResult(intent, 501)
        }

        content.addView(export)
        spacer(10)
        content.addView(restore)

        spacer(20)

        val note = TextView(this).apply {
            text =
                "Export and Restore use Android's official Storage Access Framework. " +
                    "Your phone's system file manager will be opened."
            textSize = 14f
            setTextColor(secondary)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(white, border, 12)
        }

        content.addView(note)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data?.data == null) return

        try {
            when (requestCode) {
            	REQUEST_PICK_FILE -> {

            	    pendingSelectedUri =
            	        data.data

            	    val name =
            	        getFileName(
            	            pendingSelectedUri!!
            	        )

            	    pendingSelectedFileText?.text =
            	        "Selected: $name"
            	}
                500 -> {
                
                    contentResolver
                        .openOutputStream(data.data!!)
                        ?.use { output ->
                
                            exportCompleteBackup(output)
                        }
                
                    toast(
                        "All data and uploaded files exported successfully."
                    )
                }

                501 -> {
                
                    val restored =
                        contentResolver
                            .openInputStream(data.data!!)
                            ?.use { input ->
                
                                restoreCompleteBackup(input)
                            }
                            ?: false
                
                    if (restored) {
                
                        renderPage(currentPage)
                
                        toast(
                            "All data and uploaded files restored successfully."
                        )
                
                    } else {
                
                        toast(
                            "Invalid SKD Data Drive backup."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            toast("Unable to process the selected file.")
        }
    }


    private fun exportCompleteBackup(
        output: java.io.OutputStream
    ) {

        ZipOutputStream(output).use { zip ->

            // --------------------------------------------
            // Application data
            // --------------------------------------------

            zip.putNextEntry(
                ZipEntry("backup.json")
            )

            zip.write(
                store.exportAll()
                    .toByteArray(
                        Charsets.UTF_8
                    )
            )

            zip.closeEntry()

            // --------------------------------------------
            // Uploaded files
            // --------------------------------------------

            val directory =
                getUploadDirectory()

            directory.listFiles()
                ?.filter { it.isFile }
                ?.forEach { file ->

                    zip.putNextEntry(
                        ZipEntry(
                            "uploaded_files/${file.name}"
                        )
                    )

                    FileInputStream(file)
                        .use { input ->

                            input.copyTo(zip)
                        }

                    zip.closeEntry()
                }
        }
    }

    private fun restoreCompleteBackup(
        input: java.io.InputStream
    ): Boolean {

        return try {

            var json: String? = null

            val temporaryFiles =
                mutableListOf<Pair<String, ByteArray>>()

            ZipInputStream(input).use { zip ->

                var entry =
                    zip.nextEntry

                while (entry != null) {

                    when {

                        entry.name == "backup.json" -> {

                            json =
                                zip.bufferedReader()
                                    .readText()
                        }

                        entry.name.startsWith(
                            "uploaded_files/"
                        ) -> {

                            val fileName =
                                entry.name.substringAfterLast("/")

                            if (fileName.isNotEmpty()) {

                                val bytes =
                                    zip.readBytes()

                                temporaryFiles +=
                                    fileName to bytes
                            }
                        }
                    }

                    zip.closeEntry()

                    entry =
                        zip.nextEntry
                }
            }

            if (
                json == null ||
                !store.restoreAll(json!!)
            ) {
                return false
            }

            val directory =
                getUploadDirectory()

            directory.listFiles()
                ?.forEach { file ->

                    if (file.isFile) {
                        file.delete()
                    }
                }

            temporaryFiles.forEach {
                (fileName, bytes) ->

                val destination =
                    File(
                        directory,
                        fileName
                    )

                FileOutputStream(destination)
                    .use { output ->

                        output.write(bytes)
                    }
            }

            true

        } catch (_: Exception) {

            false
        }
    }

    // ========================================================
    // Admin Panel
    // ========================================================

    private fun adminPage() {
        pageTitle(
            "Admin Panel",
            "Manage assets, Angel One value and PIN"
        )

        sectionTitle("Asset Creation")

        val name = input("Fund / Stock Name")

        val category = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Mutual Fund", "Stock")
            )
        }

        val add = button("Create Asset", navy)

        add.setOnClickListener {
            val assetName = name.text.toString().trim()

            if (assetName.isEmpty()) {
                toast("Enter fund or stock name.")
                return@setOnClickListener
            }

            store.addAsset(
                assetName,
                category.selectedItem.toString()
            )

            name.text.clear()

            renderPage("Admin Panel")
            toast("Asset created.")
        }

        content.addView(name)
        content.addView(
            category,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                bottomMargin = dp(10)
            }
        )
        content.addView(add)

        spacer(15)

        val assets = store.getAssets()

        if (assets.isNotEmpty()) {
            assets.forEach { asset ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = rounded(white, border, 13)
                }

                val total =
                    store.getSips()
                        .filter { it.assetId == asset.id }
                        .sumOf { it.amount }

                val title = TextView(this).apply {
                    text =
                        "${asset.name}\n${asset.category}  •  ${
                            inr.format(total)
                        }"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(this@MainActivity.text)
                }

                val buttons = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val edit = button("Edit", navy)
                val delete = button("Delete", danger)

                edit.setOnClickListener {
                    editAssetDialog(asset)
                }

                delete.setOnClickListener {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Delete Asset")
                        .setMessage(
                            "Deleting this asset will also remove its SIP entries and its investment total."
                        )
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            store.deleteAsset(asset.id)
                            renderPage("Admin Panel")
                            toast("Asset deleted.")
                        }
                        .show()
                }

                buttons.addView(
                    edit,
                    LinearLayout.LayoutParams(
                        0,
                        dp(43),
                        1f
                    ).apply {
                        rightMargin = dp(5)
                    }
                )

                buttons.addView(
                    delete,
                    LinearLayout.LayoutParams(
                        0,
                        dp(43),
                        1f
                    ).apply {
                        leftMargin = dp(5)
                    }
                )

                row.addView(title)
                row.addView(buttons)

                content.addView(
                    row,
                    LinearLayout.LayoutParams(
                        -1,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(9)
                    }
                )
            }
        }

        spacer(20)

        sectionTitle("Angel One Portfolio Value")

        val angelDate = input("Last Updated Date")
        val angelValue = input("Portfolio Value after charges / tax").apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val existingAngel = store.getAngelValue()

        if (existingAngel != null) {
            angelDate.setText(existingAngel.first)
            angelValue.setText(existingAngel.second.toString())
        }

        val saveAngel = button(
            "Save Angel One Value",
            navy
        )

        saveAngel.setOnClickListener {
            val v = angelValue.text.toString().toDoubleOrNull()

            if (v == null || v < 0) {
                toast("Enter a valid portfolio value.")
                return@setOnClickListener
            }

            store.setAngelValue(
                angelDate.text.toString(),
                v
            )

            renderPage("Admin Panel")
            toast("Angel One value updated.")
        }

        content.addView(angelDate)
        content.addView(angelValue)
        content.addView(saveAngel)

        spacer(20)

        sectionTitle("Reset PIN")

        val newPin = input("New 6 Digit PIN").apply {
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(
                android.text.InputFilter.LengthFilter(6)
            )
        }

        val reset = button(
            "Reset PIN",
            deepNavy
        )

        reset.setOnClickListener {
            val pin = newPin.text.toString()

            if (pin.length != 6 || !pin.all { it.isDigit() }) {
                toast("PIN must contain exactly 6 digits.")
                return@setOnClickListener
            }

            store.setPin(pin)
            newPin.text.clear()

            toast("PIN reset successfully.")
        }

        content.addView(newPin)
        content.addView(reset)
    }

    private fun editAssetDialog(asset: Asset) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }

        val name = input("Fund / Stock Name").apply {
            setText(asset.name)
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Mutual Fund", "Stock")
            )

            setSelection(
                if (asset.category == "Stock") 1 else 0
            )
        }

        box.addView(name)
        box.addView(spinner)

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Asset")
            .setView(box)
            .setPositiveButton("Update") { _, _ ->
                if (name.text.toString().trim().isNotEmpty()) {
                    store.updateAsset(
                        asset.id,
                        name.text.toString(),
                        spinner.selectedItem.toString()
                    )

                    renderPage("Admin Panel")
                    toast("Asset updated.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ========================================================
    // UI helpers
    // ========================================================

    private fun pageTitle(title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(14))
        }

        val t = TextView(this).apply {
            text = title
            textSize = 26f
            typeface = Typeface.create("serif", Typeface.BOLD)
            setTextColor(navy)
        }

        val s = TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(secondary)
            setPadding(0, dp(3), 0, 0)
        }

        box.addView(t)
        box.addView(s)

        content.addView(box)
    }

    private fun sectionTitle(title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(navy)
            setPadding(dp(4), dp(3), dp(4), dp(10))
        }

        content.addView(tv)
    }

    private fun input(hintText: String): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(white, border, 12)

            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                bottomMargin = dp(10)
            }
        }

    private fun button(
        label: String,
        backgroundColor: Int
    ): Button =
        Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(white)
            background = rounded(
                backgroundColor,
                backgroundColor,
                12
            )
            stateListAnimator = null
        }

    private fun label(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(navy)
        }

    private fun value(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(this@MainActivity.text)
        }

    private fun small(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(secondary)
            setPadding(0, dp(2), 0, dp(2))
        }

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(white, border, 14)
            elevation = dp(1).toFloat()
        }

    private fun emptyMessage(message: String) {
        val tv = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            setPadding(dp(15), dp(30), dp(15), dp(30))
            background = rounded(white, border, 14)
        }

        content.addView(
            tv,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun spacer(height: Int) {
        content.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        radius: Int
    ): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fill)
            setStroke(dp(1), stroke)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun isOnline(): Boolean {
        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val capabilities =
            cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private fun fromHtml(html: String): Spanned =
        Html.fromHtml(
            html,
            Html.FROM_HTML_MODE_LEGACY
        )

    private fun applyStyle(
        editText: EditText,
        span: Any
    ) {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start < 0 || end <= start) {
            toast("Select some text first.")
            return
        }

        val editable = editText.text

        when (span) {
            is android.text.style.StyleSpan -> {
                editable.setSpan(
                    span,
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            is android.text.style.UnderlineSpan -> {
                editable.setSpan(
                    span,
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun AlertDialogBuilder():
        android.app.AlertDialog.Builder =
        android.app.AlertDialog.Builder(this)
}
