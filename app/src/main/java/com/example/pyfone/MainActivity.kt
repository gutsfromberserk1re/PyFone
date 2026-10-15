package com.example.pyfone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PyFoneApp() }
    }
}

val ColorBgDark = Color(0xFF1E2C42)
val ColorContainer = Color(0xDD233857)
val ColorTitleBar = Color(0xFFF0F3FA)
val ColorTextPrimary = Color(0xFFD5DEEF)
val ColorTextSecondary = Color(0xFFB1C9EF)
val ColorKeyword = Color(0xFF628ECB)
val ColorString = Color(0xFF9CD3A6)
val ColorComment = Color(0xFF8AAEE0)
val ColorRunBtn = Color(0xFF395886)
val ColorError = Color(0xFFE57373)

val pythonKeywords = listOf(
    "def", "if", "elif", "else", "return", "import", "from", "print", "class",
    "while", "for", "in", "try", "except", "finally", "with", "True", "False",
    "None", "and", "or", "not", "pass", "break", "continue"
)

private val keywordRegex = Regex("\\b(${pythonKeywords.joinToString("|")})\\b")
private val stringRegex = Regex("\"[^\"\\n]*\"|'[^'\\n]*'")
private val commentRegex = Regex("#.*")
private val blockStartRegex = Regex("^(def|class|if|elif|else|while|for|try|except|finally|with)\\b")

/** Whole-word keyword, string and comment highlighting. Later spans override earlier ones. */
fun highlightCode(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    keywordRegex.findAll(text).forEach {
        addStyle(SpanStyle(color = ColorKeyword), it.range.first, it.range.last + 1)
    }
    stringRegex.findAll(text).forEach {
        addStyle(SpanStyle(color = ColorString), it.range.first, it.range.last + 1)
    }
    commentRegex.findAll(text).forEach {
        addStyle(SpanStyle(color = ColorComment), it.range.first, it.range.last + 1)
    }
}

/** Returns an error message, or null if nothing obviously wrong. */
fun checkSyntax(text: String): String? {
    val stack = ArrayDeque<Char>()
    val pairs = mapOf(')' to '(', ']' to '[', '}' to '{')
    text.lines().forEachIndexed { index, raw ->
        val line = raw.replace(stringRegex, "\"\"").replace(commentRegex, "")
        for (c in line) {
            when (c) {
                '(', '[', '{' -> stack.addLast(c)
                ')', ']', '}' -> {
                    if (stack.isEmpty() || stack.removeLast() != pairs[c]) {
                        return "✗ Line ${index + 1}: unmatched '$c'"
                    }
                }
            }
        }
        val t = line.trim()
        if (stack.isEmpty() && t.isNotEmpty() && blockStartRegex.containsMatchIn(t) && !t.endsWith(":")) {
            return "✗ Line ${index + 1}: missing ':'"
        }
    }
    if (stack.isNotEmpty()) return "✗ Unclosed '${stack.last()}'"
    if (text.contains("≠")) return "✗ Invalid operator '≠' (use !=)"
    return null
}

private val highlightTransformation = VisualTransformation { text ->
    TransformedText(highlightCode(text.text), OffsetMapping.Identity)
}

@Composable
fun PyFoneApp() {
    var textFieldState by remember {
        mutableStateOf(
            TextFieldValue("print(\"hello pyfone\")\ndef pyfone_run():\n    if status == ready:")
        )
    }
    var outputText by remember {
        mutableStateOf("> Running PyFone session...\nhello pyfone\n\n> Finished (exit code 0)")
    }

    val error = remember(textFieldState.text) { checkSyntax(textFieldState.text) }
    val statusMessage = error ?: "✓ No syntax errors"
    val hasError = error != null

    val currentWord = remember(textFieldState.text, textFieldState.selection) {
        val cursor = textFieldState.selection.start.coerceIn(0, textFieldState.text.length)
        textFieldState.text.take(cursor).takeLastWhile { it.isLetterOrDigit() || it == '_' }
    }

    val suggestions = remember(currentWord) {
        if (currentWord.length >= 2) {
            pythonKeywords.filter { it.startsWith(currentWord, ignoreCase = true) && it != currentWord }
        } else emptyList()
    }

    val backgroundGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF283A54), ColorBgDark, Color(0xFF141F30))
    )
    val codeLineHeight = 18.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .imePadding()
            .padding(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            color = ColorContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(ColorTitleBar)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "pyfone_main.py *",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFF192537))
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ColorRunBtn)
                                .clickable {
                                    outputText = if (hasError) {
                                        "> Running PyFone script...\n$statusMessage\n\n> Finished (exit code 1)"
                                    } else {
                                        "> Running PyFone script...\nExecution successful.\n\n> Finished (exit code 0)"
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "▶ Run",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ColorTitleBar)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF8AAEE0)))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF628ECB)))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF395886)))
                        }
                    }
                }

                // Editor
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                    ) {
                        val lineCount = textFieldState.text.lines().size
                        Column(
                            modifier = Modifier.padding(start = 10.dp, end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..maxOf(lineCount, 6)) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = codeLineHeight,
                                        color = ColorTextSecondary
                                    )
                                )
                            }
                        }

                        BasicTextField(
                            value = textFieldState,
                            onValueChange = { textFieldState = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = codeLineHeight,
                                color = ColorTextPrimary
                            ),
                            cursorBrush = SolidColor(ColorTextPrimary),
                            visualTransformation = highlightTransformation
                        )
                    }

                    if (suggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 50.dp, top = 40.dp)
                                .width(140.dp)
                                .heightIn(max = 110.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFF1E2C42),
                            tonalElevation = 6.dp
                        ) {
                            LazyColumn {
                                items(suggestions) { kw ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val cursor = textFieldState.selection.start
                                                val text = textFieldState.text
                                                val prefix = text.take(cursor - currentWord.length)
                                                val suffix = text.drop(cursor)
                                                textFieldState = TextFieldValue(
                                                    text = prefix + kw + suffix,
                                                    selection = TextRange(prefix.length + kw.length)
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = kw,
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ColorKeyword)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Output
                if (outputText.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF2E456B), thickness = 1.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = outputText,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ColorTextSecondary)
                        )
                    }
                }

                // Status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (hasError) ColorError else ColorKeyword
                        )
                    )
                }
            }
        }
    }
}
