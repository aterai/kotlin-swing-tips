package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox().also {
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Default", makeSlider(false)))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Triangle Tick", makeSlider(true)))
    it.add(Box.createVerticalGlue())
  }
  return JPanel(BorderLayout(5, 5)).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeSlider(icon: Boolean): JSlider {
  val slider = JSlider(0, 100)
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.paintLabels = true
  slider.snapToTicks = true
  slider.putClientProperty("Slider.paintThumbArrowShape", true)
  val labelTable = slider.labelTable
  if (icon && labelTable is Map<*, *>) {
    val tick = TickIcon()
    labelTable.forEach { (_, value) ->
      if (value is JLabel) {
        value.border = BorderFactory.createEmptyBorder(1, 0, 0, 0)
        value.icon = tick
        value.iconTextGap = 0
        value.verticalAlignment = SwingConstants.TOP
        value.verticalTextPosition = SwingConstants.BOTTOM
        value.horizontalAlignment = SwingConstants.CENTER
        value.horizontalTextPosition = SwingConstants.CENTER
        value.foreground = Color.RED
      }
    }
  } else {
    slider.paintTicks = true
    slider.foreground = Color.BLUE
  }
  return slider
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class TickIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.color = Color.BLUE
    g2.drawLine(2, 0, 2, 2)
    g2.drawLine(1, 1, 3, 1)
    g2.drawLine(0, 2, 4, 2)
    g2.dispose()
  }

  override fun getIconWidth() = 5

  override fun getIconHeight() = 3
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
