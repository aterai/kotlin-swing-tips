package example

import java.awt.*
import javax.swing.*
import kotlin.math.PI

fun makeUI(): Component {
  val box1 = Box.createHorizontalBox()
  box1.add(JLabel("JLabel"))
  box1.add(WavyLineSeparator(SwingConstants.VERTICAL))
  box1.add(Box.createHorizontalStrut(5))
  box1.add(JTextField("**********"))
  box1.add(Box.createHorizontalGlue())
  box1.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)

  val box2 = Box.createVerticalBox()
  box2.add(JCheckBox("JCheckBox"))
  box2.add(WavyLineSeparator())
  box2.add(JLabel("JLabel: 12345678901234567890"))
  box2.add(Box.createVerticalGlue())
  box2.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.NORTH)
    it.add(box2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class WavyLineSeparator(
  orientation: Int = HORIZONTAL,
) : JSeparator(orientation) {
  override fun updateUI() {
    super.updateUI()
    BorderFactory.createEmptyBorder(2, 2, 2, 2)
  }

  override fun paintComponent(g: Graphics) {
    var pos: Int
    val i = insets
    if (orientation == HORIZONTAL) {
      pos = i.left
      while (width - pos > 0) {
        HORIZONTAL_ICON.paintIcon(this, g, pos, i.top)
        pos += HORIZONTAL_ICON.iconWidth
      }
    } else {
      pos = i.top
      while (height - pos > 0) {
        VERTICAL_ICON.paintIcon(this, g, i.left, pos)
        pos += VERTICAL_ICON.iconHeight
      }
    }
  }

  override fun getPreferredSize(): Dimension {
    val i = insets
    return if (orientation == HORIZONTAL) {
      Dimension(30, ICON_SZ + i.top + i.bottom)
    } else {
      Dimension(ICON_SZ + i.left + i.right, 30)
    }
  }

  private class WavyLineIcon : Icon {
    private val sfc = UIManager.getColor("Separator.foreground")
    private val orientation: Int

    constructor() {
      orientation = HORIZONTAL
    }

    constructor(orientation: Int) {
      this.orientation = orientation
    }

    override fun paintIcon(
      c: Component,
      g: Graphics,
      x: Int,
      y: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = sfc
      if (orientation == VERTICAL) {
        g2.translate(x + iconWidth, y)
        g2.rotate(PI / 2)
      } else {
        g2.translate(x, y)
      }
      g2.drawLine(0, 2, 0, 2)
      g2.drawLine(1, 1, 1, 1)
      g2.drawLine(2, 0, 3, 0)
      g2.drawLine(4, 1, 4, 1)
      g2.drawLine(5, 2, 5, 2)
      g2.dispose()
    }

    override fun getIconWidth() = if (orientation == HORIZONTAL) ICON_SZ * 2 else ICON_SZ

    override fun getIconHeight() = if (orientation == HORIZONTAL) ICON_SZ else ICON_SZ * 2
  }

  companion object {
    private const val ICON_SZ = 3
    private val HORIZONTAL_ICON = WavyLineIcon()
    private val VERTICAL_ICON = WavyLineIcon(VERTICAL)
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
