package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI() = JPanel(GridLayout(1, 3)).also {
  it.add(makeTitledPanel("Default", JList(makeModel())))
  it.add(makeTitledPanel("MouseEvent", SingleMouseClickSelectList(makeModel())))
  it.add(makeTitledPanel("SelectionInterval", SingleClickSelectList(makeModel())))
  it.preferredSize = Dimension(320, 240)
}

private fun makeModel() = DefaultListModel<String>().also {
  it.addElement("111111111")
  it.addElement("22222222222222")
  it.addElement("333333333")
  it.addElement("44444444")
  it.addElement("5555555555")
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(JScrollPane(c))
}

private class SingleMouseClickSelectList<E>(model: ListModel<E>) : JList<E>(model) {
  override fun updateUI() {
    foreground = null
    background = null
    selectionForeground = null
    selectionBackground = null
    super.updateUI()
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    super.processMouseMotionEvent(convertMouseEvent(e))
  }

  override fun processMouseEvent(e: MouseEvent) {
    if (e.id == MouseEvent.MOUSE_ENTERED || e.id == MouseEvent.MOUSE_EXITED) {
      super.processMouseEvent(e)
    } else {
      if (getCellBounds(0, model.size - 1).contains(e.point)) {
        super.processMouseEvent(convertMouseEvent(e))
      } else {
        e.consume()
        requestFocusInWindow()
      }
    }
  }

  private fun convertMouseEvent(e: MouseEvent): MouseEvent {
    return MouseEvent(
      e.component,
      e.id, e.getWhen(),
      e.modifiersEx or Toolkit.getDefaultToolkit().menuShortcutKeyMask,
      // Java 10: e.getModifiersEx() or Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
      e.x, e.y,
      e.xOnScreen, e.yOnScreen,
      e.clickCount,
      e.isPopupTrigger,
      e.button,
    )
  }
}

private class SingleClickSelectList<E>(model: ListModel<E>) : JList<E>(model) {
  private var listener: SelectionHandler? = null
  private var isDragging = false
  private var isInsideDragging = false
  private var startOutside = false
  private var startIndex = -1

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    foreground = null
    background = null
    selectionForeground = null
    selectionBackground = null
    super.updateUI()
    listener = SelectionHandler()
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun setSelectionInterval(anchor: Int, lead: Int) {
    if (anchor == lead && lead >= 0) {
      if (isDragging) {
        addSelectionInterval(anchor, anchor)
      } else if (!isInsideDragging) {
        if (isSelectedIndex(anchor)) {
          removeSelectionInterval(anchor, anchor)
        } else {
          addSelectionInterval(anchor, anchor)
        }
        isInsideDragging = true
      }
    } else {
      super.setSelectionInterval(anchor, lead)
    }
  }

  private fun clearSelectionAndFocus() {
    selectionModel.also {
      it.clearSelection()
      it.anchorSelectionIndex = -1
      it.leadSelectionIndex = -1
    }
  }

  private fun cellsContains(pt: Point): Boolean {
    for (i in 0 until model.size) {
      if (getCellBounds(i, i).contains(pt)) {
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
      isInsideDragging = false
      startIndex = -1
    }

    override fun mouseDragged(e: MouseEvent) {
      if (!isDragging && startIndex == locationToIndex(e.point)) {
        isInsideDragging = true
      } else {
        isDragging = true
        isInsideDragging = false
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
