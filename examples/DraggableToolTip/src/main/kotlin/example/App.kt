package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit

private const val HTML_TEXT = """
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
  val tipPanel = JPanel(BorderLayout())
  tipPanel.add(hint)
  tipPanel.add(DragLabel(), BorderLayout.WEST)

  val popup = JPopupMenu()
  popup.isLightWeightPopupEnabled = false
  popup.add(JScrollPane(tipPanel))
  popup.setBorder(BorderFactory.createEmptyBorder())

  val editor = ToolTipEditorPane(tipPanel)
  editor.editorKit = HTMLEditorKit()
  editor.text = HTML_TEXT
  editor.isEditable = false
  editor.addHyperlinkListener { linkEvent(it, hint) }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun linkEvent(e: HyperlinkEvent, hint: JEditorPane) {
  val editor = e.getSource() as? JEditorPane ?: return
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
          hint.text = "<html>$title: <a href='$url'>$url</a><br/>...<br/>..."
          SwingUtilities.getWindowAncestor(hint)?.pack()
        }
    }

    HyperlinkEvent.EventType.EXITED -> {
      editor.toolTipText = null
    }
  }
}

private class ToolTipEditorPane(
  private val panel: JPanel,
) : JEditorPane() {
  override fun createToolTip(): JToolTip {
    val tip = super.createToolTip()
    tip.addHierarchyListener { e ->
      val showing = e.component.isShowing()
      val b = e.getChangeFlags() and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
      if (b && showing) {
        panel.setBackground(tip.getBackground())
        val p = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, panel)
        if (p is JPopupMenu) {
          p.show(tip, 0, 0)
        }
      }
    }
    return tip
  }
}

private class DragLabel : JLabel() {
  private var handler: MouseAdapter? = null

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    handler = PopupDragListener()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    val bgc = UIManager.getColor("ToolTip.background")
    if (bgc != null) {
      setBackground(bgc.darker())
    }
    setOpaque(true)
  }

  override fun getPreferredSize() = Dimension(16, 64)
}

private class PopupDragListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      val c = e.component
      val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, c)
      SwingUtilities.convertMouseEvent(c, e, popup)
      startPt.location = e.getPoint()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      val c = e.component
      val w = SwingUtilities.getWindowAncestor(c)
      // Popup$HeavyWeightWindow
      if (w != null && w.type == Window.Type.POPUP) {
        val pt = e.locationOnScreen
        w.setLocation(pt.x - startPt.x, pt.y - startPt.y)
      }
    }
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
