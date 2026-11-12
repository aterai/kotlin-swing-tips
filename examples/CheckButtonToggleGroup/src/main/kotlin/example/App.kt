package example

import java.awt.*
import java.awt.geom.Path2D
import javax.swing.*

private val SELECTED_ICON = ScaledIcon(CheckBoxIcon(), 12, 12)
private val EMPTY_ICON = EmptyIcon()

fun createUI(): Component {
  val styles = listOf("Bold", "Italic", "Underline")
  val foods = listOf("hot dogs", "pizza", "ravioli", "bananas")
  return JPanel().also {
    it.add(createSingleSelection(styles, 1))
    it.add(createMultipleSelection(foods, 1))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createPanel(overlap: Int): JPanel {
  // A negative hgap overlaps adjacent buttons so that their LineBorders are shared
  val p = object : JPanel(FlowLayout(FlowLayout.CENTER, -overlap, 0)) {
    override fun isOptimizedDrawingEnabled() = false

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 1000
      return d
    }
  }
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  return p
}

private fun createSingleSelection(list: List<String>, overlap: Int): JPanel {
  val p = createPanel(overlap)
  val group = ToggleButtonGroup()
  list.forEach {
    val b = createCheckToggleButton(it)
    p.add(b)
    group.add(b)
  }
  return p
}

private fun createMultipleSelection(list: List<String>, overlap: Int): JPanel {
  val p = createPanel(overlap)
  list.forEach { p.add(createCheckToggleButton(it)) }
  return p
}

private fun createCheckToggleButton(title: String): AbstractButton {
  val button = object : JToggleButton(title) {
    override fun updateUI() {
      super.updateUI()
      background = Color.GRAY
      border = createBorder(background)
      isContentAreaFilled = false
      isFocusPainted = false
      isOpaque = false
    }
  }
  // An ItemEvent is also fired on a button deselected by the ButtonGroup,
  // so each button only needs to update itself
  button.addItemListener { updateButton(button) }
  return button
}

private fun updateButton(button: AbstractButton) {
  if (button.model.isSelected) {
    button.icon = SELECTED_ICON
    button.foreground = Color.WHITE
    button.isOpaque = true
  } else {
    button.icon = EMPTY_ICON
    button.foreground = Color.BLACK
    button.isOpaque = false
  }
}

private fun createBorder(color: Color) = BorderFactory.createCompoundBorder(
  BorderFactory.createLineBorder(color),
  BorderFactory.createEmptyBorder(5, 8, 5, 8),
)

private class ScaledIcon(
  private val icon: Icon,
  private val width: Int,
  private val height: Int,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    val sx = width / icon.iconWidth.toDouble()
    val sy = height / icon.iconHeight.toDouble()
    g2.scale(sx, sy)
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class CheckBoxIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create()
    if (g2 is Graphics2D && c is AbstractButton) {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.translate(x, y)
      if (c.model.isSelected) {
        g2.paint = c.foreground
        val s = iconWidth.coerceAtMost(iconHeight) * .05
        val w = iconWidth - s - s
        val h = iconHeight - s - s
        val gw = w / 8.0
        val gh = h / 8.0
        g2.stroke = BasicStroke(3f * s.toFloat())
        // The origin is already translated to (x, y), so do not add x and y again
        val p = Path2D.Double()
        p.moveTo(2.0 * gw, .5 * h)
        p.lineTo(.4 * w, h - 2.0 * gh)
        p.lineTo(w - 2.0 * gw, 2.0 * gh)
        g2.draw(p)
      }
      g2.dispose()
    }
  }

  override fun getIconWidth() = 1000

  override fun getIconHeight() = 1000
}

private class ToggleButtonGroup : ButtonGroup() {
  private var prevModel: ButtonModel? = null

  override fun setSelected(m: ButtonModel, b: Boolean) {
    if (m == prevModel) {
      clearSelection()
    } else {
      super.setSelected(m, b)
    }
    prevModel = selection
  }
}

private class EmptyIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Do nothing
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
