package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.AbstractBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboBoxUI

class MainPanel : JPanel(BorderLayout()) {
  companion object {
    val BACKGROUND: Color = Color.BLACK
    val FOREGROUND: Color = Color.WHITE
    val SELECTION_FOREGROUND: Color = Color.ORANGE
    val PANEL_BACKGROUND: Color = Color.GRAY
    const val KEY = "ComboBox.border"
  }

  init {
    UIManager.put("ComboBox.foreground", FOREGROUND)
    UIManager.put("ComboBox.background", BACKGROUND)
    UIManager.put("ComboBox.selectionForeground", SELECTION_FOREGROUND)
    UIManager.put("ComboBox.selectionBackground", BACKGROUND)
    UIManager.put("ComboBox.buttonDarkShadow", BACKGROUND)
    UIManager.put("ComboBox.buttonBackground", FOREGROUND)
    UIManager.put("ComboBox.buttonHighlight", FOREGROUND)
    UIManager.put("ComboBox.buttonShadow", FOREGROUND)
    val combo0 = object : JComboBox<String>(makeModel()) {
      override fun updateUI() {
        UIManager.put(KEY, BorderFactory.createLineBorder(FOREGROUND))
        super.updateUI()
        setUI(BasicComboBoxUI())
        (getAccessibleContext().getAccessibleChild(0) as? JComponent)?.also {
          it.setBorder(BorderFactory.createLineBorder(FOREGROUND))
          it.setForeground(FOREGROUND)
          it.setBackground(BACKGROUND)
        }
      }
    }
    val combo1 = object : JComboBox<String>(makeModel()) {
      @Transient
      private var listener: PopupMenuListener? = null

      override fun updateUI() {
        removePopupMenuListener(listener)
        UIManager.put(KEY, RoundedCornerBorder())
        super.updateUI()
        setUI(BasicComboBoxUI())
        listener = HeavyWeightContainerListener()
        addPopupMenuListener(listener)
        (getAccessibleContext().getAccessibleChild(0) as? JComponent)?.also {
          it.setBorder(RoundedCornerBorder())
          it.setForeground(FOREGROUND)
          it.setBackground(BACKGROUND)
        }
      }
    }
    val combo2 = object : JComboBox<String>(makeModel()) {
      @Transient
      private var handler: MouseListener? = null
      @Transient
      private var listener: PopupMenuListener? = null

      override fun updateUI() {
        removeMouseListener(handler)
        removePopupMenuListener(listener)
        UIManager.put(KEY, TopRoundedCornerBorder())
        super.updateUI()
        setUI(object : BasicComboBoxUI() {
          override fun createArrowButton(): JButton {
            val b = JButton(ArrowIcon(BACKGROUND, FOREGROUND))
            b.setContentAreaFilled(false)
            b.setFocusPainted(false)
            b.setBorder(BorderFactory.createEmptyBorder())
            return b
          }
        })
        handler = ComboRolloverHandler()
        addMouseListener(handler)
        listener = HeavyWeightContainerListener()
        addPopupMenuListener(listener)
        (getAccessibleContext().getAccessibleChild(0) as? JComponent)?.also {
          it.setBorder(BottomRoundedCornerBorder())
          it.setForeground(FOREGROUND)
          it.setBackground(BACKGROUND)
        }
      }
    }
    val p = JPanel(GridLayout(0, 1, 15, 15))
    p.setOpaque(true)
    p.setBackground(PANEL_BACKGROUND)
    p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15))
    p.add(combo0)
    p.add(combo1)
    p.add(combo2)
    add(p, BorderLayout.NORTH)
    setOpaque(true)
    setBackground(PANEL_BACKGROUND)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel(): DefaultComboBoxModel<String> {
    val model = DefaultComboBoxModel<String>()
    model.addElement("1234")
    model.addElement("5555555555555555555555")
    model.addElement("6789000000000")
    model.addElement("aaa")
    model.addElement("999999999")
    return model
  }
}

class HeavyWeightContainerListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    EventQueue.invokeLater {
      val combo = e.getSource() as? JComboBox<*> ?: return@invokeLater
      (combo.getUI().getAccessibleChild(combo, 0) as? JPopupMenu)?.also {
        (it.getTopLevelAncestor() as? JWindow)?.also { top ->
          println("HeavyWeightContainer")
          top.setBackground(Color(0x0, true))
        }
      }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    /* not needed */
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    /* not needed */
  }
}

class ComboRolloverHandler : MouseAdapter() {
  override fun mouseEntered(e: MouseEvent) {
    getButtonModel(e)?.setRollover(true)
  }

  override fun mouseExited(e: MouseEvent) {
    getButtonModel(e)?.setRollover(false)
  }

  override fun mousePressed(e: MouseEvent) {
    getButtonModel(e)?.setRollover(true)
  }

  override fun mouseReleased(e: MouseEvent) {
    getButtonModel(e)?.setRollover(false)
  }

  private fun getButtonModel(e: MouseEvent): ButtonModel? {
    val c = e.getComponent() as? Container ?: return null
    return (c.getComponent(0) as? JButton)?.getModel()
  }
}

class ArrowIcon(private val color: Color, private val rollover: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(color)
    var shift = 0
    if (c is AbstractButton) {
      val m = c.getModel()
      if (m.isPressed()) {
        shift = 1
      } else {
        if (m.isRollover()) {
          g2.setPaint(rollover)
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

open class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = ARC.toDouble()
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val round = Area(RoundRectangle2D.Double(dx, dy, dw - 1.0, dh - 1.0, r, r))
    if (c is JPopupMenu) {
      g2.setPaint(c.getBackground())
      g2.fill(round)
    } else {
      c.getParent()?.also {
        g2.setPaint(it.getBackground())
        val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
        corner.subtract(round)
        g2.fill(corner)
      }
    }
    g2.setPaint(c.getForeground())
    g2.draw(round)
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(c: Component, insets: Insets): Insets {
    insets.set(4, 8, 4, 8)
    return insets
  }

  companion object {
    const val ARC = 12
  }
}

class TopRoundedCornerBorder : RoundedCornerBorder() {
  // https://ateraimemo.com/Swing/RoundedComboBox.html
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (c is JPopupMenu) {
      g2.clearRect(x, y, width, height)
    }
    val r = ARC.toDouble()
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val round = Area(RoundRectangle2D.Double(dx, dy, dw - 1.0, dh - 1.0, r, r))
    val b = round.getBounds()
    b.setBounds(b.x, b.y + ARC, b.width, b.height - ARC)
    round.add(Area(b))

    c.getParent()?.also {
      g2.setPaint(it.getBackground())
      val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.setPaint(c.getForeground())
    g2.draw(round)
    g2.dispose()
  }
}

class BottomRoundedCornerBorder : RoundedCornerBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
    // Area round = new Area(p)

    g2.setPaint(c.getBackground())
    g2.fill(p)

    g2.setPaint(c.getForeground())
    g2.draw(p)
    g2.setPaint(c.getBackground())
    g2.drawLine(x + 1, y, x + width - 2, y)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
