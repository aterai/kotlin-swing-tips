package example

import java.awt.*
import java.io.BufferedInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

fun makeUI(): Component {
  val styleSheet = StyleSheet()
  // styleSheet.addRule("body {font-size: 24pt; font-family: IPAexGothic;}")
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet
  val editor1 = JEditorPane()
  editor1.editorKit = htmlEditorKit

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/SurrogatePair.html")
  if (url == null || !read(editor1, url)) {
    editor1.text = """
      <html><p>
      (&#xD85B;&#xDE40;) (&#x26E40;)<br />
      (&#xD842;&#xDF9F;) (&#x00020B9F;)
      </p></html>
    """.trimMargin()
  }

  val editor2 = JEditorPane()
  // editor2.setFont(new Font("IPAexGothic", Font.PLAIN, 24))
  editor2.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor2.text = "(\uD85B\uDE40) (\u26E40)\n(\uD842\uDF9F) (\u20B9F)"
  // editor2.setText("(𦹀) (𦹀)\n(𠮟) (𠮟)")

  val p = JPanel(GridLayout(0, 1))
  p.add(makeTitledPanel("Numeric character reference", editor1))
  p.add(makeTitledPanel("Unicode escapes", editor2))

  val button = JButton("browse: SurrogatePair.html")
  button.addActionListener { browseCacheFile(url) }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun read(editor: JEditorPane, url: URL) = runCatching {
  Files.newBufferedReader(Paths.get(url.toURI())).use { editor.read(it, "html") }
}.isSuccess

private fun browseCacheFile(url: URL?) {
  if (url != null && Desktop.isDesktopSupported()) {
    runCatching {
      BufferedInputStream(url.openStream()).use {
        val path = Files.createTempFile("_tmp", ".html")
        path.toFile().deleteOnExit()
        Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
        Desktop.getDesktop().browse(path.toUri())
      }
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val scroll = JScrollPane(c)
  scroll.border = BorderFactory.createTitledBorder(title)
  return scroll
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
