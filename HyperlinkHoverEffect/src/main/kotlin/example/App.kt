package example

import java.awt.*
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.text.Element
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML

fun makeUI(): Component {
  val format = "<a href='%s' color='%s'>%s</a><br><br>"
  val site = "https://ateraimemo.com/"
  val s1 = format.format(site, "blue", site)
  val s2 = format.format("http://example.com/", "#0000FF", "example")
  val editor = JEditorPane("text/html", "<html>$s1$s2")
  editor.isEditable = false
  editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor.addHyperlinkListener { e ->
    when (e.eventType) {
      HyperlinkEvent.EventType.ENTERED -> setElementColor(e.sourceElement, "red")
      HyperlinkEvent.EventType.EXITED -> setElementColor(e.sourceElement, "blue")
      HyperlinkEvent.EventType.ACTIVATED -> runCatching {
        Desktop.getDesktop().browse(e.url.toURI())
      }
    }
    (e.source as? Component)?.also {
      it.foreground = Color.WHITE
      it.foreground = Color.BLACK
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setElementColor(
  element: Element,
  color: String,
) {
  val a = element.attributes
  (a.getAttribute(HTML.Tag.A) as? MutableAttributeSet)?.addAttribute(HTML.Attribute.COLOR, color)
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
