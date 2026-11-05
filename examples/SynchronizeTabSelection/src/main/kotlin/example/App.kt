package example

import java.awt.*
import javax.swing.*

private const val RED = "rgb(170, 17, 17)"
private const val GREEN = "rgb(17, 119, 0)"

fun createUI(): Component {
  val tabs1 = JTabbedPane()
  tabs1.addTab("Java", createEditor(createJava1(), 36))
  tabs1.addTab("Kotlin", createEditor(createKotlin1(), 36))
  val tabs2 = JTabbedPane()
  tabs2.addTab("Java", createEditor(createJava2(), 160))
  tabs2.addTab("Kotlin", createEditor(createKotlin2(), 160))
  // Both tabbed panes must have the same number of tabs: otherwise, selecting
  // a tab that exists only in one of them throws an IndexOutOfBoundsException.
  tabs2.setModel(tabs1.getModel())
  val box = Box.createVerticalBox()
  box.add(tabs1)
  box.add(Box.createVerticalStrut(5))
  box.add(tabs2)
  box.add(Box.createVerticalGlue())
  val scroll = object : JScrollPane(box) {
    override fun updateUI() {
      super.updateUI()
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(scroll, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createEditor(html: String, height: Int): Component {
  val editor = JEditorPane()
  editor.setContentType("text/html")
  editor.isEditable = false
  editor.text = html
  return object : JScrollPane(editor) {
    override fun updateUI() {
      super.updateUI()
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = height
      return d
    }
  }
}

private fun span(color: String, text: String) = "<span style='color: $color;'>$text</span>"

private fun createJava1() = listOf(
  "<html><pre><code>",
  "btn.setSelected((i &amp; (${span(RED, "1")}",
  " &lt;&lt; ${span(RED, "2")}))",
  " != ${span(RED, "0")});",
).joinToString("")

private fun createKotlin1() = listOf(
  "<html><pre><code>",
  "btn.setSelected(i and (${span(RED, "1")}",
  " shl ${span(RED, "2")})",
  " != ${span(RED, "0")})",
).joinToString("").trimIndent()

private fun createJava2() = """
<html><pre><code>BufferedImage bi = Optional.ofNullable(path)
  .map(url -&gt; {
    ${span(RED, "try")} {
      ${span(RED, "return")} ImageIO.read(url);
    } ${span(RED, "catch")} (IOException ex) {
      ${span(RED, "return")} createMissingImage();
    }
  }).orElseGet(() -&gt; createMissingImage());
  """.trimIndent()

private fun createKotlin2() = """
<html><pre><code>val bi = runCatching {
  ImageIO.${span(GREEN, "read")}(${span(GREEN, "path")})
}.getOrNull() ?: createMissingImage()
  """.trimIndent()

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
