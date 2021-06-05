package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EventListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.EventListenerList
import javax.swing.plaf.UIResource
import javax.swing.plaf.basic.BasicTabbedPaneUI

fun makeUI(): Component {
  val tab1 = JTabbedPaneWithCloseButton()
  val tab2 = JTabbedPaneWithCloseIcons()
  val tab3 = CloseableTabbedPane()
  val p = JPanel(GridLayout(3, 1))
  listOf(tab1, tab2, tab3).forEach { p.add(makeTabbedPane(it)) }
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeTabbedPane(tabbedPane: JTabbedPane): JTabbedPane {
  tabbedPane.addTab("aaa", JLabel("JLabel A"))
  tabbedPane.addTab("bb", JLabel("JLabel B"))
  tabbedPane.addTab("c", JLabel("JLabel C"))
  tabbedPane.addTab("dd dd", JLabel("JLabel D"))
  return tabbedPane
}

private class CloseableTabbedPane : JTabbedPane() {
  private var eventListenerList: EventListenerList? = null
  private var handler: CloseableTabIconHandler? = null
  val headerViewPosition
    get() = components
      .filterIsInstance<JViewport>()
      .firstOrNull { "TabbedPane.scrollableViewport" == it.name }
      ?.viewPosition ?: Point()

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    eventListenerList = EventListenerList()
    handler = CloseableTabIconHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    if (getUI() is WindowsTabbedPaneUI) {
      setUI(CloseableWindowsTabbedPaneUI())
    } else {
      setUI(CloseableTabbedPaneUI())
    }
  }

  override fun addTab(title: String?, component: Component?) {
    super.addTab(title, CloseTabIcon(null), component)
  }

//  fun addCloseableTabbedPaneListener(l: CloseableTabbedPaneListener?) {
//    eventListenerList!!.add(CloseableTabbedPaneListener::class.java, l)
//  }
//
//  fun removeCloseableTabbedPaneListener(l: CloseableTabbedPaneListener?) {
//    eventListenerList!!.remove(CloseableTabbedPaneListener::class.java, l)
//  }
//
//  val closeableTabbedPaneListener: Array<CloseableTabbedPaneListener>
//    get() = eventListenerList!!.getListeners(CloseableTabbedPaneListener::class.java)

  fun fireCloseTab(tabIndexToClose: Int): Boolean {
    var close = true
    val listeners = eventListenerList?.listenerList ?: return false
    for (o in listeners) {
      if (o is CloseableTabbedPaneListener && !o.closeTab(tabIndexToClose)) {
        close = false
        break
      }
    }
    return close
  }
}

private class CloseableTabIconHandler : MouseAdapter() {
  private val drawRect = Rectangle()
  private fun isCloseTabIconRollover(tabbedPane: CloseableTabbedPane, icon: CloseTabIcon, e: MouseEvent): Boolean {
    val rect = icon.bounds
    val pos = tabbedPane.headerViewPosition
    drawRect.setBounds(rect.x - pos.x, rect.y - pos.y, rect.width, rect.height)
    pos.translate(e.x, e.y)
    return rect.contains(pos)
  }

  override fun mouseClicked(e: MouseEvent) {
    val tabbedPane = e.component as? CloseableTabbedPane ?: return
    val icon = getCloseTabIcon(tabbedPane, e.point) ?: return
    if (isCloseTabIconRollover(tabbedPane, icon, e)) {
      val selIndex = tabbedPane.selectedIndex
      if (tabbedPane.fireCloseTab(selIndex)) {
        if (selIndex > 0) {
          val rec = tabbedPane.getBoundsAt(selIndex - 1)
          val event = MouseEvent(
            e.component, e.id + 1,
            System.currentTimeMillis(), e.modifiersEx,
            rec.x, rec.y,
            e.clickCount, e.isPopupTrigger, e.button
          )
          tabbedPane.dispatchEvent(event)
        }
        tabbedPane.remove(selIndex)
      } else {
        icon.mouseOver = false
        icon.mousePressed = false
      }
    } else {
      icon.mouseOver = false
    }
    tabbedPane.repaint(drawRect)
  }

  override fun mouseExited(e: MouseEvent) {
    val tabbedPane = e.component as? CloseableTabbedPane ?: return
    for (i in 0 until tabbedPane.tabCount) {
      (tabbedPane.getIconAt(i) as? CloseTabIcon)?.also {
        it.mouseOver = false
      }
    }
    tabbedPane.repaint()
  }

  override fun mousePressed(e: MouseEvent) {
    val tabbedPane = e.component as? CloseableTabbedPane ?: return
    val icon = getCloseTabIcon(tabbedPane, e.point)
    if (icon != null) {
      val rect = icon.bounds
      val pos = tabbedPane.headerViewPosition
      drawRect.setBounds(rect.x - pos.x, rect.y - pos.y, rect.width, rect.height)
      icon.mousePressed = e.modifiersEx and InputEvent.BUTTON1_DOWN_MASK != 0
      tabbedPane.repaint(drawRect)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val tabbedPane = e.component as? CloseableTabbedPane ?: return
    val icon = getCloseTabIcon(tabbedPane, e.point)
    if (icon != null) {
      if (isCloseTabIconRollover(tabbedPane, icon, e)) {
        icon.mouseOver = true
        icon.mousePressed = e.modifiersEx and InputEvent.BUTTON1_DOWN_MASK != 0
      } else {
        icon.mouseOver = false
      }
      tabbedPane.repaint(drawRect)
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    val tabbedPane = e.component as? CloseableTabbedPane ?: return
    val icon = getCloseTabIcon(tabbedPane, e.point) ?: return
    if (isCloseTabIconRollover(tabbedPane, icon, e)) {
      icon.mouseOver = true
      icon.mousePressed = e.modifiersEx and InputEvent.BUTTON1_DOWN_MASK != 0
    } else {
      icon.mouseOver = false
    }
    tabbedPane.repaint(drawRect)
  }

  private fun getCloseTabIcon(tabbedPane: CloseableTabbedPane, pt: Point): CloseTabIcon? {
    val tabNumber = tabbedPane.indexAtLocation(pt.x, pt.y)
    return if (tabNumber < 0) {
      null
    } else {
      tabbedPane.getIconAt(tabNumber) as? CloseTabIcon
    }
  }
}

interface CloseableTabbedPaneListener : EventListener {
  fun closeTab(tabIndexToClose: Int): Boolean
}

private class CloseableWindowsTabbedPaneUI : WindowsTabbedPaneUI() {
  private var horTextPosition = LEFT

  // constructor(horTextPosition: Int) : super() {
  //   this.horTextPosition = horTextPosition
  // }

  override fun layoutLabel(
    tabPlacement: Int,
    metrics: FontMetrics?,
    tabIndex: Int,
    title: String?,
    icon: Icon?,
    tabRect: Rectangle?,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean
  ) {
    textRect.setLocation(0, 0)
    iconRect.setLocation(0, 0)
    val v = getTextViewForTab(tabIndex)
    if (v != null) {
      tabPane.putClientProperty(HTML, v)
    }
    SwingUtilities.layoutCompoundLabel(
      tabPane,
      metrics, title, icon,
      CENTER,
      CENTER,
      CENTER, // SwingConstants.TRAILING,
      horTextPosition,
      tabRect,
      iconRect,
      textRect,
      textIconGap + 2
    )
    tabPane.putClientProperty(HTML, null)
    val xn = getTabLabelShiftX(tabPlacement, tabIndex, isSelected)
    val yn = getTabLabelShiftY(tabPlacement, tabIndex, isSelected)
    iconRect.x += xn
    iconRect.y += yn
    textRect.x += xn
    textRect.y += yn
  }

  companion object {
    private const val HTML = "html"
  }
}

private class CloseableTabbedPaneUI : BasicTabbedPaneUI() {
  private var horTextPosition = LEFT

  // constructor(horTextPosition: Int) : super() {
  //   this.horTextPosition = horTextPosition
  // }

  override fun layoutLabel(
    tabPlacement: Int,
    metrics: FontMetrics?,
    tabIndex: Int,
    title: String?,
    icon: Icon?,
    tabRect: Rectangle?,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean
  ) {
    textRect.setLocation(0, 0)
    iconRect.setLocation(0, 0)
    val v = getTextViewForTab(tabIndex)
    if (v != null) {
      tabPane.putClientProperty(HTML, v)
    }
    SwingUtilities.layoutCompoundLabel(
      tabPane,
      metrics, title, icon,
      CENTER,
      CENTER,
      CENTER, // SwingConstants.TRAILING,
      horTextPosition,
      tabRect,
      iconRect,
      textRect,
      textIconGap + 2
    )
    tabPane.putClientProperty(HTML, null)
    val xn = getTabLabelShiftX(tabPlacement, tabIndex, isSelected)
    val yn = getTabLabelShiftY(tabPlacement, tabIndex, isSelected)
    iconRect.x += xn
    iconRect.y += yn
    textRect.x += xn
    textRect.y += yn
  }

  companion object {
    private const val HTML = "html"
  }
}

private class CloseTabIcon(private val fileIcon: Icon?) : Icon {
  private var xp = 0
  private var yp = 0
  private val width = 16
  private val height = 16
  var mouseOver = false
  var mousePressed = false
  val bounds get() = Rectangle(xp, yp, width, height)

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    xp = x
    yp = y
    val yp = y + 2
    val g2 = g.create() as? Graphics2D ?: return
    if (mousePressed && mouseOver) {
      g2.paint = Color.WHITE
      g2.fillRect(x + 1, yp + 1, 12, 13)
    }
    g2.paint = if (mouseOver) Color.ORANGE else Color.BLACK
    g2.drawLine(x + 1, yp, x + 12, yp)
    g2.drawLine(x + 1, yp + 13, x + 12, yp + 13)
    g2.drawLine(x, yp + 1, x, yp + 12)
    g2.drawLine(x + 13, yp + 1, x + 13, yp + 12)
    g2.drawLine(x + 3, yp + 3, x + 10, yp + 10)
    g2.drawLine(x + 3, yp + 4, x + 9, yp + 10)
    g2.drawLine(x + 4, yp + 3, x + 10, yp + 9)
    g2.drawLine(x + 10, yp + 3, x + 3, yp + 10)
    g2.drawLine(x + 10, yp + 4, x + 4, yp + 10)
    g2.drawLine(x + 9, yp + 3, x + 3, yp + 9)
    g2.dispose()
  }

  override fun getIconWidth() = width + (fileIcon?.iconWidth ?: 0)

  override fun getIconHeight() = height
}

private class JTabbedPaneWithCloseButton : JTabbedPane() {
  private var closeButtons: MutableList<JButton>? = null
  override fun updateUI() {
    closeButtons?.forEach {
      this.remove(it)
    }
    closeButtons?.clear()
    super.updateUI()
    closeButtons = mutableListOf<JButton>().also {
      setUI(CloseButtonTabbedPaneUI(it))
    }
  }
}

private class CloseButtonTabbedPaneUI(val closeButtons: MutableList<JButton>) : BasicTabbedPaneUI() {
  override fun createLayoutManager() = object : TabbedPaneLayout() {
    override fun layoutContainer(parent: Container?) {
      super.layoutContainer(parent)
      while (tabPane.tabCount > closeButtons.size) {
        closeButtons.add(createTabCloseButton(tabPane, closeButtons.size))
      }
      var rect = Rectangle()
      val tabPlacement = tabPane.tabPlacement
      var i = 0
      while (i < tabPane.tabCount) {
        rect = getTabBounds(i, rect)
        val closeButton = closeButtons[i]
        val d = closeButton.preferredSize
        val isSelected = i == tabPane.selectedIndex
        val x = getTabLabelShiftX(tabPlacement, i, isSelected) + rect.x + rect.width - d.width - 2
        val y = getTabLabelShiftY(tabPlacement, i, isSelected) + rect.y + (rect.height - d.height) / 2
        closeButton.setBounds(x, y, d.width, d.height)
        tabPane.add(closeButton)
        i++
      }
      while (i < closeButtons.size) {
        tabPane.remove(closeButtons[i])
        i++
      }
    }
  }

  override fun getTabInsets(tabPlacement: Int, tabIndex: Int) =
    (super.getTabInsets(tabPlacement, tabIndex).clone() as? Insets)?.also {
      it.right += 40
      it.top += 2
      it.bottom += 2
    }

  companion object {
    fun createTabCloseButton(tabbedPane: JTabbedPane, index: Int) = CloseButton(tabbedPane, index)
  }
}

private class CloseButton(tabPane: JTabbedPane, index: Int) : JButton(CloseButtonAction(tabPane, index)), UIResource {
  init {
    toolTipText = "Close this tab"
    border = BorderFactory.createEmptyBorder()
    isFocusPainted = false
    isBorderPainted = false
    isContentAreaFilled = false
    isRolloverEnabled = false
    val ml = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent?) {
        foreground = Color.RED
      }

      override fun mouseExited(e: MouseEvent?) {
        foreground = Color.BLACK
      }
    }
    addMouseListener(ml)
  }

  override fun getPreferredSize() = Dimension(16, 16)
}

private class CloseButtonAction(private val tabPane: JTabbedPane, private val index: Int) : AbstractAction("x") {
  override fun actionPerformed(e: ActionEvent?) {
    tabPane.remove(index)
  }
}

private class JTabbedPaneWithCloseIcons : JTabbedPane() {
  init {
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val index = indexAtLocation(e.x, e.y)
        if (index < 0) {
          return
        }
        (getIconAt(index) as? SimpleCloseTabIcon)?.bounds?.takeIf { it.contains(e.x, e.y) }?.also {
          removeTabAt(index)
        }
      }
    })
  }

  override fun addTab(title: String?, component: Component?) {
    super.addTab(title, SimpleCloseTabIcon(null), component)
  }

  override fun addTab(title: String?, icon: Icon?, component: Component?) {
    super.addTab(title, SimpleCloseTabIcon(icon), component)
  }
}

private class SimpleCloseTabIcon(private val fileIcon: Icon?) : Icon {
  private val dim = Dimension(16, 16)
  private val pos = Point()
  val bounds get() = Rectangle(pos, dim)

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    pos.setLocation(x, y)
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y + 2)
    g2.paint = Color.BLACK
    g2.drawLine(1, 0, 12, 0)
    g2.drawLine(1, 13, 12, 13)
    g2.drawLine(0, 1, 0, 12)
    g2.drawLine(13, 1, 13, 12)
    g2.drawLine(3, 3, 10, 10)
    g2.drawLine(3, 4, 9, 10)
    g2.drawLine(4, 3, 10, 9)
    g2.drawLine(10, 3, 3, 10)
    g2.drawLine(10, 4, 4, 10)
    g2.drawLine(9, 3, 3, 9)
    fileIcon?.paintIcon(c, g2, dim.width, 0)
    g2.dispose()
  }

  override fun getIconWidth() = dim.width + (fileIcon?.iconWidth ?: 0)

  override fun getIconHeight() = dim.height
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
