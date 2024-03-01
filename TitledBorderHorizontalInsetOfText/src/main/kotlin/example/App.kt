package example

import java.awt.*
import java.awt.geom.Path2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.ref.WeakReference
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.plaf.basic.BasicHTML

private const val TITLE = "TitledBorder Test"

fun makeUI() = JPanel(GridLayout(0, 1, 5, 5)).also {
  val b1 = object : TitledBorder(TITLE + "1") {
    override fun paintBorder(
      c: Component,
      g: Graphics,
      x: Int,
      y: Int,
      width: Int,
      height: Int,
    ) {
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

private fun makeComp(
  str: String,
  bdr: Border,
): JLabel {
  val c = JLabel()
  c.border = bdr
  c.putClientProperty("html.disable", true)
  c.text = str
  return c
}

private class ComponentTitledBorder(
  private val comp: Component,
  private val border: Border,
) : Border, SwingConstants {
  init {
    (comp as? JComponent)?.isOpaque = true
  }

  override fun isBorderOpaque() = false

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
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

private class TitledBorder2(title: String?) : TitledBorder(title) {
  private val label2 = JLabel()

  private val position2: Int
    get() {
      val position = getTitlePosition()
      if (position != DEFAULT_POSITION) {
        return position
      }
      val value = UIManager.get("TitledBorder.position")
      if (value is Int) {
        if (value in 1..6) {
          return value
        }
      } else if (value is String) {
        return TitledBorderUtils.getPositionByString(value)
      }
      return TOP
    }

  init {
    label2.isOpaque = false
    label2.putClientProperty(BasicHTML.propertyKey, null)
    installPropertyChangeListeners()
  }

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val bdr = getBorder()
    if (bdr != null && getTitle()?.isNotEmpty() == true) {
      val edge = if (bdr is TitledBorder2) 0 else EDGE_SPACING
      val br = Rectangle(x, y, width, height)
      br.grow(-edge, -edge)

      val lr = Rectangle()
      lr.y = y
      val size = getLabel2(c).preferredSize
      lr.height = size.height

      val ins = TitledBorderUtils.getBorderInsets(bdr, c)
      initPositionRect(height, edge, ins, br, lr)
      ins.left += edge + TEXT_INSET_H2
      ins.right += edge + TEXT_INSET_H2

      initJustificationRect(c, x, width, lr, ins)

      paintWrapBorder(c, bdr, g, br, lr)
      g.translate(lr.x, lr.y)
      label2.setSize(lr.width, lr.height)
      label2.paint(g)
      g.translate(-lr.x, -lr.y)
    } else {
      super.paintBorder(c, g, x, y, width, height)
    }
  }

  private fun initJustificationRect(
    c: Component,
    x: Int,
    width: Int,
    lr: Rectangle,
    ins: Insets,
  ) {
    val sz = getLabel2(c).preferredSize
    lr.x = x
    lr.width = width - ins.left - ins.right
    if (lr.width > sz.width) {
      lr.width = sz.width
    }
    when (getJustification2(c)) {
      LEFT -> lr.x += ins.left
      RIGHT -> lr.x += width - ins.right - lr.width
      CENTER -> lr.x += (width - lr.width) / 2
    }
  }

  private fun initPositionRect(
    height: Int,
    edge: Int,
    ins: Insets,
    br: Rectangle,
    lr: Rectangle,
  ) {
    when (position2) {
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

      BELOW_TOP -> {
        val a = ins.top + edge
        lr.y += a
      }

      ABOVE_BOTTOM -> {
        val a = ins.bottom + edge
        lr.y += height - lr.height - a
      }

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

  private fun paintWrapBorder(
    c: Component,
    bdr: Border?,
    g: Graphics,
    b: Rectangle,
    l: Rectangle,
  ) {
    bdr?.also {
      if (position2 == TOP || position2 == BOTTOM) {
        val xx = l.x + l.width + TEXT_SPACING2
        val p = Path2D.Float()
        p.append(Rectangle(b.x, b.y, b.width, l.y - b.y), false)
        p.append(Rectangle(b.x, l.y, l.x - b.x - TEXT_SPACING, l.height), false)
        p.append(Rectangle(xx, l.y, b.x + b.width - xx, l.height), false)
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

  override fun getBorderInsets(
    c: Component?,
    insets: Insets,
  ): Insets {
    return if (getTitle()?.isNotEmpty() == true) {
      val edge = if (getBorder() is TitledBorder2) 0 else EDGE_SPACING
      val size = getLabel2(c).preferredSize
      TitledBorderUtils.initInsets(insets, position2, edge, size)
      insets.top += edge + TEXT_SPACING2
      insets.left += edge + TEXT_SPACING2
      insets.right += edge + TEXT_SPACING2
      insets.bottom += edge + TEXT_SPACING2
      insets
    } else {
      super.getBorderInsets(c, insets)
    }
  }

  private fun getJustification2(c: Component): Int {
    val justification = getTitleJustification()
    return when (justification) {
      DEFAULT_JUSTIFICATION -> if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
      LEADING -> if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
      TRAILING -> if (c.componentOrientation.isLeftToRight) RIGHT else LEFT
      else -> justification
    }
  }

  private fun getLabel2(c: Component?): JLabel {
    if (c != null) {
      label2.text = getTitle()
      label2.font = getFont(c)
      label2.foreground = titleColor ?: c.foreground
      label2.componentOrientation = c.componentOrientation
      label2.isEnabled = c.isEnabled
    }
    return label2
  }

  private fun installPropertyChangeListeners() {
    val weakReference = WeakReference(this)
    val listener = object : PropertyChangeListener {
      override fun propertyChange(e: PropertyChangeEvent) {
        if (weakReference.get() == null) {
          UIManager.removePropertyChangeListener(this)
          UIManager.getDefaults().removePropertyChangeListener(this)
        } else {
          val prop = e.propertyName
          if ("lookAndFeel" == prop || "LabelUI" == prop) {
            label2.updateUI()
          }
        }
      }
    }
    UIManager.addPropertyChangeListener(listener)
    UIManager.getDefaults().addPropertyChangeListener(listener)
  }

  companion object {
    const val TEXT_SPACING2 = 5
    const val TEXT_INSET_H2 = 11 // TEXT_SPACING2 * 2 + 1
  }
}

private object TitledBorderUtils {
  fun getBorderInsets(
    bdr: Border?,
    c: Component?,
  ): Insets {
    var insets = Insets(0, 0, 0, 0)
    if (bdr is AbstractBorder) {
      insets = bdr.getBorderInsets(c, insets)
    } else if (bdr != null) {
      val i = bdr.getBorderInsets(c)
      insets[i.top, i.left, i.bottom] = i.right
    }
    return insets
  }

  fun initInsets(
    insets: Insets,
    position: Int?,
    edge: Int,
    size: Dimension,
  ) {
    when (position) {
      TitledBorder.ABOVE_TOP -> insets.top += size.height - edge

      TitledBorder.TOP -> if (insets.top < size.height) {
        insets.top = size.height - edge
      }

      TitledBorder.BELOW_TOP -> insets.top += size.height

      TitledBorder.ABOVE_BOTTOM -> insets.bottom += size.height

      TitledBorder.BOTTOM -> if (insets.bottom < size.height) {
        insets.bottom = size.height - edge
      }

      TitledBorder.BELOW_BOTTOM -> insets.bottom += size.height - edge
    }
  }

  fun getPositionByString(value: String) = when (value.uppercase()) {
    "ABOVE_TOP" -> TitledBorder.ABOVE_TOP
    "TOP" -> TitledBorder.TOP
    "BELOW_TOP" -> TitledBorder.BELOW_TOP
    "ABOVE_BOTTOM" -> TitledBorder.ABOVE_BOTTOM
    "BOTTOM" -> TitledBorder.BOTTOM
    "BELOW_BOTTOM" -> TitledBorder.BELOW_BOTTOM
    else -> TitledBorder.TOP
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
