package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.Component.BaselineResizeBehavior
import java.awt.geom.Path2D
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

private fun makeComp(str: String, border: Border) = JLabel().also {
  it.border = border
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
  border: Border? = null,
  title: String? = "",
  titleJustification: Int = LEADING,
  titlePosition: Int = DEFAULT_POSITION,
  titleFont: Font? = null,
  titleColor: Color? = null
) : AbstractBorder() {
  var titlePosition = 0
    set(titlePosition) {
      field =
        when (titlePosition) {
          ABOVE_TOP, TOP, BELOW_TOP, ABOVE_BOTTOM, BOTTOM, BELOW_BOTTOM, DEFAULT_POSITION -> titlePosition
          else -> throw IllegalArgumentException("$titlePosition is not a valid title position.")
        }
    }
  var titleJustification = 0
    set(titleJustification) {
      field =
        when (titleJustification) {
          DEFAULT_JUSTIFICATION, LEFT, CENTER, RIGHT, LEADING, TRAILING -> titleJustification
          else -> throw IllegalArgumentException("$titleJustification is not a valid title justification.")
        }
    }
  private val position: Int
    get() {
      val position = titlePosition
      if (position != DEFAULT_POSITION) {
        return position
      }
      val value = UIManager.get("TitledBorder.position")
      if (value is Int) {
        if (0 < value && value <= 6) {
          return value
        }
      } else if (value is String) {
        if ("ABOVE_TOP".equals(value, ignoreCase = true)) {
          return ABOVE_TOP
        }
        if ("TOP".equals(value, ignoreCase = true)) {
          return TOP
        }
        if ("BELOW_TOP".equals(value, ignoreCase = true)) {
          return BELOW_TOP
        }
        if ("ABOVE_BOTTOM".equals(value, ignoreCase = true)) {
          return ABOVE_BOTTOM
        }
        if ("BOTTOM".equals(value, ignoreCase = true)) {
          return BOTTOM
        }
        if ("BELOW_BOTTOM".equals(value, ignoreCase = true)) {
          return BELOW_BOTTOM
        }
      }
      return TOP
    }
  private var border: Border?
  private var title: String?
  private var titleFont: Font?
  private var titleColor: Color?
  private val label = JLabel().also {
    it.isOpaque = false
    it.putClientProperty(BasicHTML.propertyKey, null)
  }

  init {
    this.border = border
    this.title = title
    this.titleFont = titleFont
    this.titleColor = titleColor
    this.titleJustification = titleJustification
    this.titlePosition = titlePosition
  }

  constructor(title: String?) : this(null, title, LEADING, DEFAULT_POSITION, null, null)

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val b = getBorder()
    val str = title
    if (str?.isNotEmpty() == true) {
      val edge = if (b is TitledBorder2) 0 else EDGE_SPACING
      val size = getLabel(c).preferredSize
      val insets = makeBorderInsets(b, c, Insets(0, 0, 0, 0))
      val bdrX = x + edge
      var bdrY = y + edge
      val bdrW = width - edge - edge
      var bdrH = height - edge - edge
      var lblY = y
      val lblH = size.height
      val position = position
      when (position) {
        ABOVE_TOP -> {
          insets.left = 0
          insets.right = 0
          bdrY += lblH - edge
          bdrH -= lblH - edge
        }
        TOP -> {
          insets.top = edge + insets.top / 2 - lblH / 2
          if (insets.top < edge) {
            bdrY -= insets.top
            bdrH += insets.top
          } else {
            lblY += insets.top
          }
        }
        BELOW_TOP -> lblY += insets.top + edge
        ABOVE_BOTTOM -> lblY += height - lblH - insets.bottom - edge
        BOTTOM -> {
          lblY += height - lblH
          insets.bottom = edge + (insets.bottom - lblH) / 2
          if (insets.bottom < edge) {
            bdrH += insets.bottom
          } else {
            lblY -= insets.bottom
          }
        }
        BELOW_BOTTOM -> {
          insets.left = 0
          insets.right = 0
          lblY += height - lblH
          bdrH -= lblH - edge
        }
      }
      insets.left += edge + TEXT_INSET_H
      insets.right += edge + TEXT_INSET_H
      var lblX = x
      var lblW = width - insets.left - insets.right
      if (lblW > size.width) {
        lblW = size.width
      }
      when (getJustification(c)) {
        LEFT -> lblX += insets.left
        RIGHT -> lblX += width - insets.right - lblW
        CENTER -> lblX += (width - lblW) / 2
      }
      if (position != TOP && position != BOTTOM) {
        b?.paintBorder(c, g, bdrX, bdrY, bdrW, bdrH)
      } else {
        val sp = TEXT_SPACING
        val p = Path2D.Float()
        p.append(Rectangle(bdrX, bdrY, bdrW, lblY - bdrY), false)
        p.append(Rectangle(bdrX, lblY, lblX - bdrX - TEXT_SPACING, lblH), false)
        p.append(Rectangle(lblX + lblW + sp, lblY, bdrX - lblX + bdrW - lblW - sp, lblH), false)
        p.append(Rectangle(bdrX, lblY + lblH, bdrW, bdrY - lblY + bdrH - lblH), false)
        val g2 = g.create() as? Graphics2D
        g2?.clip(p)
        b?.paintBorder(c, g2, bdrX, bdrY, bdrW, bdrH)
        g2?.dispose()
      }
      g.translate(lblX, lblY)
      label.setSize(lblW, lblH)
      label.paint(g)
      g.translate(-lblX, -lblY)
    } else {
      b?.paintBorder(c, g, x, y, width, height)
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

  fun getTitleFont(): Font? = titleFont ?: UIManager.getFont("TitledBorder.font")

  fun getTitleColor(): Color? = titleColor ?: UIManager.getColor("TitledBorder.titleColor")

  fun setBorder(border: Border?) {
    this.border = border
  }

  fun setTitleFont(titleFont: Font?) {
    this.titleFont = titleFont
  }

  fun setTitleColor(titleColor: Color?) {
    this.titleColor = titleColor
  }

  fun getMinimumSize(c: Component): Dimension {
    val insets = getBorderInsets(c)
    val minSize = Dimension(insets.right + insets.left, insets.top + insets.bottom)
    val str = title
    if (str?.isNotEmpty() == true) {
      val size = getLabel(c).preferredSize
      val pos = position
      if (pos != ABOVE_TOP && pos != BELOW_BOTTOM) {
        minSize.width += size.width
      } else if (minSize.width < size.width) {
        minSize.width += size.width
      }
    }
    return minSize
  }

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

  private fun getJustification(c: Component): Int {
    val justification = titleJustification
    if (justification == LEADING || justification == DEFAULT_JUSTIFICATION) {
      return if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
    }
    return if (justification == TRAILING) {
      if (c.componentOrientation.isLeftToRight) RIGHT else LEFT
    } else justification
  }

  fun getFont(c: Component?) = getTitleFont() ?: c?.font ?: Font(Font.DIALOG, Font.PLAIN, 12)

  private fun getColor(c: Component?) = getTitleColor() ?: c?.foreground

  private fun getLabel(c: Component): JLabel {
    label.text = title
    label.font = getFont(c)
    label.foreground = getColor(c)
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
    const val DEFAULT_JUSTIFICATION = 0
    const val LEFT = 1
    const val CENTER = 2
    const val RIGHT = 3
    const val LEADING = 4
    const val TRAILING = 5
    const val EDGE_SPACING = 2
    const val TEXT_SPACING = 5 // 2
    const val TEXT_INSET_H = 10 // 5

    private fun makeBorderInsets(border: Border?, c: Component, insets: Insets): Insets {
      when (border) {
        null -> insets[0, 0, 0] = 0
        is AbstractBorder -> border.getBorderInsets(c, insets)
        else -> {
          val i = border.getBorderInsets(c)
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
