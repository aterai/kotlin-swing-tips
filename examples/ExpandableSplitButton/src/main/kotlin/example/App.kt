package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.LayerUI
import kotlin.math.roundToInt

fun createUI(): Component {
  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))

  val btn1 = ExpandableSplitButton("Project")
  btn1.setAlignmentX(Component.LEFT_ALIGNMENT)
  btn1.setAction(
    createAction("Project") {
      JOptionPane.showMessageDialog(
        box.rootPane,
        "'Project' title area was clicked.",
        "Title Click",
        JOptionPane.INFORMATION_MESSAGE,
      )
    },
  )
  btn1.setComponentPopupMenu(buildDefaultPopupMenu())
  box.add(createLayer(btn1))
  box.add(Box.createVerticalStrut(16))

  val btn2 = ExpandableSplitButton("Settings")
  btn2.setAlignmentX(Component.LEFT_ALIGNMENT)
  btn2.setAction(
    createAction("Settings") {
      JOptionPane.showMessageDialog(
        box.rootPane,
        "Opening 'Settings' panel.",
        "Settings",
        JOptionPane.INFORMATION_MESSAGE,
      )
    },
  )
  btn2.setComponentPopupMenu(createPopupMenu())
  box.add(createLayer(btn2))
  box.add(Box.createVerticalStrut(16))

  val btn3 = ExpandableSplitButton("Dashboard")
  btn3.setAlignmentX(Component.LEFT_ALIGNMENT)
  btn3.setAction(
    createAction("Dashboard") {
      JOptionPane.showMessageDialog(
        box.rootPane,
        "Navigating to Dashboard.",
        "Navigate",
        JOptionPane.INFORMATION_MESSAGE,
      )
    },
  )
  btn3.setIcon(CharIcon("⏰", 10))
  btn3.setComponentPopupMenu(createPopupMenu())
  box.add(createLayer(btn3))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun buildDefaultPopupMenu(): JPopupMenu {
  val popupMenu = JPopupMenu()
  for (label in listOf("Edit", "Copy", "Share", "Delete")) {
    popupMenu.add(createMenuItem(label))
  }
  return popupMenu
}

private fun createPopupMenu(): JPopupMenu {
  val popupMenu = JPopupMenu()
  for (opt in listOf("General", "Security", "Network")) {
    popupMenu.add(createMenuItem(opt))
  }
  return popupMenu
}

private fun createMenuItem(label: String): JMenuItem {
  val item = JMenuItem(label)
  item.addActionListener {
    JOptionPane.showMessageDialog(
      null,
      "$label' was selected.",
      "Action",
      JOptionPane.INFORMATION_MESSAGE,
    )
  }
  return item
}

private fun createLayer(button: ExpandableSplitButton): JLayer<*> {
  val layer = JLayer(button, SplitButtonLayerUI())
  layer.setAlignmentX(Component.LEFT_ALIGNMENT)
  return layer
}

private fun createAction(name: String, al: ActionListener): Action {
  val action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent?) {
      al.actionPerformed(e)
    }
  }
  action.putValue(Action.NAME, name)
  return action
}

private class SplitButtonLayerUI : LayerUI<ExpandableSplitButton>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
    )
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    layer: JLayer<out ExpandableSplitButton>,
  ) {
    val button = layer.getView()
    when (e.getID()) {
      MouseEvent.MOUSE_ENTERED -> {
        button.anim.expand()
        button.setForeground(
          Color(
            UIManager.getColor("List.selectionForeground").rgb,
          ),
        )
      }

      MouseEvent.MOUSE_EXITED -> {
        button.mouseOnArrow = false
        if (!button.isPopupOpen()) {
          button.anim.collapse()
        }
        button.setForeground(UIManager.getColor("List.foreground"))
      }

      MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED -> {
        if (SwingUtilities.isRightMouseButton(e)) {
          e.consume()
        } else {
          if (button.isOnArrowArea(e.getPoint())) {
            button.showPopup()
            e.consume()
          }
        }
      }

      else -> {}
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    layer: JLayer<out ExpandableSplitButton>,
  ) {
    if (e.getID() == MouseEvent.MOUSE_MOVED) {
      val btn: ExpandableSplitButton = layer.getView()
      val onArrow = btn.isOnArrowArea(e.getPoint())
      if (onArrow != btn.mouseOnArrow) {
        btn.mouseOnArrow = onArrow
        btn.repaint()
      }
    }
  }
}

class ExpandableSplitButton(
  title: String,
) : JButton(title) {
  var mouseOnArrow: Boolean = false
  var collapsedWidth = 0
  val anim = AnimationController()
  private val popupCtrl = PopupController()

  init {
    EventQueue.invokeLater {
      revalidate()
      repaint()
    }
  }

  override fun updateUI() {
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBorderPainted(false)
    setOpaque(false)
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    setHorizontalAlignment(LEFT)
    setMargin(Insets(6, 12, 6, 12))
  }

  fun isPopupOpen(): Boolean = popupCtrl.isOpen

  fun isOnArrowArea(p: Point): Boolean {
    val progress = easeInOut(anim.progress)
    return progress > .5f && p.x >= collapsedWidth && p.x <= getWidth()
  }

  fun showPopup() {
    val menu = getComponentPopupMenu()
    if (menu != null) {
      popupCtrl.show(menu, this)
    }
  }

  override fun doLayout() {
    updateCollapsedWidth()
    super.doLayout()
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    val f = EXTRA_WIDTH * easeInOut(anim.progress)
    d.width = collapsedWidth + f.roundToInt()
    return d
  }

  override fun getMinimumSize(): Dimension {
    val d = super.getMinimumSize()
    d.width = collapsedWidth
    return d
  }

  override fun getMaximumSize() = getPreferredSize()

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val w = getWidth()
    val h = getHeight()

    // Background
    val bg = if (getModel().isRollover) {
      UIManager.getColor("List.selectionBackground")
    } else {
      UIManager.getColor("List.background")
    }
    g2.color = bg
    g2.fill(
      RoundRectangle2D.Float(
        0f,
        0f,
        w.toFloat(),
        h.toFloat(),
        ARC_RADIUS.toFloat(),
        ARC_RADIUS.toFloat(),
      ),
    )

    // Separator and Border
    g2.color = UIManager.getColor("List.dropLineColor")
    val progress = easeInOut(anim.progress)
    val isProgress = progress > .5f
    if (isProgress) {
      g2.stroke = BasicStroke(1f)
      g2.drawLine(collapsedWidth, 6, collapsedWidth, h - 6)
    }
    g2.draw(
      RoundRectangle2D.Float(
        0f,
        0f,
        w - 1f,
        h - 1f,
        ARC_RADIUS.toFloat(),
        ARC_RADIUS.toFloat(),
      ),
    )
    g2.dispose()

    // Text and Icon
    super.paintComponent(g)

    // Arrow
    if (isProgress) {
      paintArrow(g, progress, h)
    }
  }

  private fun paintArrow(g: Graphics, progress: Float, h: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val hover = UIManager.getColor("List.selectionForeground")
    g2.color = if (mouseOnArrow) getForeground() else hover.darker()
    val arrowX = collapsedWidth + (EXTRA_WIDTH * progress).roundToInt() / 2
    val arrowY = h / 2
    val aw = 10
    val ah = 6
    val xp = intArrayOf(arrowX - aw / 2, arrowX + aw / 2, arrowX)
    val yp = intArrayOf(arrowY - ah / 2, arrowY - ah / 2, arrowY + ah / 2)
    g2.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g2.drawPolyline(
      intArrayOf(xp[0], xp[2], xp[1]),
      intArrayOf(yp[0], yp[2], yp[1]),
      3,
    )
    g2.dispose()
  }

  // Recalculates collapsed width from title text width.
  private fun updateCollapsedWidth() {
    val fm = getFontMetrics(getFont())
    val d = getPreferredSize()
    val viewR = Rectangle(0, 0, Int.MAX_VALUE, d.height)
    val iconR = Rectangle()
    val textR = Rectangle()
    SwingUtilities.layoutCompoundLabel(
      this,
      fm,
      text,
      icon,
      verticalAlignment,
      horizontalAlignment,
      verticalTextPosition,
      horizontalTextPosition,
      viewR,
      iconR,
      textR,
      iconTextGap,
    )
    val ins = getInsets()
    collapsedWidth = iconR.union(textR).width + ins.left + ins.right
  }

  inner class AnimationController {
    var progress = 0f
    var direction = 0
    val timer = Timer(ANIM_INTERVAL_MS) { onTick() }

    // fun getProgress() = progress

    fun expand() {
      direction = 1
      if (!timer.isRunning) {
        timer.start()
      }
    }

    fun collapse() {
      direction = -1
      if (!timer.isRunning) {
        timer.start()
      }
    }

    private fun onTick() {
      val step = 1f / ANIM_STEPS
      progress = (progress + direction * step).coerceIn(0f, 1f)
      if (progress <= 0f || progress >= 1f) {
        timer.stop()
      }
      revalidate()
      repaint()
      val parent = getParent()
      if (parent != null) {
        parent.revalidate()
      }
    }
  }

  private inner class PopupController : PopupMenuListener {
    var isOpen: Boolean = false
      private set

    fun show(menu: JPopupMenu, invoker: Component) {
      this.isOpen = true
      menu.addPopupMenuListener(this)
      menu.show(invoker, 0, invoker.getHeight() + 4)
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      this.isOpen = false
      if (!getModel().isRollover) {
        anim.collapse()
      }
      (e.source as? JPopupMenu)?.removePopupMenuListener(this)
    }
  }

  companion object {
    private const val ARC_RADIUS = 8
    private const val ANIM_INTERVAL_MS = 6
    private const val ANIM_STEPS = 10
    private const val ARROW_AREA_WIDTH = 20
    private const val EXTRA_WIDTH = ARROW_AREA_WIDTH + 8

    private fun easeInOut(t: Float) = t * t * (3f - 2f * t)
  }
}

private class CharIcon(
  private val name: String,
  private val size: Int,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = c.getForeground()
    val fontMetrics = g2.fontMetrics
    g2.translate(x, y)
    val tx = (size - fontMetrics.stringWidth(name)) / 2
    val ty = (size - fontMetrics.height) / 2 + fontMetrics.ascent
    g2.drawString(name, tx, ty)
    g2.dispose()
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
