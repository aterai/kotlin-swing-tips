package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val bg0 = ButtonGroup()
  val box0 = Box.createVerticalBox()
  box0.border = BorderFactory.createTitledBorder("Default")
  for (i in 0..<4) {
    val b = JRadioButton("Default: $i")
    bg0.add(b)
    box0.add(b)
    box0.add(Box.createVerticalStrut(5))
  }

  val bg1 = ButtonGroup()
  val box1 = Box.createVerticalBox()
  box1.border = BorderFactory.createTitledBorder("Text Color")
  for (i in 4..<8) {
    val b = ColorRadioButton("Text: $i")
    bg1.add(b)
    box1.add(b)
    box1.add(Box.createVerticalStrut(5))
  }

  val bg2 = ButtonGroup()
  val box2 = Box.createVerticalBox()
  box2.border = BorderFactory.createTitledBorder("Icon Color")
  for (i in 8..<12) {
    val b = object : ColorRadioButton("Icon: $i") {
      override fun updateUI() {
        super.updateUI()
        icon = DefaultIcon()
        pressedIcon = PressedIcon()
        selectedIcon = SelectedIcon()
        rolloverIcon = RolloverIcon()
      }
    }
    bg2.add(b)
    box2.add(b)
    box2.add(Box.createVerticalStrut(5))
  }

  return JPanel(GridLayout(1, 3)).also {
    it.add(box0)
    it.add(box1)
    it.add(box2)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class ColorRadioButton(text: String?) : JRadioButton(text) {
  override fun updateUI() {
    super.updateUI()
    foreground = DefaultIcon.DEFAULT_COLOR
    // alignmentX = Component.LEFT_ALIGNMENT
  }

  override fun fireStateChanged() {
    val model = getModel()
    foreground = if (model.isEnabled) {
      when {
        model.isPressed && model.isArmed -> DefaultIcon.PRESSED_COLOR
        model.isSelected -> DefaultIcon.SELECTED_COLOR
        isRolloverEnabled && model.isRollover -> DefaultIcon.ROLLOVER_COLOR
        else -> DefaultIcon.DEFAULT_COLOR
      }
    } else {
      Color.GRAY
    }
    super.fireStateChanged()
  }
}

private open class DefaultIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = DEFAULT_COLOR
    g2.drawRect(0, 0, iconWidth - 1, iconHeight - 1)
    g2.drawRect(1, 1, iconWidth - 2 - 1, iconHeight - 2 - 1)
    g2.dispose()
  }

  override fun getIconWidth() = ICON_SIZE * 2

  override fun getIconHeight() = ICON_SIZE

  companion object {
    val DEFAULT_COLOR: Color = Color.BLACK
    val PRESSED_COLOR: Color = Color.GREEN
    val SELECTED_COLOR: Color = Color.RED
    val ROLLOVER_COLOR: Color = Color.BLUE
    protected const val ICON_SIZE = 16
  }
}

private class PressedIcon : DefaultIcon() {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = PRESSED_COLOR
    g2.drawRect(0, 0, iconWidth - 1, iconHeight - 1)
    g2.drawRect(1, 1, iconWidth - 2 - 1, iconHeight - 2 - 1)
    g2.paint = SELECTED_COLOR
    g2.fillRect(4, 4, iconWidth - 8, iconHeight - 8)
    g2.dispose()
  }
}

private class SelectedIcon : DefaultIcon() {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = SELECTED_COLOR
    g2.drawRect(0, 0, iconWidth - 1, iconHeight - 1)
    g2.drawRect(1, 1, iconWidth - 2 - 1, iconHeight - 2 - 1)
    g2.paint = PRESSED_COLOR
    g2.fillRect(6, 6, iconWidth - 12, iconHeight - 12)
    g2.dispose()
  }
}

private class RolloverIcon : DefaultIcon() {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = ROLLOVER_COLOR
    g2.drawRect(0, 0, iconWidth - 1, iconHeight - 1)
    g2.drawRect(1, 1, iconWidth - 2 - 1, iconHeight - 2 - 1)
    g2.dispose()
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
