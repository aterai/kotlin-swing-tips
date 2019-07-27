package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.text.DefaultEditorKit

class MainPanel : JPanel(BorderLayout()) {
  init {
    add(JScrollPane(JTextArea()))
    EventQueue.invokeLater { getRootPane().setJMenuBar(makeManuBar()) }
    setPreferredSize(Dimension(320, 240))
  }

  fun makeManuBar(): JMenuBar {
    val edit = makeEditButtonBar(listOf(
        makeButton("Cut", DefaultEditorKit.CutAction()),
        makeButton("Copy", DefaultEditorKit.CopyAction()),
        makeButton("Paste", DefaultEditorKit.PasteAction())))

    val menu = JMenu("File").also {
      it.add("aaaaaaaaaa")
      it.addSeparator()
      it.add(makeEditMenuItem(edit))
      it.addSeparator()
      it.add("bbbb")
      it.add("cccccc")
      it.add("ddddd")
    }

    return JMenuBar().also { it.add(menu) }
  }

  private fun makeEditMenuItem(edit: Component): JMenuItem {
    val item = object : JMenuItem("Edit") {
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.width += edit.getPreferredSize().width
        d.height = maxOf(edit.getPreferredSize().height, d.height)
        return d
      }

      protected override fun fireStateChanged() {
        setForeground(Color.BLACK)
        super.fireStateChanged()
      }
    }
    item.setEnabled(false)

    val c = GridBagConstraints()
    item.setLayout(GridBagLayout())
    c.anchor = GridBagConstraints.LINE_END
    // c.gridx = GridBagConstraints.RELATIVE
    c.weightx = 1.0

    c.fill = GridBagConstraints.HORIZONTAL
    item.add(Box.createHorizontalGlue(), c)
    c.fill = GridBagConstraints.NONE
    item.add(edit, c)

    return item
  }

  private fun makeEditButtonBar(list: List<AbstractButton>): Component {
    val size = list.size
    val p = object : JPanel(GridLayout(1, size, 0, 0)) {
      override fun getMaximumSize() = super.getPreferredSize()
    }
    list.forEach {
      it.setIcon(ToggleButtonBarCellIcon())
      p.add(it)
    }
    p.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10))
    p.setOpaque(false)

    return JLayer<Component>(p, EditMenuLayerUI(list[size - 1]))
  }

  private fun makeButton(title: String, action: Action): AbstractButton {
    val b = JButton(action)
    b.addActionListener { e ->
      val c = e.getSource() as? Component ?: return@addActionListener
      (SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, c) as? JPopupMenu)?.setVisible(false)
    }
    b.setText(title)
    // b.setVerticalAlignment(SwingConstants.CENTER)
    // b.setVerticalTextPosition(SwingConstants.CENTER)
    // b.setHorizontalAlignment(SwingConstants.CENTER)
    b.setHorizontalTextPosition(SwingConstants.CENTER)
    b.setBorder(BorderFactory.createEmptyBorder())
    b.setContentAreaFilled(false)
    b.setFocusPainted(false)
    b.setOpaque(false)
    return b
  }
}

// https://ateraimemo.com/Swing/ToggleButtonBar.html
class ToggleButtonBarCellIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val parent = c.getParent() ?: return
    val r = 8
    var w = c.getWidth()
    val h = c.getHeight() - 1

    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val p = Path2D.Double()
    if (c === parent.getComponent(0)) {
      // :first-child
      p.moveTo(x.toDouble(), (y + r).toDouble())
      p.quadTo(x.toDouble(), y.toDouble(), (x + r).toDouble(), y.toDouble())
      p.lineTo((x + w).toDouble(), y.toDouble())
      p.lineTo((x + w).toDouble(), (y + h).toDouble())
      p.lineTo((x + r).toDouble(), (y + h).toDouble())
      p.quadTo(x.toDouble(), (y + h).toDouble(), x.toDouble(), (y + h - r).toDouble())
    } else if (c === parent.getComponent(parent.getComponentCount() - 1)) {
      // :last-child
      w--
      p.moveTo(x.toDouble(), y.toDouble())
      p.lineTo((x + w - r).toDouble(), y.toDouble())
      p.quadTo((x + w).toDouble(), y.toDouble(), (x + w).toDouble(), (y + r).toDouble())
      p.lineTo((x + w).toDouble(), (y + h - r).toDouble())
      p.quadTo((x + w).toDouble(), (y + h).toDouble(), (x + w - r).toDouble(), (y + h).toDouble())
      p.lineTo(x.toDouble(), (y + h).toDouble())
    } else {
      p.moveTo(x.toDouble(), y.toDouble())
      p.lineTo((x + w).toDouble(), y.toDouble())
      p.lineTo((x + w).toDouble(), (y + h).toDouble())
      p.lineTo(x.toDouble(), (y + h).toDouble())
    }
    p.closePath()
    val area = Area(p)

    var color = Color(0x0, true)
    var borderColor = Color.GRAY.brighter()
    if (c is AbstractButton) {
      val m = c.getModel()
      if (m.isPressed()) {
        color = Color(0xC8_C8_FF)
      } else if (m.isSelected() || m.isRollover()) {
        borderColor = Color.GRAY
      }
    }
    g2.setPaint(color)
    g2.fill(area)
    g2.setPaint(borderColor)
    g2.draw(area)
    g2.dispose()
  }

  override fun getIconWidth() = 40

  override fun getIconHeight() = 20
}

class EditMenuLayerUI<V : Component>(private val lastButton: AbstractButton) : LayerUI<V>() {
  private var shape: Shape? = null

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    shape?.also {
      val g2 = g.create() as Graphics2D
      g2.setPaint(Color.GRAY)
      g2.draw(it)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  private fun update(e: MouseEvent, l: JLayer<out V>) {
    val id = e.getID()
    var s: Shape? = null
    if (id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_MOVED) {
      val c = e.getComponent()
      if (c != lastButton) {
        val r = c.getBounds()
        val x = r.getX()
        val y = r.getY()
        val w = r.getWidth()
        val h = r.getHeight()
        s = Line2D.Double(x + w, y, x + w, y + h - 1.0)
      }
    }
    if (s != shape) {
      shape = s
      l.getView().repaint()
    }
  }

  protected override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    update(e, l)
  }

  protected override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    update(e, l)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
