package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val informationIcon = UIManager.getIcon("OptionPane.informationIcon")
  val errorIcon = UIManager.getIcon("OptionPane.errorIcon")
  val questionIcon = UIManager.getIcon("OptionPane.questionIcon")
  val warningIcon = UIManager.getIcon("OptionPane.warningIcon")
  val p = JPanel(GridLayout(0, 1))
  listOf(informationIcon, errorIcon, questionIcon, warningIcon).forEach {
    p.add(makeBox(it))
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeBox(icon: Icon): Box {
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeLabel("0", icon))
  box.add(Box.createHorizontalStrut(16))
  box.add(makeLabel("180", QuadrantRotateIcon(icon, QuadrantRotate.HORIZONTAL_FLIP)))
  box.add(Box.createHorizontalStrut(16))
  box.add(makeLabel("90", QuadrantRotateIcon(icon, QuadrantRotate.CLOCKWISE)))
  box.add(Box.createHorizontalStrut(16))
  box.add(makeLabel("-90", QuadrantRotateIcon(icon, QuadrantRotate.COUNTER_CLOCKWISE)))
  box.add(Box.createHorizontalGlue())
  return box
}

private fun makeLabel(title: String, icon: Icon): JLabel {
  val l = JLabel(title, icon, SwingConstants.CENTER)
  l.verticalTextPosition = SwingConstants.BOTTOM
  l.horizontalTextPosition = SwingConstants.CENTER
  return l
}

private enum class QuadrantRotate(val numQuadrants: Int) {
  CLOCKWISE(1), HORIZONTAL_FLIP(2), COUNTER_CLOCKWISE(-1)
}

private class QuadrantRotateIcon(
  private val icon: Icon,
  private val rotate: QuadrantRotate
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val w = icon.iconWidth
    val h = icon.iconHeight
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    when (rotate) {
      QuadrantRotate.CLOCKWISE -> g2.translate(h, 0)
      QuadrantRotate.HORIZONTAL_FLIP -> g2.translate(w, h)
      QuadrantRotate.COUNTER_CLOCKWISE -> g2.translate(0, w)
    }
    g2.transform(AffineTransform.getQuadrantRotateInstance(rotate.numQuadrants))
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
  }

  override fun getIconWidth() =
    if (rotate == QuadrantRotate.HORIZONTAL_FLIP) icon.iconWidth else icon.iconHeight

  override fun getIconHeight() =
    if (rotate == QuadrantRotate.HORIZONTAL_FLIP) icon.iconHeight else icon.iconWidth
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
