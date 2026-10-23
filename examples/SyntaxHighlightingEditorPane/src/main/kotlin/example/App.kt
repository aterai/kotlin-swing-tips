package example

import java.awt.*
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

fun createUI(): Component {
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = createStyleSheet()

  val editor = JEditorPane()
  editor.editorKit = htmlEditorKit
  editor.isEditable = false

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.selectedFile.toPath(), editor)
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun loadFile(path: Path, editor: JEditorPane) {
  try {
    // Files.readAllLines(...) throws a MalformedInputException (an IOException)
    // for a non UTF-8 file, while Files.lines(...) throws an UncheckedIOException
    // lazily from the stream, which is not caught by catch (IOException ex).
    val txt = Files.readAllLines(path, StandardCharsets.UTF_8).joinToString("\n")
    editor.text = "<pre>${JavaSyntaxHighlighter.highlight(txt)}\n</pre>"
  } catch (ex: IOException) {
    editor.text = ex.message
  }
}

private fun createStyleSheet() = StyleSheet().also {
  it.addRule(".str{color:#008800}")
  it.addRule(".kwd{color:#000088}")
  it.addRule(".com{color:#880000}")
  it.addRule(".typ{color:#660066}")
  it.addRule(".lit{color:#006666}")
  it.addRule(".pun{color:#666600}")
  it.addRule(".pln{color:#000000}")
  it.addRule(".tag{color:#000088}")
  it.addRule(".atn{color:#660066}")
  it.addRule(".atv{color:#008800}")
  it.addRule(".dec{color:#660066}")
}

enum class TokenType(
  val cssClass: String,
) {
  KEYWORD("kwd"),
  COMMENT_LINE("com"),
  COMMENT_BLOCK("com"),
  STRING("str"),
  CHAR("str"),
  NUMBER("lit"),
  ANNOTATION("lit"),
  TYPE("typ"),
  PUNCTUATION("pun"),
  IDENTIFIER("pln"),
  PLAIN("pln"),
  WHITESPACE("pln"),
}

data class Token(
  val type: TokenType,
  val value: String,
  val startIndex: Int,
  val endIndex: Int,
)

object JavaSyntaxHighlighter {
  private val KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "false", "final", "finally", "float", "for", "goto", "if",
    "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "null", "package", "private", "protected", "public", "return", "short",
    "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
    "throws", "transient", "true", "try", "void", "volatile", "while",
  )

  private data class TokenRule(
    val regex: String,
    val type: TokenType,
  )

  // Checked in this order (highest priority first).
  // All groups inside each regex must be non-capturing, because the index of
  // the capturing group in the combined pattern identifies the matched rule.
  private val TOKEN_RULES = listOf(
    TokenRule("""/\*[\s\S]*?\*/""", TokenType.COMMENT_BLOCK),
    TokenRule("""//[^\r\n]*""", TokenType.COMMENT_LINE),
    TokenRule(""""(?:\\.|[^"\\\r\n])*"""", TokenType.STRING),
    TokenRule("""'(?:\\.|[^'\\\r\n])+'""", TokenType.CHAR),
    // The floating point alternatives must precede the integer one,
    // otherwise "3.14" is split into "3", "." and "14".
    TokenRule(
      """0[xX][0-9a-fA-F_]+[lL]?""" +
        """|\d[\d_]*\.[\d_]*(?:[eE][+-]?\d+)?[fFdD]?""" +
        """|\.\d[\d_]*(?:[eE][+-]?\d+)?[fFdD]?""" +
        """|\d[\d_]*(?:[eE][+-]?\d+)?[fFdDlL]?""",
      TokenType.NUMBER,
    ),
    TokenRule("""@[a-zA-Z_$][a-zA-Z0-9_$]*""", TokenType.ANNOTATION),
    // Keywords and type names are classified later by classifyIdentifier()
    TokenRule("""[a-zA-Z_$][a-zA-Z0-9_$]*""", TokenType.IDENTIFIER),
    TokenRule("""[{};,()\[\].<>]""", TokenType.PUNCTUATION),
    TokenRule("""\s+""", TokenType.WHITESPACE),
    // Single character fallback, so that the combined pattern always matches
    TokenRule("""[\s\S]""", TokenType.PLAIN),
  )

  private val TOKEN_PATTERN = Pattern.compile(
    TOKEN_RULES.joinToString("|") { "(${it.regex})" },
  )

  fun highlight(sourceCode: String) = generateHtml(tokenize(sourceCode))

  private fun tokenize(source: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val matcher = TOKEN_PATTERN.matcher(source)
    // The fallback rule matches any character, so each find() starts
    // exactly at the end of the previous token.
    while (matcher.find()) {
      val value = matcher.group()
      var type = getMatchedType(matcher)
      if (type == TokenType.IDENTIFIER) {
        type = classifyIdentifier(value)
      }
      tokens.add(Token(type, value, matcher.start(), matcher.end()))
    }
    return tokens
  }

  private fun getMatchedType(matcher: Matcher): TokenType {
    var index = 0
    while (matcher.start(index + 1) < 0) {
      index++
    }
    return TOKEN_RULES[index].type
  }

  private fun classifyIdentifier(value: String) = when {
    value in KEYWORDS -> TokenType.KEYWORD
    value[0].isUpperCase() -> TokenType.TYPE
    else -> TokenType.PLAIN
  }

  private fun generateHtml(tokens: List<Token>): String {
    val html = StringBuilder(500)
    for (token in tokens) {
      when (val type = token.type) {
        TokenType.WHITESPACE -> {
          // Whitespace contains no characters that need to be escaped
          html.append(normalizeWhitespace(token.value))
        }

        TokenType.PLAIN -> {
          html.append(escapeHtml(token.value))
        }

        else -> {
          html
            .append("<span class=\"")
            .append(type.cssClass)
            .append("\">")
            .append(escapeHtml(token.value))
            .append("</span>")
        }
      }
    }
    return html.toString()
  }

  private fun escapeHtml(text: String) = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

  // Replace spaces first so that a tab becomes 4 plain spaces, keep \n and \r as is
  private fun normalizeWhitespace(whitespace: String) = whitespace
    .replace(" ", "&#160;")
    .replace("\t", "    ")
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
