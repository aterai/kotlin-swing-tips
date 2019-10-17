package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicScrollBarUI

class MainPanel : JPanel(BorderLayout()) {
  var willExpand = false

  init {
    val scroll = JScrollPane(makeList())
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    val controls = JPanel()
    val animator = Timer(10, ActionListener { controls.revalidate() })
    controls.setLayout(object : BorderLayout(0, 0) {
      private var controlsWidth = MIN_WIDTH
      override fun preferredLayoutSize(target: Container): Dimension {
        val ps = super.preferredLayoutSize(target)
        val controlsPreferredWidth = ps.width
        if (animator.isRunning()) {
          if (willExpand) {
            if (controls.width < controlsPreferredWidth) {
              controlsWidth += 1
            }
          } else {
            if (controls.width > MIN_WIDTH) {
              controlsWidth -= 1
            }
          }
          if (controlsWidth <= MIN_WIDTH) {
            controlsWidth = MIN_WIDTH
            animator.stop()
          } else if (controlsWidth >= controlsPreferredWidth) {
            controlsWidth = controlsPreferredWidth
            animator.stop()
          }
        }
        ps.width = controlsWidth
        return ps
      }
    })
    controls.add(scroll.getVerticalScrollBar())
    val p = JPanel(BorderLayout())
    p.add(controls, BorderLayout.EAST)
    p.add(scroll)
    val pp = JPanel(GridLayout(1, 2))
    pp.add(JLayer(p, object : LayerUI<JPanel>() {
      private var isDragging = false
      override fun installUI(c: JComponent) {
        super.installUI(c)
        (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
      }

      override fun uninstallUI(c: JComponent) {
        (c as? JLayer<*>)?.setLayerEventMask(0)
        super.uninstallUI(c)
      }

      override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JPanel>) {
        val id = e.getID()
        val c = e.getComponent()
        if (c is JScrollBar && id == MouseEvent.MOUSE_DRAGGED) {
          isDragging = true
        }
      }

      override fun processMouseEvent(e: MouseEvent, l: JLayer<out JPanel>) {
        if (e.getComponent() is JScrollBar) {
          when (e.getID()) {
            MouseEvent.MOUSE_ENTERED -> if (!animator.isRunning() && !isDragging) {
              willExpand = true
              animator.initialDelay = 0
              animator.start()
            }
            MouseEvent.MOUSE_EXITED -> if (!animator.isRunning() && !isDragging) {
              willExpand = false
              animator.setInitialDelay(500)
              animator.start()
            }
            MouseEvent.MOUSE_RELEASED -> {
              isDragging = false
              if (!animator.isRunning() && !e.getComponent().getBounds().contains(e.getPoint())) {
                willExpand = false
                animator.setInitialDelay(500)
                animator.start()
              }
            }
          }
          l.getView().repaint()
        }
      }
    }))
    pp.add(
      JLayer<JScrollPane>(
        makeTranslucentScrollBar(makeList()),
        ScrollBarOnHoverLayerUI()
      )
    )
    add(pp)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeList(): Component {
    val m = DefaultListModel<String>()
    (0 until 50).map {
      String.format("%05d: %s", it, LocalDateTime.now(ZoneId.systemDefault()))
    }.forEach { m.addElement(it) }
    return JList(m)
  }

  private fun makeTranslucentScrollBar(c: Component): JScrollPane {
    return object : JScrollPane(c) {
      override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

      override fun updateUI() {
        super.updateUI()
        EventQueue.invokeLater {
          getVerticalScrollBar().setUI(TranslucentScrollBarUI())
          setComponentZOrder(getVerticalScrollBar(), 0)
          setComponentZOrder(getViewport(), 1)
          getVerticalScrollBar().setOpaque(false)
          getVerticalScrollBar().setPreferredSize(Dimension(6, 0))
        }
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        setLayout(TranslucentScrollPaneLayout())
      }
    }
  }

  companion object {
    private const val MIN_WIDTH = 6
  }
}

class TranslucentScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    if (parent is JScrollPane) {
      val availR = parent.getBounds()
      availR.setLocation(0, 0) // availR.x = availR.y = 0;

      val insets = parent.getInsets()
      availR.x = insets.left
      availR.y = insets.top
      availR.width -= insets.left + insets.right
      availR.height -= insets.top + insets.bottom
      val vsbR = Rectangle()
      vsbR.width = 12
      vsbR.height = availR.height
      vsbR.x = availR.x + availR.width - vsbR.width
      vsbR.y = availR.y
      viewport?.setBounds(availR)
      vsb?.setVisible(true)
      vsb?.setBounds(vsbR)
    }
  }
}

class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent, r: Rectangle) {
    // val g2 = g.create() as? Graphics2D ?: return
    // g2.setPaint(new Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent, r: Rectangle) {
    val sb = c as? JScrollBar
    val color: Color
    if (sb == null || !sb.isEnabled || r.width > r.height) {
      return
    } else if (isDragging) {
      color = DRAGGING_COLOR
    } else if (isThumbRollover) {
      color = ROLLOVER_COLOR
    } else {
      color = DEFAULT_COLOR
      val dw = r.width - sb.getPreferredSize().width
      r.x += dw
      r.width -= dw
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(color)
    g2.fillRect(r.x, r.y, r.width - 2, r.height - 1)
    g2.dispose()
  }

  companion object {
    const val MAX_WIDTH = 12
    const val MIN_WIDTH = 6
    private val DEFAULT_COLOR = Color(100, 100, 100, 190)
    private val DRAGGING_COLOR = Color(100, 100, 100, 220)
    private val ROLLOVER_COLOR = Color(100, 100, 100, 220)
  }
}

class ScrollBarOnHoverLayerUI : LayerUI<JScrollPane>() {
  private val timer = Timer(2000, null)
  @Transient
  private var listener: ActionListener? = null

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.getComponent() as? JScrollBar ?: return
    when (e.getID()) {
      MouseEvent.MOUSE_ENTERED ->
        c.setPreferredSize(Dimension(TranslucentScrollBarUI.MAX_WIDTH, 0))
      MouseEvent.MOUSE_EXITED -> {
        timer.removeActionListener(listener)
        listener = ActionListener {
          c.setPreferredSize(Dimension(TranslucentScrollBarUI.MIN_WIDTH, 0))
          l.getView().revalidate()
          l.getView().repaint()
        }
        timer.addActionListener(listener)
        timer.setRepeats(false)
        timer.start()
      }
    }
    l.view.repaint()
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
