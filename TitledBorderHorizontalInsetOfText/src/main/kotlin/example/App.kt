package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.Component.BaselineResizeBehavior
import java.awt.geom.Path2D
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.plaf.basic.BasicHTML

private const val TITLE = "TitledBorder Test"

fun makeUI() = JPanel(GridLayout(0, 1, 5, 5)).also {
  val b1 = object : TitledBorder(TITLE + "1") {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
      super.paintBorder(c, g, x + 10, y, width, height)
    }
  }
  it.add(makeComp("override TitledBorder#paintBorder(...)", b1))

  val b2 = object : TitledBorder(TITLE + "2") {
    override fun getBorderInsets(c: Component, insets: Insets): Insets {
      val i = super.getBorderInsets(c, insets)
      i.left += 10
      return i
    }
  }
  it.add(makeComp("override TitledBorder#getBorderInsets(...)", b2))

  val label = JLabel(TITLE + "3", null, SwingConstants.LEFT)
  label.border = EmptyBorder(0, 5, 0, 5)
  val b3 = ComponentTitledBorder(label, UIManager.getBorder("TitledBorder.border"))
  it.add(makeComp("ComponentTitledBorder + EmptyBorder", b3))

  val b4 = TitledBorder2(TITLE + "4")
  it.add(makeComp("TitledBorder2: copied from TitledBorder", b4))

  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun makeComp(str: String, bdr: Border) = JLabel().also {
  it.border = bdr
  it.putClientProperty("html.disable", true)
  it.text = str
}

private class ComponentTitledBorder(
  private val comp: Component,
  private val border: Border
) : Border, SwingConstants {
  init {
    (comp as? JComponent)?.isOpaque = true
  }

  override fun isBorderOpaque() = false

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    if (c is Container) {
      val borderInsets = border.getBorderInsets(c)
      val insets = getBorderInsets(c)
      val v = 0.coerceAtLeast((insets.top - borderInsets.top) / 2)
      border.paintBorder(c, g, x, y + v, width, height - v)
      val size = comp.preferredSize
      val rect = Rectangle(OFFSET, 0, size.width, size.height)
      comp.bounds = rect
      SwingUtilities.paintComponent(g, comp, c, rect)
    }
  }

  override fun getBorderInsets(c: Component): Insets {
    val size = comp.preferredSize
    val insets = border.getBorderInsets(c)
    insets.top = insets.top.coerceAtLeast(size.height)
    return insets
  }

  companion object {
    private const val OFFSET = 10
  }
}

private class TitledBorder2 @JvmOverloads constructor(
  private var border: Border? = null,
  private var title: String? = "",
  titleJust: Int = LEADING,
  titlePosition: Int = DEFAULT_POSITION,
  titleFont: Font? = null,
  titleColor: Color? = null
) : AbstractBorder() {
  var titlePosition = 0
    set(titlePosition) {
      field = when (titlePosition) {
        ABOVE_TOP, TOP, BELOW_TOP, ABOVE_BOTTOM, BOTTOM, BELOW_BOTTOM, DEFAULT_POSITION -> titlePosition
        else -> throw IllegalArgumentException("$titlePosition is not a valid title position.")
      }
    }
  var titleJust = 0
    set(titleJust) {
      field = when (titleJust) {
        DEFAULT_JUST, LEFT, CENTER, RIGHT, LEADING, TRAILING -> titleJust
        else -> throw IllegalArgumentException("$titleJust is not a valid title justification.")
      }
    }
  var titleFont = titleFont
    set(titleFont) {
      field = titleFont ?: UIManager.getFont("TitledBorder.font")
    }
  var titleColor = titleColor
    set(titleColor) {
      field = titleColor ?: UIManager.getColor("TitledBorder.titleColor")
    }
  private val position: Int
    get() {
      val position = titlePosition
      if (position != DEFAULT_POSITION) {
        return position
      }
      val value = UIManager.get("TitledBorder.position")
      return if (value is Int && DEFAULT_POSITION < value && value <= BELOW_BOTTOM) {
        value
      } else {
        when ((value as? String)?.uppercase(Locale.ENGLISH)) {
          "ABOVE_TOP" -> ABOVE_TOP
          "TOP" -> TOP
          "BELOW_TOP" -> BELOW_TOP
          "ABOVE_BOTTOM" -> ABOVE_BOTTOM
          "BOTTOM" -> BOTTOM
          "BELOW_BOTTOM" -> BELOW_BOTTOM
          else -> TOP
        }
      }
    }
  private val label = JLabel().also {
    it.isOpaque = false
    it.putClientProperty(BasicHTML.propertyKey, null)
  }

  init {
    this.titleJust = titleJust
    this.titlePosition = titlePosition
  }

  constructor(title: String?) : this(null, title, LEADING, DEFAULT_POSITION, null, null)

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val bdr = getBorder()
    val str = title
    if (str?.isNotEmpty() == true) {
      val edge = if (bdr is TitledBorder2) 0 else EDGE_SPACING
      val br = Rectangle()
      br.x = x + edge
      br.y = y + edge
      br.width = width - edge - edge
      br.height = height - edge - edge
      val lr = Rectangle()
      lr.y = y
      val size = getLabel(c).preferredSize
      lr.height = size.height
      val insets = makeBorderInsets(bdr, c, Insets(0, 0, 0, 0))

      initPositionRect(height, edge, insets, br, lr)
      insets.left += edge + TEXT_INSET_H
      insets.right += edge + TEXT_INSET_H

      val just = getJustification(c, titleJust)
      lr.x = x
      lr.width = width - insets.left - insets.right
      if (lr.width > size.width) {
        lr.width = size.width
      }
      when (just) {
        LEFT -> lr.x += insets.left
        RIGHT -> lr.x += width - insets.right - lr.width
        CENTER -> lr.x += (width - lr.width) / 2
      }

      paintWrapBorder(c, bdr, g, position, br, lr)
      g.translate(lr.x, lr.y)
      label.setSize(lr.width, lr.height)
      label.paint(g)
      g.translate(-lr.x, -lr.y)
    } else {
      border?.paintBorder(c, g, x, y, width, height)
    }
  }

  private fun initPositionRect(height: Int, edge: Int, ins: Insets, br: Rectangle, lr: Rectangle) {
    when (position) {
      ABOVE_TOP -> {
        ins.left = 0
        ins.right = 0
        br.y += lr.height - edge
        br.height -= lr.height - edge
      }
      TOP -> {
        ins.top = edge + ins.top / 2 - lr.height / 2
        if (ins.top < edge) {
          br.y -= ins.top
          br.height += ins.top
        } else {
          lr.y += ins.top
        }
      }
      BELOW_TOP -> lr.y += ins.top + edge
      ABOVE_BOTTOM -> lr.y += height - lr.height - ins.bottom - edge
      BOTTOM -> {
        lr.y += height - lr.height
        ins.bottom = edge + (ins.bottom - lr.height) / 2
        if (ins.bottom < edge) {
          br.height += ins.bottom
        } else {
          lr.y -= ins.bottom
        }
      }
      BELOW_BOTTOM -> {
        ins.left = 0
        ins.right = 0
        lr.y += height - lr.height
        br.height -= lr.height - edge
      }
    }
  }

  private fun paintWrapBorder(c: Component, bdr: Border?, g: Graphics, position: Int, b: Rectangle, l: Rectangle) {
    bdr?.also {
      if (position == TOP || position == BOTTOM) {
        val tsp = TEXT_SPACING
        val p = Path2D.Float()
        p.append(Rectangle(b.x, b.y, b.width, l.y - b.y), false)
        p.append(Rectangle(b.x, l.y, l.x - b.x - TEXT_SPACING, l.height), false)
        p.append(Rectangle(l.x + l.width + tsp, l.y, b.x - l.x + b.width - l.width - tsp, l.height), false)
        p.append(Rectangle(b.x, l.y + l.height, b.width, b.y - l.y + b.height - l.height), false)
        val g2 = g.create() as? Graphics2D ?: return
        g2.clip(p)
        it.paintBorder(c, g2, b.x, b.y, b.width, b.height)
        g2.dispose()
      } else {
        it.paintBorder(c, g, b.x, b.y, b.width, b.height)
      }
    }
  }

  override fun getBorderInsets(c: Component, insets: Insets): Insets {
    var ins = insets
    val b = getBorder()
    ins = makeBorderInsets(b, c, ins)
    val str = title
    if (str?.isNotEmpty() == true) {
      val edge = if (b is TitledBorder2) 0 else EDGE_SPACING
      val size = getLabel(c).preferredSize
      when (position) {
        ABOVE_TOP -> ins.top += size.height - edge
        TOP -> if (ins.top < size.height) {
          ins.top = size.height - edge
        }
        BELOW_TOP -> ins.top += size.height
        ABOVE_BOTTOM -> ins.bottom += size.height
        BOTTOM -> if (ins.bottom < size.height) {
          ins.bottom = size.height - edge
        }
        BELOW_BOTTOM -> ins.bottom += size.height - edge
      }
      ins.top += edge + TEXT_SPACING
      ins.left += edge + TEXT_SPACING
      ins.right += edge + TEXT_SPACING
      ins.bottom += edge + TEXT_SPACING
    }
    return ins
  }

  override fun isBorderOpaque() = true

  fun getBorder(): Border? = border ?: UIManager.getBorder("TitledBorder.border")

  // fun getTitleFont(): Font? = titleFont ?: UIManager.getFont("TitledBorder.font")

  // fun getTitleColor(): Color? = titleColor ?: UIManager.getColor("TitledBorder.titleColor")

//  fun setBorder(border: Border?) {
//    this.border = border
//  }
//
//  fun setTitleFont(titleFont: Font?) {
//    this.titleFont = titleFont
//  }
//
//  fun setTitleColor(titleColor: Color?) {
//    this.titleColor = titleColor
//  }
//
//  fun getMinimumSize(c: Component): Dimension {
//    val insets = getBorderInsets(c)
//    val minSize = Dimension(insets.right + insets.left, insets.top + insets.bottom)
//    val str = title
//    if (str?.isNotEmpty() == true) {
//      val size = getLabel(c).preferredSize
//      val pos = position
//      if (pos != ABOVE_TOP && pos != BELOW_BOTTOM) {
//        minSize.width += size.width
//      } else if (minSize.width < size.width) {
//        minSize.width += size.width
//      }
//    }
//    return minSize
//  }

  override fun getBaseline(c: Component?, width: Int, height: Int): Int {
    require(c != null) { "Must supply non-null component" }
    require(width >= 0) { "Width must be >= 0" }
    require(height >= 0) { "Height must be >= 0" }
    val b = getBorder()
    val str = title
    if (str?.isNotEmpty() == true) {
      val edge = if (b is TitledBorder2) 0 else EDGE_SPACING
      val size = getLabel(c).preferredSize
      val i = makeBorderInsets(b, c, Insets(0, 0, 0, 0))
      val baseline = getLabel(c).getBaseline(size.width, size.height)
      return when (position) {
        ABOVE_TOP -> baseline
        TOP -> {
          i.top = edge + (i.top - size.height) / 2
          if (i.top < edge) baseline else baseline + i.top
        }
        BELOW_TOP -> baseline + i.top + edge
        ABOVE_BOTTOM -> baseline + height - size.height - i.bottom - edge
        BOTTOM -> {
          i.bottom = edge + (i.bottom - size.height) / 2
          if (i.bottom < edge) baseline + height - size.height else baseline + height - size.height + i.bottom
        }
        BELOW_BOTTOM -> baseline + height - size.height
        else -> -1
      }
    }
    return -1
  }

  override fun getBaselineResizeBehavior(c: Component): BaselineResizeBehavior {
    super.getBaselineResizeBehavior(c)
    return when (position) {
      ABOVE_TOP, TOP, BELOW_TOP -> BaselineResizeBehavior.CONSTANT_ASCENT
      ABOVE_BOTTOM, BOTTOM, BELOW_BOTTOM -> BaselineResizeBehavior.CONSTANT_DESCENT
      else -> BaselineResizeBehavior.OTHER
    }
  }

  // fun getFont(c: Component?) = getTitleFont() ?: c?.font ?: Font(Font.DIALOG, Font.PLAIN, 12)

  // private fun getColor(c: Component?) = getTitleColor() ?: c?.foreground

  private fun getLabel(c: Component): JLabel {
    label.text = title
    label.font = titleFont ?: c.font ?: Font(Font.DIALOG, Font.PLAIN, 12)
    label.foreground = titleColor ?: c.foreground
    label.componentOrientation = c.componentOrientation
    label.isEnabled = c.isEnabled
    return label
  }

  companion object {
    const val DEFAULT_POSITION = 0
    const val ABOVE_TOP = 1
    const val TOP = 2
    const val BELOW_TOP = 3
    const val ABOVE_BOTTOM = 4
    const val BOTTOM = 5
    const val BELOW_BOTTOM = 6
    const val DEFAULT_JUST = 0
    const val LEFT = 1
    const val CENTER = 2
    const val RIGHT = 3
    const val LEADING = 4
    const val TRAILING = 5
    const val EDGE_SPACING = 2
    const val TEXT_SPACING = 5 // 2
    const val TEXT_INSET_H = 10 // 5

    private fun getJustification(c: Component, just: Int) = if (just == LEADING || just == DEFAULT_JUST) {
      if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
    } else if (just == TRAILING) {
      if (c.componentOrientation.isLeftToRight) RIGHT else LEFT
    } else just

    private fun makeBorderInsets(bdr: Border?, c: Component, insets: Insets): Insets {
      when (bdr) {
        null -> insets[0, 0, 0] = 0
        is AbstractBorder -> bdr.getBorderInsets(c, insets)
        else -> {
          val i = bdr.getBorderInsets(c)
          insets[i.top, i.left, i.bottom] = i.right
        }
      }
      return insets
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
