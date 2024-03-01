package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.text.DefaultEditorKit
import kotlin.math.sqrt

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = makeMenuBar() }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeMenuBar(): JMenuBar {
  val actions = listOf(
    makeButton("Cut", DefaultEditorKit.CutAction()),
    makeButton("Copy", DefaultEditorKit.CopyAction()),
    makeButton("Paste", DefaultEditorKit.PasteAction()),
  )
  val edit = makeEditButtonBar(actions)

  val menu = JMenu("File").also {
    it.add("1111111111111")
    it.addSeparator()
    it.add(makeEditMenuItem(edit))
    it.addSeparator()
    it.add("2222")
    it.add("333333")
    it.add("44444")
  }
  return JMenuBar().also { it.add(menu) }
}

private fun makeEditMenuItem(edit: Component): JMenuItem {
  val item = object : JMenuItem("Edit") {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width += edit.preferredSize.width
      d.height = maxOf(edit.preferredSize.height, d.height)
      return d
    }

    override fun fireStateChanged() {
      foreground = Color.BLACK
      super.fireStateChanged()
    }
  }
  item.isEnabled = false

  val c = GridBagConstraints()
  item.layout = GridBagLayout()
  c.anchor = GridBagConstraints.LINE_END
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
    it.icon = ToggleButtonBarCellIcon()
    p.add(it)
  }
  p.border = BorderFactory.createEmptyBorder(4, 10, 4, 10)
  p.isOpaque = false
  return JLayer<Component>(p, EditMenuLayerUI(list[size - 1]))
}

private fun makeButton(
  title: String,
  action: Action,
): AbstractButton {
  val b = JButton(action)
  b.addActionListener { e ->
    val c = e.source
    if (c is Component) {
      val pop = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, c)
      if (pop is JPopupMenu) {
        pop.isVisible = false
      }
    }
  }
  b.text = title
  // b.verticalAlignment = SwingConstants.CENTER
  // b.verticalTextPosition = SwingConstants.CENTER
  // b.horizontalAlignment = SwingConstants.CENTER
  b.horizontalTextPosition = SwingConstants.CENTER
  b.border = BorderFactory.createEmptyBorder()
  b.isContentAreaFilled = false
  b.isFocusPainted = false
  b.isOpaque = false
  return b
}

// https://ateraimemo.com/Swing/ToggleButtonBar.html
private class ToggleButtonBarCellIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val parent = c.parent ?: return
    val r = 4.0
    val rr = r * 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    var dw = c.width.toDouble()
    val dh = c.height - 1.0

    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val p = Path2D.Double()
    when {
      c === parent.getComponent(0) -> {
        // :first-child
        p.moveTo(dx, dy + r)
        p.curveTo(dx, dy + r - rr, dx + r - rr, dy, dx + r, dy)
        p.lineTo(dx + dw, dy)
        p.lineTo(dx + dw, dy + dh)
        p.lineTo(dx + r, dy + dh)
        p.curveTo(dx + r - rr, dy + dh, dx, dy + dh - r + rr, dx, dy + dh - r)
      }

      c === parent.getComponent(parent.componentCount - 1) -> {
        // :last-child
        dw--
        p.moveTo(dx, dy)
        p.lineTo(dx + dw - r, dy)
        p.curveTo(dx + dw - r + rr, dy, dx + dw, dy + r - rr, dx + dw, dy + r)
        p.lineTo(dx + dw, dy + dh - r)
        p.curveTo(dx + dw, dy + dh - r + rr, dx + dw - r + rr, dy + dh, dx + dw - r, dy + dh)
        p.lineTo(dx, dy + dh)
      }

      else -> {
        p.moveTo(dx, dy)
        p.lineTo(dx + dw, dy)
        p.lineTo(dx + dw, dy + dh)
        p.lineTo(dx, dy + dh)
      }
    }
    p.closePath()
    val area = Area(p)

    var color = Color(0x0, true)
    var borderColor = Color.GRAY.brighter()
    if (c is AbstractButton) {
      val m = c.model
      if (m.isPressed) {
        color = Color(0xC8_C8_FF)
      } else if (m.isSelected || m.isRollover) {
        borderColor = Color.GRAY
      }
    }
    g2.paint = color
    g2.fill(area)
    g2.paint = borderColor
    g2.draw(area)
    g2.dispose()
  }

  override fun getIconWidth() = 40

  override fun getIconHeight() = 20
}

private class EditMenuLayerUI<V : Component>(
  private val lastButton: AbstractButton,
) : LayerUI<V>() {
  private var shape: Shape? = null

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    shape?.also {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.GRAY
      g2.draw(it)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  private fun update(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    val id = e.id
    var s: Shape? = null
    if (id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_MOVED) {
      val c = e.component
      if (c != lastButton) {
        val r = c.bounds
        s = Line2D.Double(r.maxX, r.getY(), r.maxX, r.maxY - 1.0)
      }
    }
    if (s != shape) {
      shape = s
      l.view.repaint()
    }
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    update(e, l)
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
