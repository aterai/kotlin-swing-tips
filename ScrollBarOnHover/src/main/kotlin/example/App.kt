package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val text = "1234567890\n".repeat(100)
  val textArea = JTextArea("Mouse cursor flickers over the JScrollBar.\n$text")
  val ml = object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      val sp = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, e.source as? Component)
      (sp as? JScrollPane)?.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    }

    override fun mouseExited(e: MouseEvent) {
      val sp = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, e.source as? Component)
      (sp as? JScrollPane)?.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    }
  }
  textArea.addMouseListener(ml)
  val scroll1 = makeScrollPane(textArea)
  val scroll2 = makeScrollPane(JTextArea(text))
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("MouseListener", scroll1))
    it.add(makeTitledPanel("JLayer", JLayer(scroll2, ScrollBarOnHoverLayerUI())))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(c: JComponent): JScrollPane {
  val scroll = JScrollPane(c)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  return scroll
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class ScrollBarOnHoverLayerUI : LayerUI<JScrollPane>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val id = e.id
    if (id == MouseEvent.MOUSE_ENTERED) {
      l.view.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    } else if (id == MouseEvent.MOUSE_EXITED) {
      l.view.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
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
