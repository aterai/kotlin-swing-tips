package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(LinkCellList(makeModel())))
  it.preferredSize = Dimension(320, 240)
}

private fun makeModel(): ListModel<String> {
  val model = DefaultListModel<String>()
  listOf("aa", "bbb bbb bbb bb bb", "ccc", "ddd ddd ddd", "eee eee")
    .forEach { model.addElement(it) }
  return model
}

private class LinkCellList<E>(
  model: ListModel<E>,
) : JList<E>(model) {
  private var prevIndex = -1

  override fun updateUI() {
    foreground = null
    background = null
    selectionForeground = null
    selectionBackground = null
    super.updateUI()
    fixedCellHeight = 32
    cellRenderer = LinkCellRenderer()
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    val pt = e.point
    val i = locationToIndex(pt)
    val s = model.getElementAt(i)
    val c = cellRenderer.getListCellRendererComponent(this, s, i, false, false)
    val r = getCellBounds(i, i)
    c.bounds = r
    if (prevIndex != i) {
      c.doLayout()
    }
    prevIndex = i
    pt.translate(-r.x, -r.y)
    val child = SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y)
    cursor = child?.cursor ?: Cursor.getDefaultCursor()
  }
}

private class LinkCellRenderer<E> : ListCellRenderer<E> {
  private val panel = JPanel(FlowLayout(FlowLayout.LEFT))
  private val check = object : JCheckBox("check") {
    override fun updateUI() {
      super.updateUI()
      cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
      isOpaque = false
    }
  }
  private val button = object : JButton("button") {
    override fun updateUI() {
      super.updateUI()
      cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
    }
  }
  private val label = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    panel.removeAll()
    panel.add(label)
    panel.add(check)
    panel.add(button)
    panel.isOpaque = true
    if (isSelected) {
      panel.background = list.selectionBackground
      panel.foreground = list.selectionForeground
    } else {
      panel.background = list.background
      panel.foreground = list.foreground
    }
    label.text = "<html><a href='#'>$value"
    return panel
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
