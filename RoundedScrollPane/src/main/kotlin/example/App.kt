package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.basic.BasicScrollBarUI
import kotlin.math.sqrt

private val BACKGROUND = Color.WHITE
private val FOREGROUND = Color.BLACK
private val SELECTION_FOREGROUND = Color.BLUE
private val THUMB = Color(0xCD_CD_CD)
private const val KEY = "ComboBox.border"

fun makeUI(): Component {
  UIManager.put("ScrollBar.width", 10)
  // ScrollBar.thumbHeight: GTKLookAndFeel, SynthLookAndFeel, NimbusLookAndFeel
  UIManager.put("ScrollBar.thumbHeight", 20)
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(30, 30))
  UIManager.put("ScrollBar.incrementButtonGap", 0)
  UIManager.put("ScrollBar.decrementButtonGap", 0)
  UIManager.put("ScrollBar.thumb", THUMB)
  UIManager.put("ScrollBar.track", BACKGROUND)
  UIManager.put("ComboBox.foreground", FOREGROUND)
  UIManager.put("ComboBox.background", BACKGROUND)
  UIManager.put("ComboBox.selectionForeground", SELECTION_FOREGROUND)
  UIManager.put("ComboBox.selectionBackground", BACKGROUND)
  UIManager.put("ComboBox.buttonDarkShadow", BACKGROUND)
  UIManager.put("ComboBox.buttonBackground", FOREGROUND)
  UIManager.put("ComboBox.buttonHighlight", FOREGROUND)
  UIManager.put("ComboBox.buttonShadow", FOREGROUND)
  val combo = object : JComboBox<String>(makeModel()) {
    private var handler: MouseListener? = null
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removeMouseListener(handler)
      removePopupMenuListener(listener)
      UIManager.put(KEY, TopRoundedCornerBorder())
      super.updateUI()
      val tmp = object : BasicComboBoxUI() {
        override fun createArrowButton() = JButton(ArrowIcon(BACKGROUND, FOREGROUND)).also {
          it.isContentAreaFilled = false
          it.isFocusPainted = false
          it.border = BorderFactory.createEmptyBorder()
        }

        override fun createPopup() = object : BasicComboPopup(comboBox) {
          override fun createScroller() = object : JScrollPane(list) {
            override fun updateUI() {
              super.updateUI()
              verticalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
              horizontalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
            }
          }.also {
            it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            it.horizontalScrollBar = null
          }
        }
      }
      setUI(tmp)
      handler = ComboRolloverHandler()
      addMouseListener(handler)
      listener = HeavyWeightContainerListener()
      addPopupMenuListener(listener)
      (getAccessibleContext().getAccessibleChild(0) as? JComponent)?.also {
        it.border = BottomRoundedCornerBorder()
        it.foreground = FOREGROUND
        it.background = BACKGROUND
      }
    }
  }

  val p = JPanel(GridLayout(0, 1, 15, 15))
  p.isOpaque = true
  p.add(combo)

  val tree = JTree()
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row)
    row++
  }

  val scroll = object : JScrollPane(tree) {
    override fun updateUI() {
      super.updateUI()
      verticalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
      horizontalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
    }
  }
  scroll.background = tree.background
  scroll.border = RoundedCornerBorder()

  return JPanel(BorderLayout(15, 15)).also {
    it.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    it.add(scroll)
    it.add(p, BorderLayout.NORTH)
    it.isOpaque = true
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("1234")
  model.addElement("5555555555555555555555")
  model.addElement("6789000000000")
  model.addElement("aaa")
  model.addElement("999999999")
  model.addElement("1234555")
  model.addElement("555555555555")
  model.addElement("666666")
  model.addElement("bbb")
  model.addElement("444444444")
  model.addElement("1234")
  model.addElement("000000000000000")
  model.addElement("2222222222")
  model.addElement("ccc")
  model.addElement("111111111111111111")
  return model
}

private class HeavyWeightContainerListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val c = e.source as? JComboBox<*> ?: return
    EventQueue.invokeLater {
      val pop = c.getUI().getAccessibleChild(c, 0)
      val top = (pop as? JPopupMenu)?.topLevelAncestor
      if (top is JWindow && top.type == Window.Type.POPUP) {
        top.background = Color(0x0, true)
      }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

private class ComboRolloverHandler : MouseAdapter() {
  override fun mouseEntered(e: MouseEvent) {
    getButtonModel(e)?.isRollover = true
  }

  override fun mouseExited(e: MouseEvent) {
    getButtonModel(e)?.isRollover = false
  }

  override fun mousePressed(e: MouseEvent) {
    getButtonModel(e)?.isPressed = true
  }

  override fun mouseReleased(e: MouseEvent) {
    getButtonModel(e)?.isPressed = false
  }

  private fun getButtonModel(e: MouseEvent) =
    ((e.component as? Container)?.getComponent(0) as? JButton)?.model
}

private class ArrowIcon(private val color: Color, private val rollover: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = color
    var shift = 0
    if (c is AbstractButton) {
      val m = c.model
      if (m.isPressed) {
        shift = 1
      } else {
        if (m.isRollover) {
          g2.paint = rollover
        }
      }
    }
    g2.translate(x, y + shift)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}

private open class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val dr = ARC * 2.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val round = Area(RoundRectangle2D.Double(dx, dy, dw - 1, dh - 1, dr, dr))
    if (c is JPopupMenu) {
      g2.paint = c.background
      g2.fill(round)
    } else {
      c.parent?.also {
        g2.paint = it.background
        val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
        corner.subtract(round)
        g2.fill(corner)
      }
    }
    g2.paint = c.foreground
    g2.draw(round)
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(c: Component, insets: Insets) = insets.also {
    it.set(4, 8, 4, 8)
  }

  companion object {
    const val ARC = 6
  }
}

private class TopRoundedCornerBorder : RoundedCornerBorder() {
  // https://ateraimemo.com/Swing/RoundedComboBox.html
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (c is JPopupMenu) {
      g2.clearRect(x, y, width, height)
    }
    val dr = ARC * 2.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val round = Area(RoundRectangle2D.Double(dx, dy, dw - 1.0, dh - 1.0, dr, dr))
    val b = round.bounds
    b.setBounds(b.x, b.y + ARC, b.width, b.height - ARC)
    round.add(Area(b))

    c.parent?.also {
      g2.paint = it.background
      val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.paint = c.foreground
    g2.draw(round)
    g2.dispose()
  }
}

private class BottomRoundedCornerBorder : RoundedCornerBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = ARC.toDouble()
    val rr = r * 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val w = width - 1.0
    val h = height - 1.0

    val p = Path2D.Double()
    p.moveTo(x.toDouble(), y.toDouble())
    p.lineTo(x.toDouble(), y + h - r)
    p.curveTo(x.toDouble(), y + h - r + rr, x + r - rr, y + h, x + r, y + h)
    p.lineTo(x + w - r, y + h)
    p.curveTo(x + w - r + rr, y + h, x + w, y + h - r + rr, x + w, y + h - r)
    p.lineTo(x + w, y.toDouble())
    p.closePath()

    g2.paint = c.background
    g2.fill(p)

    g2.paint = c.foreground
    g2.draw(p)
    g2.paint = c.background
    g2.drawLine(x + 1, y, x + width - 2, y)
    g2.dispose()
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class WithoutArrowButtonScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = trackColor
    g2.fill(r)
    g2.dispose()
  }

  override fun paintThumb(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    if (c !is JScrollBar || !c.isEnabled) {
      return
    }
    val m = c.model
    if (m.maximum - m.minimum - m.extent > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = when {
        isDragging -> thumbDarkShadowColor
        isThumbRollover -> thumbLightShadowColor
        else -> thumbColor
      }
      g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 10, 10)
      g2.dispose()
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
