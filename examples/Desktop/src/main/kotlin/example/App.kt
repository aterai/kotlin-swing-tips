package example

import java.awt.*
import java.net.URI
import javax.swing.*
import javax.swing.event.HyperlinkEvent

private const val SITE = "https://ateraimemo.com/"

fun makeUI(): Component {
  val textArea = JTextArea()
  val editor = JEditorPane("text/html", "<html><a href='$SITE'>$SITE</a>")
  editor.isOpaque = false
  editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor.isEditable = false
  editor.addHyperlinkListener { e ->
    val desktopSupported = Desktop.isDesktopSupported()
    if (desktopSupported && e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      runCatching {
        Desktop.getDesktop().browse(URI(SITE))
      }.onFailure {
        it.printStackTrace()
        textArea.text = it.message
      }
      textArea.text = e.toString()
    }
  }

  val p = JPanel()
  p.add(editor)
  p.border = BorderFactory.createTitledBorder("Desktop.getDesktop().browse(URI)")

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
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
