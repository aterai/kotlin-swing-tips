package example

import java.awt.*
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

fun makeUI(): Component {
  val model = arrayOf("111", "2222", "33333")
  val combo0 = JComboBox(model)
  val combo1 = object : JComboBox<String>(model) {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      setRenderer(null)
      removePopupMenuListener(listener)
      super.updateUI()
      border = RoundedCornerBorder()
      setRenderer(RoundedCornerListCellRenderer<Any>())
      setUI(object : BasicComboBoxUI() {
        override fun createArrowButton(): JButton {
          val b = JButton(ArrowIcon(Color.WHITE, Color.BLACK))
          b.isContentAreaFilled = false
          b.isFocusPainted = false
          b.border = BorderFactory.createEmptyBorder()
          return b
        }

        override fun createPopup() = object : BasicComboPopup(comboBox) {
          override fun createScroller() = object : JScrollPane(list) {
            override fun updateUI() {
              super.updateUI()
              verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
              horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
              verticalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
              horizontalScrollBar.setUI(WithoutArrowButtonScrollBarUI())
              // horizontalScrollBar = null
            }
          }
        }
      })
      listener = HeavyWeightContainerListener()
      addPopupMenuListener(listener)
      val o = getAccessibleContext().getAccessibleChild(0)
      if (o is JComponent) {
        o.border = BottomRoundedCornerBorder()
        o.background = Color.WHITE
      }
    }
  }

  val check = JCheckBox("setEditable")
  check.isOpaque = false
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    for (c in listOf(combo0, combo1)) {
      c.isEditable = b
      c.repaint()
    }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(makeTitledPanel("Default JComboBox", JComboBox(model)))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("RoundedCornerListCellRenderer", combo1))

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(check)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title)
  p.add(c)
  return p
}

private class RoundedCornerListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = object : DefaultListCellRenderer() {
    override fun paintComponent(g: Graphics) {
      if (icon != null) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.paint = background
        val r = SwingUtilities.calculateInnerArea(this, null)
        val rr = RoundRectangle2D.Double(
          r.x.toDouble(),
          r.y.toDouble(),
          r.width.toDouble(),
          r.height.toDouble(),
          12.0,
          12.0,
        )
        g2.fill(rr)
        super.paintComponent(g2)
        g2.dispose()
      } else {
        super.paintComponent(g)
      }
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JLabel) {
      c.isOpaque = false
      c.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      c.iconTextGap = 0
      val isListItem = index >= 0
      c.icon = if (isListItem) GapIcon() else null
      c.isOpaque = !isListItem
    }
    return c
  }
}

private class HeavyWeightContainerListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val combo = e.source as? JComboBox<*> ?: return
    combo.border = TopRoundedCornerBorder()
    EventQueue.invokeLater {
      val pop = combo.ui.getAccessibleChild(combo, 0)
      if (pop is JPopupMenu) {
        SwingUtilities
          .getWindowAncestor(pop)
          ?.takeIf { it.graphicsConfiguration?.isTranslucencyCapable == true }
          ?.takeIf { it is JWindow && it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    (e.source as? JComponent)?.border = RoundedCornerBorder()
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

private class ArrowIcon(
  private val color: Color,
  private val rollover: Color,
) : Icon {
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
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val dr = ARC.toDouble() * 2.0
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
    g2.paint = Color.GRAY
    g2.draw(round)
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(
    c: Component,
    insets: Insets,
  ) = insets.also {
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
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    if (c is JPopupMenu) {
      g2.clearRect(x, y, width, height)
    }
    val dr = ARC.toDouble() * 2.0
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
    g2.paint = Color.GRAY
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
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val r = ARC.toDouble()
    val w = width - 1.0
    val h = height - 1.0

    val p = Path2D.Double()
    p.moveTo(x.toDouble(), y.toDouble())
    p.lineTo(x.toDouble(), y + h - r)
    p.quadTo(x.toDouble(), y + h, x + r, y + h)
    p.lineTo(x + w - r, y + h)
    p.quadTo(x + w, y + h, x + w, y + h - r)
    p.lineTo(x + w, y.toDouble())
    p.closePath()

    g2.paint = c.background
    g2.fill(p)

    g2.paint = Color.GRAY
    g2.draw(p)
    g2.paint = c.background
    g2.drawLine(x + 1, y, x + width - 2, y)
    g2.dispose()
  }
}

private class GapIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Empty paint
  }

  override fun getIconWidth() = 2

  override fun getIconHeight() = 18
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
    if (c is JScrollBar && c.isEnabled) {
      val m = c.model
      if (m.maximum - m.minimum - m.extent > 0) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
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
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
