package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val p1 = JPanel(GridBagLayout())
  p1.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val c = GridBagConstraints()
  c.anchor = GridBagConstraints.CENTER
  c.gridheight = 1
  c.gridwidth = 1
  c.gridx = 0
  c.gridy = 0
  c.weightx = 1.0
  c.weighty = 1.0
  c.fill = GridBagConstraints.BOTH
  p1.add(JLabel("left", SwingConstants.CENTER), c)

  c.gridx = 1
  c.weightx = 0.0
  c.fill = GridBagConstraints.VERTICAL
  p1.add(JSeparator(SwingConstants.VERTICAL), c)

  c.gridx = 2
  c.weightx = 1.0
  c.fill = GridBagConstraints.BOTH
  p1.add(JLabel("right", SwingConstants.CENTER), c)

  val p2 = JPanel(GridLayout(0, 2, 5, 5))
  val b2 = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p2.border = BorderFactory.createCompoundBorder(b2, ColumnRulesBorder())

  val p3 = JPanel(GridLayout(0, 2, 5, 5))
  p3.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  listOf(p2, p3).forEach {
    it.add(JLabel("left", SwingConstants.CENTER))
    it.add(JLabel("right", SwingConstants.CENTER))
  }

  val tabs = JTabbedPane().also {
    it.addTab("GridBagLayout", p1)
    it.addTab("GridLayout + Border", p2)
    it.addTab("GridLayout + JLayer", JLayer(p3, ColumnRulesLayerUI()))
  }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColumnRulesBorder : Border {
  private val insets = Insets(0, 0, 0, 0)
  private val separator = JSeparator(SwingConstants.VERTICAL)
  private val renderer = JPanel()

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    if (c is JComponent) {
      val r = SwingUtilities.calculateInnerArea(c, null)
      val sw = separator.preferredSize.width
      val sh = r.height
      val sx = (r.centerX - sw / 2.0).toInt()
      val sy = r.minY.toInt()
      val g2 = g.create() as? Graphics2D ?: return
      SwingUtilities.paintComponent(g2, separator, renderer, sx, sy, sw, sh)
      g2.dispose()
    }
  }

  override fun getBorderInsets(c: Component) = insets

  override fun isBorderOpaque() = true
}

private class ColumnRulesLayerUI : LayerUI<JComponent>() {
  private val separator = JSeparator(SwingConstants.VERTICAL)
  private val renderer = JPanel()

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val tc = (c as? JLayer<*>)?.view
    if (tc is JComponent) {
      val r = SwingUtilities.calculateInnerArea(tc, null)
      val sw = separator.preferredSize.width
      val sh = r.height
      val sx = (r.centerX - sw / 2.0).toInt()
      val sy = r.minY.toInt()
      val g2 = g.create() as? Graphics2D ?: return
      SwingUtilities.paintComponent(g2, separator, renderer, sx, sy, sw, sh)
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
