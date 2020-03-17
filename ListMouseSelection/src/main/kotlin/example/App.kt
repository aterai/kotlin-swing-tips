package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(1, 3)) {
  init {
    add(makeTitledPanel("Default", JList(makeModel())))
    add(makeTitledPanel("MouseEvent", SingleMouseClickSelectList(makeModel())))
    add(makeTitledPanel("SelectionInterval", SingleClickSelectList(makeModel())))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel() = DefaultListModel<String>().also {
    it.addElement("111111111")
    it.addElement("22222222222222")
    it.addElement("333333333")
    it.addElement("44444444")
    it.addElement("5555555555")
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(JScrollPane(c))
  }
}

class SingleMouseClickSelectList<E>(model: ListModel<E>) : JList<E>(model) {
  override fun updateUI() {
    setForeground(null)
    setBackground(null)
    setSelectionForeground(null)
    setSelectionBackground(null)
    super.updateUI()
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    super.processMouseMotionEvent(convertMouseEvent(e))
  }

  override fun processMouseEvent(e: MouseEvent) {
    if (e.getID() == MouseEvent.MOUSE_ENTERED || e.getID() == MouseEvent.MOUSE_EXITED) {
      super.processMouseEvent(e)
    } else {
      if (getCellBounds(0, getModel().getSize() - 1).contains(e.getPoint())) {
        super.processMouseEvent(convertMouseEvent(e))
      } else {
        e.consume()
        requestFocusInWindow()
      }
    }
  }

  private fun convertMouseEvent(e: MouseEvent): MouseEvent {
    return MouseEvent(
      e.getComponent(),
      e.getID(), e.getWhen(),
      e.getModifiersEx() or Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(),
      // Java 10: e.getModifiersEx() or Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
      e.getX(), e.getY(),
      e.getXOnScreen(), e.getYOnScreen(),
      e.getClickCount(),
      e.isPopupTrigger(),
      e.getButton()
    )
  }
}

class SingleClickSelectList<E>(model: ListModel<E>) : JList<E>(model) {
  @Transient
  private var listener: SelectionHandler? = null
  private var isDragging = false
  private var isCellInsideDragging = false
  private var startOutside = false
  private var startIndex = -1
  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    setForeground(null)
    setBackground(null)
    setSelectionForeground(null)
    setSelectionBackground(null)
    super.updateUI()
    listener = SelectionHandler()
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun setSelectionInterval(anchor: Int, lead: Int) {
    if (anchor == lead && lead >= 0) {
      if (isDragging) {
        addSelectionInterval(anchor, anchor)
      } else if (!isCellInsideDragging) {
        if (isSelectedIndex(anchor)) {
          removeSelectionInterval(anchor, anchor)
        } else {
          addSelectionInterval(anchor, anchor)
        }
        isCellInsideDragging = true
      }
    } else {
      super.setSelectionInterval(anchor, lead)
    }
  }

  private fun clearSelectionAndFocus() {
    getSelectionModel().also {
      it.clearSelection()
      it.setAnchorSelectionIndex(-1)
      it.setLeadSelectionIndex(-1)
    }
  }

  private fun cellsContains(pt: Point): Boolean {
    for (i in 0 until getModel().getSize()) {
      val r = getCellBounds(i, i)
      if (r.contains(pt)) {
        return true
      }
    }
    return false
  }

  private inner class SelectionHandler : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      startOutside = !cellsContains(e.point)
      startIndex = locationToIndex(e.point)
      if (startOutside) {
        clearSelectionAndFocus()
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      startOutside = false
      isDragging = false
      isCellInsideDragging = false
      startIndex = -1
    }

    override fun mouseDragged(e: MouseEvent) {
      if (!isDragging && startIndex == locationToIndex(e.point)) {
        isCellInsideDragging = true
      } else {
        isDragging = true
        isCellInsideDragging = false
      }
      if (cellsContains(e.point)) {
        startOutside = false
        isDragging = true
      } else if (startOutside) {
        clearSelectionAndFocus()
      }
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
