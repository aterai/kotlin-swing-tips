package example

import java.awt.*
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1))
  val path = "example/Burnstown Dam.ttf"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val font = url?.openStream()?.use {
    Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(12f)
  } ?: p.font
  GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)

  val label = JLabel()
  label.font = font.deriveFont(Font.PLAIN, 24f)
  label.text = "1: setFont(font.deriveFont(Font.PLAIN, 24))"
  p.add(label)

  val label2 = JLabel()
  label2.text = "<html><font size='+3' face='Burnstown Dam'>2: html, font, size,+3"
  p.add(label2)

  val styleSheet = StyleSheet()
  styleSheet.addRule("body {font-size: 24pt; font-family: Burnstown Dam;}")
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet

  val editor = JEditorPane()
  editor.editorKit = htmlEditorKit
  editor.text = """
    <html>
      <body>
        <div>3: StyleSheet, body{font-size:24pt; font-family:Burnstown Dam;}</div>
      </body>
    </html>
  """.trimIndent()

  val editor2 = JEditorPane()
  editor2.font = font
  editor2.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor2.text = "4: putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)"

  val p2 = JPanel(GridLayout(0, 1)).also {
    it.add(JScrollPane(editor))
    it.add(JScrollPane(editor2))
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(p2)
    it.preferredSize = Dimension(320, 240)
  }
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
