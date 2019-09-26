package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.ComboPopup

class MainPanel : JPanel(BorderLayout()) {
  init {
    val c0 = makeComboBox(isDefault = true, isEditable = false)
    val c1 = makeComboBox(isDefault = false, isEditable = false)
    val c2 = makeComboBox(isDefault = true, isEditable = true)
    val c3 = makeComboBox(isDefault = false, isEditable = true)

    val button = JButton("add")
    button.addActionListener {
      val str = LocalDateTime.now(ZoneId.systemDefault()).toString()
      listOf(c0, c1, c2, c3).forEach { c ->
        (c.getModel() as? MutableComboBoxModel<String>)?.also { m ->
          m.insertElementAt(str, m.getSize())
        }
      }
    }

    val p = JPanel(GridLayout(2, 1))
    p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    p.add(makeTitledPanel("setEditable(false)", listOf(c0, c1)))
    p.add(makeTitledPanel("setEditable(true)", listOf(c2, c3)))

    add(p, BorderLayout.NORTH)
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeComboBox(isDefault: Boolean, isEditable: Boolean): JComboBox<String> {
    val m = DefaultComboBoxModel(arrayOf("aaa", "bbb", "ccc"))
    val comboBox = if (isDefault) JComboBox(m) else RemoveButtonComboBox(m)
    comboBox.setEditable(isEditable)
    return comboBox
  }

  private fun makeTitledPanel(title: String, list: List<Component>): Component {
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    c.weightx = 1.0
    c.gridx = GridBagConstraints.REMAINDER
    list.forEach { cmp -> p.add(cmp, c) }
    return p
  }
}

class RemoveButtonComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  @Transient
  private var handler: CellButtonsMouseListener? = null

  private fun getList() = (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.getList()

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

class CellButtonsMouseListener : MouseAdapter() {
  override fun mouseMoved(e: MouseEvent) {
    val list = e.getComponent() as? JList<*> ?: return
    val pt = e.getPoint()
    val idx = list.locationToIndex(pt)
    (list.getCellRenderer() as? ButtonsRenderer<*>)?.rolloverIndex = getButton(list, pt, idx)?.let { idx } ?: -1
    list.repaint()
  }

  override fun mousePressed(e: MouseEvent) {
    e.getComponent().repaint()
  }

  override fun mouseReleased(e: MouseEvent) {
    val list = e.getComponent() as? JList<*> ?: return
    val pt = e.getPoint()
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      getButton(list, pt, index)?.doClick()
    }
    (list.getCellRenderer() as? ButtonsRenderer<*>)?.rolloverIndex = -1
    list.repaint()
  }

  override fun mouseExited(e: MouseEvent) {
    val list = e.getComponent() as? JList<*> ?: return
    (list.getCellRenderer() as? ButtonsRenderer<*>)?.rolloverIndex = -1
  }

  private fun <E> getButton(list: JList<E>, pt: Point, index: Int): JButton? {
    val proto = list.getPrototypeCellValue()
    val c = list.getCellRenderer().getListCellRendererComponent(list, proto, index, false, false)
    val r = list.getCellBounds(index, index)
    c.setBounds(r)
    // c.doLayout(); // may be needed for other layout managers (eg. FlowLayout)
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? JButton
  }
}

class ButtonsRenderer<E>(comboBox: RemoveButtonComboBox<E>) : ListCellRenderer<E> {
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
      setBorder(BorderFactory.createEmptyBorder())
      setFocusable(false)
      setRolloverEnabled(false)
      setContentAreaFilled(false)
    }
  }

  init {
    deleteButton.addActionListener {
      val m = comboBox.getModel()
      val isMoreThanOneItem = m.getSize() > 1
      if (isMoreThanOneItem && m is MutableComboBoxModel<*>) {
        m.removeElementAt(targetIndex)
        comboBox.setSelectedIndex(-1)
        comboBox.showPopup()
      }
    }
    panel.setOpaque(true)
    panel.add(deleteButton, BorderLayout.EAST)
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    if (index < 0) {
      return c
    }
    (c as? JComponent)?.setOpaque(false)
    this.targetIndex = index
    if (isSelected) {
      panel.setBackground(list.getSelectionBackground())
    } else {
      panel.setBackground(if (index % 2 == 0) EVEN_COLOR else list.getBackground())
    }
    val showDeleteButton = list.getModel().getSize() > 1
    deleteButton.setVisible(showDeleteButton)
    if (showDeleteButton) {
      val isRollover = index == rolloverIndex
      deleteButton.getModel().setRollover(isRollover)
      deleteButton.setForeground(if (isRollover) Color.WHITE else list.getForeground())
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
