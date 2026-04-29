package example

import java.awt.*
import java.awt.event.HierarchyEvent
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit

private const val HTML_TEXT = """
<html>
<body>
<a href='https://ateraimemo.com/Swing.html' title='JST'>Java Swing Tips</a>
</body>
</html>
"""

fun createUI(): Component {
  val hintEditor = JEditorPane()
  hintEditor.editorKit = HTMLEditorKit()
  hintEditor.isEditable = false
  hintEditor.isOpaque = false

  val check = JCheckBox()
  check.isOpaque = false

  val tooltipContentPanel = JPanel(BorderLayout())
  tooltipContentPanel.add(hintEditor)
  tooltipContentPanel.add(check, BorderLayout.EAST)

  val popup = JPopupMenu()
  popup.add(JScrollPane(tooltipContentPanel))
  popup.setBorder(BorderFactory.createEmptyBorder())

  val editor = RichToolTipEditorPane(popup, tooltipContentPanel)
  editor.editorKit = HTMLEditorKit()
  editor.text = HTML_TEXT
  editor.isEditable = false
  editor.addHyperlinkListener { e ->
    handleHyperlinkEvent(e, editor, hintEditor)
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(editor))
  p.add(JScrollPane(JTextArea(HTML_TEXT)))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun handleHyperlinkEvent(
  e: HyperlinkEvent,
  editor: RichToolTipEditorPane,
  hintEditor: JEditorPane,
) {
  when (e.eventType) {
    HyperlinkEvent.EventType.ACTIVATED -> {
      JOptionPane.showMessageDialog(
        editor,
        "You click the link with the URL " + e.url,
      )
    }

    HyperlinkEvent.EventType.ENTERED -> {
      e.sourceElement
        ?.let { it.attributes.getAttribute(HTML.Tag.A) as? AttributeSet }
        ?.also {
          editor.toolTipText = ""
          val title = it.getAttribute(HTML.Attribute.TITLE).toString()
          val url = e.url.toString()
          hintEditor.text = "<html>$title: <a href='$url'>$url</a>"
          SwingUtilities.getWindowAncestor(hintEditor)?.pack()
        }
    }

    HyperlinkEvent.EventType.EXITED -> {
      editor.toolTipText = null
    }
  }
}

private class RichToolTipEditorPane(
  private val popup: JPopupMenu,
  private val tooltipContentPanel: JPanel,
) : JEditorPane() {
  override fun createToolTip(): JToolTip {
    val tip = super.createToolTip()
    tip.addHierarchyListener { e ->
      val b = e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
      if (b && e.component.isShowing) {
        tooltipContentPanel.background = tip.background
        popup.show(tip, 0, 0)
      }
    }
    return tip
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
