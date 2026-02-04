package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.max

fun makeUI(): Component {
  val menuBar = JMenuBar()
  val titles = arrayOf("File", "Edit", "Code", "Analyze", "Refactor", "Help")
  for (title in titles) {
    menuBar.add(makeMenu(title))
  }
  val menuLayer = JLayer<JMenuBar?>(menuBar, MenuDragLayerUI())
  return JPanel(BorderLayout()).also {
    it.add(menuLayer, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenu(title: String?): JMenu {
  val menu = JMenu(title)
  menu.add("MenuItem 1")
  menu.add("MenuItem 2")
  menu.add("MenuItem 3")
  return menu
}

private class MenuDragLayerUI : LayerUI<JMenuBar?>() {
  private var draggingMenu: JMenu? = null
  private var ghostWindow: JWindow? = null
  private var ghostLabel: JLabel? = null
  private var startPt: Point? = null
  private var isDragging = false
  private var targetIndex = -1
  private var dividerX = -1

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
    )
    ghostWindow = JWindow().also {
      it.opacity = .7f
    }
    ghostLabel = JLabel().also {
      it.setOpaque(false)
      it.setBorder(
        BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(Color.GRAY),
          BorderFactory.createEmptyBorder(2, 5, 2, 5),
        ),
      )
      it.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
    }
    ghostWindow?.add(ghostLabel)
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JMenuBar>) {
    val bar = l.getView()
    val p = SwingUtilities.convertPoint(e.component, e.getPoint(), bar)
    if (e.getID() == MouseEvent.MOUSE_PRESSED) {
      val c = bar.getComponentAt(p)
      if (c is JMenu) {
        draggingMenu = c
        startPt = p
      }
    } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
      if (isDragging && draggingMenu != null) {
        finalizeDrop(bar)
      }
      resetDragState(l)
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JMenuBar>) {
    if (draggingMenu != null) {
      val bar = l.getView()
      val p = SwingUtilities.convertPoint(e.component, e.getPoint(), bar)
      val diff = startPt?.distance(p)?.toInt() ?: 0
      if (!isDragging && diff > DRAG_THRESHOLD) {
        initiateDrag(e)
      }
      if (isDragging) {
        updateDragFeedback(e, bar, p)
        l.repaint()
        e.consume()
      }
    }
  }

  private fun initiateDrag(e: MouseEvent) {
    isDragging = true
    MenuSelectionManager.defaultManager().clearSelectedPath()
    draggingMenu?.also {
      it.setEnabled(false)
      ghostLabel?.setText(it.text)
      ghostLabel?.setFont(it.getFont())
      ghostWindow?.pack()
      updateGhostLocation(e)
      ghostWindow?.isVisible = true
    }
  }

  private fun updateDragFeedback(e: MouseEvent, bar: JMenuBar, p: Point) {
    updateGhostLocation(e)
    val menus = bar.components
    targetIndex = 0
    dividerX = if (menus.size > 0) menus[0].getX() else 0
    for (i in menus.indices) {
      val m = menus[i]
      if (m != draggingMenu) {
        val midX = m.getX() + m.getWidth() / 2
        if (p.x < midX) {
          targetIndex = i
          dividerX = m.getX()
          break
        } else {
          targetIndex = i + 1
          dividerX = m.getX() + m.getWidth()
        }
      }
    }
  }

  private fun finalizeDrop(bar: JMenuBar) {
    var currentIdx = -1
    for (i in 0..<bar.componentCount) {
      if (bar.getComponent(i) == draggingMenu) {
        currentIdx = i
        break
      }
    }
    var finalIdx = targetIndex
    if (currentIdx != -1 && currentIdx < targetIndex) {
      finalIdx--
    }
    bar.add(draggingMenu, max(0, finalIdx))
    draggingMenu?.setEnabled(true)
  }

  private fun resetDragState(l: JLayer<out JMenuBar>) {
    draggingMenu?.also {
      it.setEnabled(true)
      ghostWindow?.isVisible = false
      draggingMenu = null
      isDragging = false
      targetIndex = -1
      dividerX = -1
      l.getView().revalidate()
      l.repaint()
    }
  }

  private fun updateGhostLocation(e: MouseEvent) {
    val screenPt = e.locationOnScreen
    ghostWindow?.setLocation(screenPt.x + 10, screenPt.y + 10)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (isDragging && dividerX != -1) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.color = UIManager.getColor("List.dropLineColor")
      g2.stroke = BasicStroke(2f)
      g2.drawLine(dividerX, 0, dividerX, c.getHeight())
      g2.dispose()
    }
  }

  companion object {
    private const val DRAG_THRESHOLD = 8
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
