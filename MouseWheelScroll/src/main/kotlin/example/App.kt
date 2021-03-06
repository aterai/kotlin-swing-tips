package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private var isShiftPressed = false

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val label = JLabel()
  label.icon = ImageIcon(cl.getResource("example/CRW_3857_JFR.jpg"))

  val ml = DragScrollListener()
  label.addMouseMotionListener(ml)
  label.addMouseListener(ml)

  val scroll = JScrollPane(label)
  val verticalBar = scroll.verticalScrollBar
  val horizontalBar = scroll.horizontalScrollBar
  val zeroVerticalBar = object : JScrollBar(Adjustable.VERTICAL) {
    override fun isVisible() = !isShiftPressed && super.isVisible()

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0
      return d
    }
  }
  val zeroHorizontalBar = object : JScrollBar(Adjustable.HORIZONTAL) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 0
      return d
    }
  }
  listOf(zeroVerticalBar, zeroHorizontalBar, verticalBar, horizontalBar)
    .forEach { it.unitIncrement = 25 }

  initActionMap(scroll)

  val r0 = JRadioButton("PreferredSize: 0, shift pressed: Horizontal WheelScrolling", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scroll.verticalScrollBar = zeroVerticalBar
      scroll.horizontalScrollBar = zeroHorizontalBar
    }
  }

  val r1 = JRadioButton("SCROLLBAR_ALWAYS")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scroll.verticalScrollBar = verticalBar
      scroll.horizontalScrollBar = horizontalBar
    }
  }

  val r2 = JRadioButton("SCROLLBAR_NEVER")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    }
  }

  val bg = ButtonGroup()
  listOf(r0, r1, r2).forEach { bg.add(it) }
  val b = Box.createHorizontalBox()
  val p = JPanel(GridLayout(2, 1))
  b.add(r1)
  b.add(r2)
  p.add(r0)
  p.add(b)
  scroll.verticalScrollBar = zeroVerticalBar
  scroll.horizontalScrollBar = zeroHorizontalBar

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initActionMap(scroll: JScrollPane) {
  val im = scroll.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK, false), "pressed")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true), "released")

  val am = scroll.actionMap
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      isShiftPressed = true
    }
  }
  am.put("pressed", a1)
  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      isShiftPressed = false
    }
  }
  am.put("released", a2)
}

private class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()
  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport && c is JComponent) {
      val cp = SwingUtilities.convertPoint(c, e.point, p)
      val vp = p.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      c.scrollRectToVisible(Rectangle(vp, p.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      pp.location = SwingUtilities.convertPoint(c, e.point, it)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
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
