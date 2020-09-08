package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val model = DefaultListModel<String>()
  model.addElement("ABC DEF GHI JKL MNO PQR STU VW XYZ")
  model.addElement("aaa")
  model.addElement("aaa bbb")
  model.addElement("aaa bbb cc")
  model.addElement("1234567890abc def ghi jkl mno pqr stu vw xyz")
  model.addElement("bbb1")
  model.addElement("bbb12")
  model.addElement("1234567890-+*/=ABC DEF GHI JKL MNO PQR STU VW XYZ")
  model.addElement("bbb123")

  val list1 = object : TooltipList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer()
    }
  }

  val list2 = object : CellRendererTooltipList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer()
    }
  }

  val list3 = object : JList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      cellRenderer = TooltipListCellRenderer()
    }
  }

  return JPanel(GridLayout(1, 0)).also {
    mapOf(
      "CellBounds" to list1,
      "ListCellRenderer" to list2,
      "Default location" to list3
    ).forEach { (title, c) -> it.add(makeTitledPanel(title, c)) }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val scroll = JScrollPane(c)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(scroll)
  return p
}

private open class TooltipList<E>(m: ListModel<E>) : JList<E>(m) {
  override fun getToolTipLocation(e: MouseEvent): Point? {
    val p = e.point
    val r = cellRenderer
    val i = locationToIndex(p)
    val cellBounds = getCellBounds(i, i)
    if (i >= 0 && r != null && cellBounds?.contains(p.x, p.y) == true) {
      val lsm = selectionModel
      val hasFocus = hasFocus() && lsm.leadSelectionIndex == i
      val value = model.getElementAt(i)
      val renderer = r.getListCellRendererComponent(this, value, i, lsm.isSelectedIndex(i), hasFocus)
      if (renderer is JComponent && renderer.toolTipText != null) {
        return cellBounds.location
      }
    }
    return null
  }
}

private open class CellRendererTooltipList<E>(m: ListModel<E>) : JList<E>(m) {
  private val label = JLabel()
  override fun getToolTipLocation(e: MouseEvent): Point? {
    val p = e.point
    val i = locationToIndex(p)
    val r = cellRenderer
    val cellBounds = getCellBounds(i, i)
    if (i >= 0 && r != null && cellBounds?.contains(p.x, p.y) == true) {
      val lsm = selectionModel
      val str = model.getElementAt(i)
      val hasFocus = hasFocus() && lsm.leadSelectionIndex == i
      val renderer = r.getListCellRendererComponent(this, str, i, lsm.isSelectedIndex(i), hasFocus)
      if (renderer is JComponent && renderer.toolTipText != null) {
        val pt = cellBounds.location
        val ins = label.insets
        pt.translate(-ins.left, -ins.top)
        label.icon = RendererIcon(renderer, cellBounds)
        return pt
      }
    }
    return null
  }

  override fun createToolTip(): JToolTip {
    val tip = object : JToolTip() {
      override fun getPreferredSize(): Dimension {
        val i = insets
        val d = label.preferredSize
        return Dimension(d.width + i.left + i.right, d.height + i.top + i.bottom)
      }
    }
    tip.removeAll()
    tip.border = BorderFactory.createEmptyBorder()
    tip.layout = BorderLayout()
    tip.component = this
    tip.add(label)
    return tip
  }

  init {
    label.border = BorderFactory.createLineBorder(Color.GRAY, 1)
  }
}

private open class TooltipListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer: ListCellRenderer<in E> = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val l = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
    val i = l.insets
    val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, list)
    val rect = c.bounds
    rect.width -= i.left + i.right
    val fm = l.getFontMetrics(l.font)
    val str = value?.toString() ?: ""
    l.toolTipText = if (fm.stringWidth(str) > rect.width) str else null
    return l
  }
}

private class RendererIcon(private val renderer: Component, private val rect: Rectangle) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is Container) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(x, y)
      SwingUtilities.paintComponent(g2, renderer, c, rect)
      g2.dispose()
    }
  }

  override fun getIconWidth() = renderer.preferredSize.width

  override fun getIconHeight() = renderer.preferredSize.height

  init {
    rect.setLocation(0, 0)
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
