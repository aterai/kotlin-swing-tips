package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val toolBar = JToolBar("ToolBarButton")
  val leftToRight = toolBar.componentOrientation.isLeftToRight
  val check = JCheckBox("", leftToRight)
  check.toolTipText = "isLeftToRight"
  check.addActionListener {
    toolBar.componentOrientation = if ((it.source as? JCheckBox)?.isSelected == true) {
      ComponentOrientation.LEFT_TO_RIGHT
    } else {
      ComponentOrientation.RIGHT_TO_LEFT
    }
    toolBar.revalidate()
  }
  makeList().forEach {
    toolBar.add(createToolBarButton(it))
  }
  toolBar.add(check)
  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBarButton(item: ColorItem): Component {
  val button = object : JButton(item.icon) {
    private var tip: BalloonToolTip? = null
    private val label = JLabel(" ", CENTER)

    override fun getToolTipLocation(e: MouseEvent) = toolTipText?.let {
      val tips = createToolTip()
      tips.tipText = toolTipText
      label.text = toolTipText
      val bar = SwingUtilities.getAncestorOfClass(JToolBar::class.java, this)
      val constraint = calculateConstraint(bar.parent, bar)
      if (tips is BalloonToolTip) {
        tips.updateBalloonShape(constraint)
      }
      getToolTipPoint(preferredSize, tips.preferredSize, constraint)
    }

    override fun createToolTip(): JToolTip {
      val t = tip ?: BalloonToolTip().also {
        LookAndFeel.installColorsAndFont(
          label,
          "ToolTip.background",
          "ToolTip.foreground",
          "ToolTip.font",
        )
        it.add(label)
        val bar = SwingUtilities.getAncestorOfClass(JToolBar::class.java, this)
        val constraint = calculateConstraint(bar.parent, bar)
        it.updateBalloonShape(constraint)
        it.component = this
      }
      tip = t
      return t
    }

    override fun updateUI() {
      tip = null
      super.updateUI()
    }
  }
  button.isOpaque = false
  button.toolTipText = item.title
  button.isFocusPainted = false
  return button
}

private fun calculateConstraint(source: Container, toolBar: Component): String {
  var constraint: String? = null
  if (toolBar is JToolBar && (toolBar.ui as? BasicToolBarUI)?.isFloating == true) {
    if (toolBar.orientation == SwingConstants.VERTICAL) {
      val leftToRight = toolBar.componentOrientation.isLeftToRight
      constraint = if (leftToRight) BorderLayout.WEST else BorderLayout.EAST
    }
  } else {
    val lm = source.layout
    if (lm is BorderLayout) {
      constraint = lm.getConstraints(toolBar) as? String
    }
  }
  return constraint ?: BorderLayout.NORTH
}

private fun getToolTipPoint(btnSz: Dimension, tipSz: Dimension, constraint: String): Point {
  val dx: Double
  val dy: Double
  when (constraint) {
    BorderLayout.WEST -> {
      dx = btnSz.getWidth()
      dy = (btnSz.getHeight() - tipSz.getHeight()) / 2.0
    }

    BorderLayout.EAST -> {
      dx = -tipSz.getWidth()
      dy = (btnSz.getHeight() - tipSz.getHeight()) / 2.0
    }

    BorderLayout.SOUTH -> {
      dx = (btnSz.getWidth() - tipSz.getWidth()) / 2.0
      dy = -tipSz.getHeight()
    }

    else -> {
      dx = (btnSz.getWidth() - tipSz.getWidth()) / 2.0
      dy = btnSz.getHeight()
    }
  }
  return Point((dx + .5).toInt(), (dy + .5).toInt())
}

private fun makeList() = listOf(
  ColorItem("red", ColorIcon(Color.RED)),
  ColorItem("green", ColorIcon(Color.GREEN)),
  ColorItem("blue", ColorIcon(Color.BLUE)),
  ColorItem("cyan", ColorIcon(Color.CYAN)),
  ColorItem("magenta", ColorIcon(Color.MAGENTA)),
  ColorItem("orange", ColorIcon(Color.ORANGE)),
  ColorItem("pink", ColorIcon(Color.PINK)),
  ColorItem("yellow", ColorIcon(Color.YELLOW)),
)

private class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null
  private var shape: Shape? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    layout = BorderLayout()
    listener = HierarchyListener { e ->
      val c = e.component
      if (e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing) {
        SwingUtilities
          .getWindowAncestor(c)
          ?.takeIf { it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    isOpaque = false
    border = BorderFactory.createEmptyBorder(SIZE, SIZE, SIZE, SIZE)
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width += SIZE
    d.height += SIZE
    return d
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = getBackground()
    g2.fill(shape)
    g2.paint = getForeground()
    g2.draw(shape)
    g2.dispose()
    // super.paintComponent(g)
  }

  fun updateBalloonShape(placement: String) {
    val i = insets
    val d = preferredSize
    val tail = Path2D.Double()
    val w = d.getWidth() - i.left - i.right - 1.0
    val h = d.getHeight() - i.top - i.bottom - 1.0
    val cx = w / 2.0
    val cy = h / 2.0
    when (placement) {
      BorderLayout.WEST -> {
        tail.moveTo(0.0, cy - SIZE)
        tail.lineTo(-SIZE.toDouble(), cy)
        tail.lineTo(0.0, cy + SIZE)
      }

      BorderLayout.EAST -> {
        tail.moveTo(w, cy - SIZE)
        tail.lineTo(w + SIZE, cy)
        tail.lineTo(w, cy + SIZE)
      }

      BorderLayout.SOUTH -> {
        tail.moveTo(cx - SIZE, h)
        tail.lineTo(cx, h + SIZE)
        tail.lineTo(cx + SIZE, h)
      }

      else -> {
        tail.moveTo(cx - SIZE, 0.0)
        tail.lineTo(cx, -SIZE.toDouble())
        tail.lineTo(cx + SIZE, 0.0)
      }
    }
    val area = Area(RoundRectangle2D.Double(0.0, 0.0, w, h, ARC, ARC))
    area.add(Area(tail))
    val at = AffineTransform.getTranslateInstance(i.left.toDouble(), i.top.toDouble())
    shape = at.createTransformedShape(area)
  }

  companion object {
    private const val SIZE = 4
    private const val ARC = 4.0
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private data class ColorItem(val title: String, val icon: Icon)

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
