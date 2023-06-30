package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

fun makeUI(): Component {
  UIManager.put("ScrollBar.width", 10)
  UIManager.put("ScrollBar.thumbHeight", 6) // GTK, Synth, NimbusLookAndFeel
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(30, 30))
  UIManager.put("ScrollBar.incrementButtonGap", 0)
  UIManager.put("ScrollBar.decrementButtonGap", 0)

  // UIManager.put("ScrollBar.squareButtons", true)
  // UIManager.put("ArrowButton.size", 8)

  val thumbColor = Color(0xCD_CD_CD)
  UIManager.put("ScrollBar.thumb", thumbColor)
  // UIManager.put("ScrollBar.thumbShadow", thumbColor)
  // UIManager.put("ScrollBar.thumbDarkShadow", thumbColor)
  // UIManager.put("ScrollBar.thumbHighlight", thumbColor)

  val trackColor = Color(0xF0_F0_F0)
  UIManager.put("ScrollBar.track", trackColor)

  val txt = "*****************\n".repeat(100)
  val scroll = object : JScrollPane(JTextArea(txt)) {
    override fun updateUI() {
      super.updateUI()
      getVerticalScrollBar().ui = WithoutArrowButtonScrollBarUI()
      getHorizontalScrollBar().ui = WithoutArrowButtonScrollBarUI()
    }
  }

  return JPanel(GridLayout(1, 0)).also {
    it.add(JScrollPane(JTextArea(txt)))
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class WithoutArrowButtonScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = trackColor
    g2.fill(r)
    g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    val sb = (c as? JScrollBar)?.takeIf { it.isEnabled } ?: return
    val m = sb.model
    if (m.maximum - m.minimum - m.extent > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val color = when {
        isDragging -> thumbDarkShadowColor
        isThumbRollover -> thumbLightShadowColor
        else -> thumbColor
      }
      g2.paint = color
      g2.fillRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2)
      g2.dispose()
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
