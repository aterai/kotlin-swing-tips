package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tabs1 = JTabbedPane()
  tabs1.addTab("Java", makeEditor(makeJava1(), 36))
  tabs1.addTab("Kotlin", makeEditor(makeKotlin1(), 36))
  val tabs2 = JTabbedPane()
  tabs2.addTab("Java", makeEditor(makeJava2(), 160))
  tabs2.addTab("Kotlin", makeEditor(makeKotlin2(), 160))
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

private fun makeEditor(s: String, height: Int): Component {
  val editor = JEditorPane()
  editor.setContentType("text/html")
  editor.isEditable = false
  editor.text = s
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

private fun makeJava1() = listOf(
  "<html><pre><code>",
  "btn.setSelected((i &amp; (<span style='color: rgb(170, 17, 17);'>1</span> &lt;&lt;",
  " <span style='color: rgb(170, 17, 17);'>2</span>)) !=",
  " <span style='color: rgb(170, 17, 17);'>0</span>);",
).joinToString("")

private fun makeKotlin1() = listOf(
  "<html><pre><code>",
  "btn.setSelected(i and (<span style='color: rgb(170, 17, 17);'>1</span>",
  " shl <span style='color: rgb(170, 17, 17);'>2</span>)",
  " != <span style='color: rgb(170, 17, 17);'>0</span>)",
).joinToString("").trimIndent()

private fun makeJava2() = """
<html><pre><code>BufferedImage bi = Optional.ofNullable(path)
  .map(url -&gt; {
    <span style='color: rgb(170, 17, 17);'>try</span> {
      <span style='color: rgb(170, 17, 17);'>return</span> ImageIO.read(url);
    } <span style='color: rgb(170, 17, 17);'>catch</span> (IOException ex) {
      <span style='color: rgb(170, 17, 17);'>return</span> makeMissingImage();
    }
  }).orElseGet(() -&gt; makeMissingImage());
""".trimIndent()

private fun makeKotlin2(): String {
  val span = "<span style='color: rgb(17, 119, 0);'>read</span>"
  return """
    <pre><code>val bi = runCatching {
      ImageIO.${span.format("read")}(${span.format("path")})
    }.getOrNull() ?: makeMissingImage()
    """.trimIndent()
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
