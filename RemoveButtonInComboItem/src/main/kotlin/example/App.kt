package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  val c0 = makeComboBox(isDefault = true, isEditable = false)
  val c1 = makeComboBox(isDefault = false, isEditable = false)
  val c2 = makeComboBox(isDefault = true, isEditable = true)
  val c3 = makeComboBox(isDefault = false, isEditable = true)

  val button = JButton("add")
  button.addActionListener {
    val str = LocalDateTime.now(ZoneId.systemDefault()).toString()
    listOf(c0, c1, c2, c3)
      // .map { it.getModel() }.filterIsInstance<MutableComboBoxModel<String>>()
      .mapNotNull { it.model as? MutableComboBoxModel<String> }
      .forEach { it.insertElementAt(str, it.size) }
  }

  val p = JPanel(GridLayout(2, 1))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(makeTitledPanel("setEditable(false)", listOf(c0, c1)))
  p.add(makeTitledPanel("setEditable(true)", listOf(c2, c3)))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox(isDefault: Boolean, isEditable: Boolean): JComboBox<String> {
  val m = DefaultComboBoxModel(arrayOf("aaa", "bbb", "ccc"))
  val comboBox = if (isDefault) JComboBox(m) else RemoveButtonComboBox(m)
  comboBox.isEditable = isEditable
  return comboBox
}

private fun makeTitledPanel(title: String, list: List<Component>): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  c.gridx = GridBagConstraints.REMAINDER
  list.forEach { cmp -> p.add(cmp, c) }
  return p
}

private class RemoveButtonComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  private var handler: CellButtonsMouseListener? = null

  private fun getList() = (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.list

  override fun updateUI() {
    if (handler != null) {
      getList()?.also {
        it.removeMouseListener(handler)
        it.removeMouseMotionListener(handler)
      }
    }
    super.updateUI()
    setRenderer(ButtonsRenderer(this))
    getList()?.also {
      handler = CellButtonsMouseListener()
      it.addMouseListener(handler)
      it.addMouseMotionListener(handler)
    }
  }
}

private class CellButtonsMouseListener : MouseAdapter() {
  override fun mouseMoved(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    val pt = e.point
    val idx = list.locationToIndex(pt)
    val r = list.cellRenderer
    if (r is ButtonsRenderer<*>) {
      r.rolloverIndex = getButton(list, pt, idx)?.let { idx } ?: -1
    }
    list.repaint()
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.repaint()
  }

  override fun mouseReleased(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    val pt = e.point
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      getButton(list, pt, index)?.doClick()
    }
    (list.cellRenderer as? ButtonsRenderer<*>)?.rolloverIndex = -1
    list.repaint()
  }

  override fun mouseExited(e: MouseEvent) {
    ((e.component as? JList<*>)?.cellRenderer as? ButtonsRenderer<*>)?.rolloverIndex = -1
  }

  private fun <E> getButton(list: JList<E>, pt: Point, index: Int): JButton? {
    val proto = list.prototypeCellValue
    val c = list.cellRenderer.getListCellRendererComponent(list, proto, index, false, false)
    val r = list.getCellBounds(index, index)
    c.bounds = r
    // c.doLayout() // may be needed for other layout managers (e.g. FlowLayout)
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? JButton
  }
}

private class ButtonsRenderer<E>(comboBox: RemoveButtonComboBox<E>) : ListCellRenderer<E> {
  private var targetIndex = 0
  var rolloverIndex = -1
  private val panel = object : JPanel(BorderLayout()) { // *1
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0
      return d
    }
  }
  private val renderer = DefaultListCellRenderer()
  private val deleteButton = object : JButton("x") {
    override fun getPreferredSize() = Dimension(16, 16)

    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      isFocusable = false
      isRolloverEnabled = false
      isContentAreaFilled = false
    }
  }

  init {
    deleteButton.addActionListener {
      val m = comboBox.model
      val oneOrMore = m.size > 1
      if (oneOrMore && m is MutableComboBoxModel<*>) {
        m.removeElementAt(targetIndex)
        comboBox.selectedIndex = -1
        comboBox.showPopup()
      }
    }
    panel.isOpaque = true
    panel.add(deleteButton, BorderLayout.EAST)
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    if (index < 0) {
      return c
    }
    (c as? JComponent)?.isOpaque = false
    this.targetIndex = index
    if (isSelected) {
      panel.setBackground(list.selectionBackground)
    } else {
      panel.setBackground(if (index % 2 == 0) EVEN_COLOR else list.background)
    }
    val showDeleteButton = list.model.size > 1
    deleteButton.isVisible = showDeleteButton
    if (showDeleteButton) {
      val isRollover = index == rolloverIndex
      deleteButton.model.isRollover = isRollover
      deleteButton.foreground = if (isRollover) Color.WHITE else list.foreground
    }
    panel.add(c)
    return panel
  }

  companion object {
    private val EVEN_COLOR = Color(0xE6_FF_E6)
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
