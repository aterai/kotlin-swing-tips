package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI(): Component {
  val p = JPanel(GridLayout(1, 2))
  p.add(makeTitledPanel("Default", JScrollPane(JList(makeModel()))))
  p.add(makeTitledPanel("clearSelection", JScrollPane(makeList(makeModel()))))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeModel(): ListModel<String> {
  val m = DefaultListModel<String>()
  m.addElement("000000000")
  m.addElement("111111111")
  m.addElement("2222222222")
  m.addElement("33333")
  m.addElement("44444444444")
  return m
}

private fun <E> makeList(model: ListModel<E>) = object : JList<E>(model) {
  private var listener: MouseInputListener? = null

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    foreground = null
    background = null
    selectionForeground = null
    selectionBackground = null
    super.updateUI()
    listener = ClearSelectionListener()
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class ClearSelectionListener : MouseInputAdapter() {
  private var startOutside = false

  override fun mousePressed(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    startOutside = !contains(list, e.point)
    if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  override fun mouseReleased(e: MouseEvent?) {
    startOutside = false
  }

  override fun mouseDragged(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    if (contains(list, e.point)) {
      startOutside = false
    } else if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  private fun clearSelectionAndFocus(list: JList<*>) {
    list.clearSelection()
    list.selectionModel.also {
      it.anchorSelectionIndex = -1
      it.leadSelectionIndex = -1
    }
  }

  private fun contains(
    list: JList<*>,
    pt: Point,
  ): Boolean {
    for (i in 0..<list.model.size) {
      if (list.getCellBounds(i, i).contains(pt)) {
        return true
      }
    }
    return false
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
