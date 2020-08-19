package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit

private const val HTML_TEXT =
  """
  <html>
    <body>
      <a href='https://ateraimemo.com/Swing.html' title='Title: JST'>Java Swing Tips</a>
    </body>
  </html>
  """

fun makeUI(): Component {
  val hint = JEditorPane()
  hint.editorKit = HTMLEditorKit()
  hint.isEditable = false
  hint.isOpaque = false

  val check = JCheckBox()
  check.isOpaque = false

  val panel = JPanel(BorderLayout())
  panel.add(hint)
  panel.add(check, BorderLayout.EAST)

  val popup = JPopupMenu()
  popup.add(JScrollPane(panel))
  popup.border = BorderFactory.createEmptyBorder()

  val editor = object : JEditorPane() {
    override fun createToolTip(): JToolTip {
      val tip = super.createToolTip()
      tip.addHierarchyListener { e ->
        if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && e.component.isShowing) {
          panel.background = tip.background
          popup.show(tip, 0, 0)
        }
      }
      return tip
    }
  }
  editor.editorKit = HTMLEditorKit()
  editor.text = HTML_TEXT
  editor.isEditable = false
  editor.addHyperlinkListener { e ->
    when (e.eventType) {
      HyperlinkEvent.EventType.ACTIVATED -> JOptionPane.showMessageDialog(
        editor,
        "You click the link with the URL " + e.url
      )
      HyperlinkEvent.EventType.ENTERED -> {
        editor.toolTipText = ""
        e.sourceElement
          ?.let { it.attributes.getAttribute(HTML.Tag.A) as? AttributeSet }
          ?.also {
            val title = it.getAttribute(HTML.Attribute.TITLE).toString()
            val url = e.url.toString()
            hint.text = "<html>$title: <a href='$url'>$url</a>"
            popup.pack()
          }
      }
      HyperlinkEvent.EventType.EXITED -> editor.toolTipText = null
    }
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(editor))
  p.add(JScrollPane(JTextArea("dummy")))
  p.preferredSize = Dimension(320, 240)
  return p
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
