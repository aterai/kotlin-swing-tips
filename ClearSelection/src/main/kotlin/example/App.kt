package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    add(makeTitledPanel("Default", JScrollPane(JList(makeModel()))))
    add(makeTitledPanel("clearSelection", JScrollPane(makeList(makeModel()))))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel() = DefaultListModel<String>().also {
    it.addElement("000000000")
    it.addElement("111111111")
    it.addElement("2222222222")
    it.addElement("33333")
    it.addElement("44444444444")
  }

  private fun <E> makeList(model: ListModel<E>): JList<E> {
    return object : JList<E>(model) {
      @Transient
      private var listener: MouseInputListener? = null

      override fun updateUI() {
        removeMouseListener(listener)
        removeMouseMotionListener(listener)
        setForeground(null)
        setBackground(null)
        setSelectionForeground(null)
        setSelectionBackground(null)
        super.updateUI()
        listener = ClearSelectionListener()
        addMouseListener(listener)
        addMouseMotionListener(listener)
      }
    }
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }
}

class ClearSelectionListener : MouseInputAdapter() {
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
    if (contains(list, e.getPoint())) {
      startOutside = false
    } else if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  private fun clearSelectionAndFocus(list: JList<*>) {
    list.clearSelection()
    list.getSelectionModel().also {
      it.setAnchorSelectionIndex(-1)
      it.setLeadSelectionIndex(-1)
    }
  }

  private fun contains(list: JList<*>, pt: Point): Boolean {
    for (i in 0 until list.getModel().getSize()) {
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
