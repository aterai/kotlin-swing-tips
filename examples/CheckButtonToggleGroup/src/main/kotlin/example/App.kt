package example

import java.awt.*
import java.awt.geom.Path2D
import javax.swing.*

private val SELECTED_ICON = ScaledIcon(CheckBoxIcon(), 12, 12)
private val EMPTY_ICON = EmptyIcon()

fun makeUI(): Component {
  val style = listOf("Bold", "Italic", "Underline")
  val food = listOf("hot dogs", "pizza", "ravioli", "bananas")
  return JPanel().also {
    it.add(makeButtonToggles(style))
    it.add(makeMultipleSelection(food))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(): JPanel {
  return object : JPanel(FlowLayout(FlowLayout.CENTER, -1, 0)) {
    override fun isOptimizedDrawingEnabled() = false

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 1000
      return d
    }
  }
}

private fun makeButtonToggles(list: List<String>): JPanel {
  val overlap = 1
  val p = makePanel()
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  val bg = ToggleButtonGroup()
  list.forEach {
    val b = makeCheckToggleButton(it)
    p.add(b)
    bg.add(b)
  }
  return p
}

private fun makeMultipleSelection(list: List<String>): JPanel {
  val overlap = 1
  val p = makePanel()
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  list.forEach {
    p.add(makeCheckToggleButton(it))
  }
  return p
}

private fun makeCheckToggleButton(title: String): AbstractButton {
  val button = object : JToggleButton(title) {
    override fun updateUI() {
      super.updateUI()
      background = Color.GRAY
      border = makeBorder(background)
      isContentAreaFilled = false
      isFocusPainted = false
      isOpaque = false
    }
  }
  button.addActionListener { e ->
    (e.source as? AbstractButton)
      ?.parent
      ?.also {
        descendants(it)
          .filterIsInstance<AbstractButton>()
          .forEach { b -> updateButton(b) }
      }
  }
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

private fun makeBorder(bgc: Color) = BorderFactory.createCompoundBorder(
  BorderFactory.createLineBorder(bgc),
  BorderFactory.createEmptyBorder(5, 8, 5, 8),
)

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

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
      val model = c.model
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.translate(x, y)
      g2.paint = c.foreground
      val s = iconWidth.coerceAtMost(iconHeight) * .05
      val w = iconWidth - s - s
      val h = iconHeight - s - s
      val gw = w / 8.0
      val gh = h / 8.0
      g2.stroke = BasicStroke(s.toFloat())
      if (model.isSelected) {
        g2.stroke = BasicStroke(3f * s.toFloat())
        val p = Path2D.Double()
        p.moveTo(x + 2f * gw, y + .5f * h)
        p.lineTo(x + .4f * w, y + h - 2f * gh)
        p.lineTo(x + w - 2f * gw, y + 2f * gh)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
