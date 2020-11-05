package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.cos
import kotlin.math.sin

fun makeUI(): Component {
  val label = JLabel("Test Test", StarburstIcon(), SwingConstants.CENTER)
  label.isOpaque = true
  label.background = Color.WHITE

  val verticalAlignment = JComboBox(Vertical.values())
  verticalAlignment.selectedItem = Vertical.CENTER

  val verticalTextPosition = JComboBox(Vertical.values())
  verticalTextPosition.selectedItem = Vertical.CENTER

  val horizontalAlignment = JComboBox(Horizontal.values())
  horizontalAlignment.selectedItem = Horizontal.CENTER

  val horizontalTextPosition = JComboBox(Horizontal.values())
  horizontalTextPosition.selectedItem = Horizontal.TRAILING

  val listener = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.verticalAlignment = getSelectedItem(verticalAlignment)!!.alignment
      label.verticalTextPosition = getSelectedItem(verticalTextPosition)!!.alignment
      label.horizontalAlignment = getSelectedItem(horizontalAlignment)!!.alignment
      label.horizontalTextPosition = getSelectedItem(horizontalTextPosition)!!.alignment
      label.repaint()
    }
  }

  listOf(verticalAlignment, verticalTextPosition, horizontalAlignment, horizontalTextPosition)
    .forEach { it.addItemListener(listener) }

  val p1 = JPanel(BorderLayout())
  p1.border = BorderFactory.createTitledBorder("JLabel Test")
  p1.add(label)

  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_END

  val p2 = JPanel(GridBagLayout())
  p2.add(JLabel("setVerticalAlignment:"), c)
  p2.add(JLabel("setVerticalTextPosition:"), c)
  p2.add(JLabel("setHorizontalAlignment:"), c)
  p2.add(JLabel("setHorizontalTextPosition:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p2.add(verticalAlignment, c)
  p2.add(verticalTextPosition, c)
  p2.add(horizontalAlignment, c)
  p2.add(horizontalTextPosition, c)

  return JPanel(BorderLayout()).also {
    it.add(p1)
    it.add(p2, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> getSelectedItem(combo: JComboBox<E>) = combo.getItemAt(combo.selectedIndex)

private enum class Vertical(val alignment: Int) {
  TOP(SwingConstants.TOP), CENTER(SwingConstants.CENTER), BOTTOM(SwingConstants.BOTTOM);
}

private enum class Horizontal(val alignment: Int) {
  LEFT(SwingConstants.LEFT),
  CENTER(SwingConstants.CENTER),
  RIGHT(SwingConstants.RIGHT),
  LEADING(SwingConstants.LEADING),
  TRAILING(SwingConstants.TRAILING);
}

private class StarburstIcon : Icon {
  private val star: Shape

  init {
    var agl = 0.0
    val add = Math.PI / VC
    val p = Path2D.Double()
    p.moveTo(R2.toDouble(), 0.0)
    for (i in 0 until VC * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) R1 else R2
      p.lineTo(r * cos(agl), r * sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-Math.PI / 2.0, R2.toDouble(), 0.0)
    star = Path2D.Double(p, at)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.YELLOW
    g2.fill(star)
    g2.paint = Color.BLACK
    g2.draw(star)
    g2.dispose()
  }

  override fun getIconWidth() = 2 * R2

  override fun getIconHeight() = 2 * R2

  companion object {
    private const val R2 = 24
    private const val R1 = 20
    private const val VC = 18
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
