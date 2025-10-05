package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  val model = DefaultListModel<String>()
  fonts.map { it.fontName }.sorted().forEach { model.addElement(it) }
  return JPanel(GridLayout(1, 0)).also {
    it.add(makeScrollPane(JList(model)))
    it.add(JLayer(makeScrollPane(JList(model)), FadeScrollLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(c: Component) = object : JScrollPane(c) {
  override fun updateUI() {
    super.updateUI()
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
  }
}

private class FadeScrollLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = Color(0x12_FF_FF_FF, true)
    if (c is JLayer<*>) {
      val scroll = c.getView() as? JScrollPane ?: return
      val r = scroll.getViewportBorderBounds()
      val m = scroll.getVerticalScrollBar().getModel()
      g2.clip = r
      if (m.minimum < m.value) {
        for (i in OVERFLOW downTo 1) {
          g2.fillRect(0, r.y - i, r.width, OVERFLOW - i)
        }
      }
      if (m.value + m.extent < m.maximum) {
        g2.translate(r.x, r.y + r.height - OVERFLOW)
        for (i in 0..<OVERFLOW) {
          g2.fillRect(0, i, r.width, OVERFLOW - i)
        }
      }
    }
    g2.dispose()
  }

  companion object {
    const val OVERFLOW = 32
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
