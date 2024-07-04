package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val model = DefaultListModel<String>()
  for (i in 0..7) {
    model.addElement("$i: 11111")
    model.addElement("$i: 22222")
    model.addElement("$i: 33333")
    model.addElement("$i: 44444")
    model.addElement("$i: 55555")
    model.addElement("$i: 66666")
    model.addElement("$i: 77777")
    model.addElement("$i: 88888")
    model.addElement("$i: 99999")
    model.addElement("$i: 00000")
  }

  val list0 = object : JList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer<Any>()
    }
  }

  val list1 = object : JList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer<Any>()
    }
  }
  val scroll1 = JScrollPane(list1)
  scroll1.addMouseWheelListener { e ->
    val event = SwingUtilities.convertMouseEvent(e.component, e, list1)
    ToolTipManager.sharedInstance().mouseMoved(event)
  }

  val list2 = object : TooltipList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer<Any>()
    }
  }

  return JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("Default", JScrollPane(list0)))
    it.add(makeTitledPanel("MouseWheelListener", scroll1))
    it.add(makeTitledPanel("getToolTipLocation", JScrollPane(list2)))
    it.preferredSize = Dimension(320, 240)
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

private open class TooltipList<E>(m: ListModel<E>?) : JList<E>(m) {
  override fun getToolTipText(e: MouseEvent): String {
    val event = getToolTipCellPoint(e)?.let {
      MouseEvent(
        e.component,
        MouseEvent.MOUSE_MOVED,
        e.getWhen(),
        e.modifiersEx or e.modifiers,
        it.x,
        it.y,
        e.clickCount,
        e.isPopupTrigger,
      )
    } ?: e
    return super.getToolTipText(event)
  }

  override fun getToolTipLocation(e: MouseEvent) = getToolTipCellPoint(e)?.let {
    val i = locationToIndex(it)
    Point(it.x, it.y + getCellBounds(i, i).height)
  }

  private fun getToolTipCellPoint(e: MouseEvent) = mousePosition
    ?.takeIf { it != e.point }
    ?.takeIf { p ->
      val i = locationToIndex(p)
      val cellBounds = getCellBounds(i, i)
      cellBounds != null && cellBounds.contains(p)
    }
}

private class TooltipListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer: ListCellRenderer<in E> = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JComponent && value != null) {
      c.toolTipText = value.toString()
    }
    return c
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
