package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI

class MainPanel : JPanel(GridLayout(1, 0)) {
  init {
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

    add(JScrollPane(JTextArea(txt)))
    add(object : JScrollPane(JTextArea(txt)) {
      override fun updateUI() {
        super.updateUI()
        getVerticalScrollBar().setUI(ArrowButtonlessScrollBarUI())
        getHorizontalScrollBar().setUI(ArrowButtonlessScrollBarUI())
      }
    })
    setPreferredSize(Dimension(320, 240))
  }
}

class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

class ArrowButtonlessScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(trackColor)
    g2.fill(r)
    g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    val sb = (c as? JScrollBar)?.takeIf { it.isEnabled() } ?: return
    val m = sb.getModel()
    if (m.getMaximum() - m.getMinimum() - m.getExtent() > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val color = when {
        isDragging -> thumbDarkShadowColor
        isThumbRollover() -> thumbLightShadowColor
        else -> thumbColor
      }
      g2.setPaint(color)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
