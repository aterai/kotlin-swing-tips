package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun createUI(): Component {
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
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val scroll = c.getView() as? JScrollPane ?: return
      val r = scroll.getViewportBorderBounds()
      val m = scroll.getVerticalScrollBar().getModel()

      // 1. Dynamically get background color of JList
      val bgc = getBgColor(scroll)
      val transparent = Color(bgc.rgb and 0x00_FF_FF_FF, true)

      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      // g2.clip = r

      // 2. Top edge fade (background color -> transparent)
      if (m.minimum < m.value) {
        g2.paint = GradientPaint(
          0f,
          r.y.toFloat(),
          bgc,
          0f,
          (r.y + OVERFLOW).toFloat(),
          transparent,
        )
        g2.fillRect(r.x, r.y, r.width, OVERFLOW)
      }

      // 3. Bottom edge fade (transparent -> background color)
      if (m.value + m.extent < m.maximum) {
        val fadeTop = r.y + r.height - OVERFLOW
        g2.paint = GradientPaint(
          0f,
          fadeTop.toFloat(),
          transparent,
          0f,
          (r.y + r.height).toFloat(),
          bgc,
        )
        g2.fillRect(r.x, fadeTop, r.width, OVERFLOW)
      }
      g2.dispose()
    }
  }

  private fun getBgColor(scroll: JScrollPane) = scroll
    .getViewport()
    .view
    ?.getBackground()
    ?: UIManager.getColor("List.background")
    ?: Color.WHITE

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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
