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
    val c0 = makeComboBox(true, false)
    val c1 = makeComboBox(false, false)
    val c2 = makeComboBox(true, true)
    val c3 = makeComboBox(false, true)

    val button = JButton("add")
    button.addActionListener {
      val str = LocalDateTime.now(ZoneId.systemDefault()).toString()
      listOf(c0, c1, c2, c3).forEach {
        (it.getModel() as? MutableComboBoxModel<String>)?.also {
          it.insertElementAt(str, it.getSize())
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
    val m = DefaultComboBoxModel<String>(arrayOf("aaa", "bbb", "ccc"))
    val comboBox: JComboBox<String>
    if (isDefault) {
      comboBox = JComboBox<String>(m)
    } else {
      comboBox = RemoveButtonComboBox<String>(m)
    }
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

internal class RemoveButtonComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  @Transient
  private var cbml: CellButtonsMouseListener? = null

  protected fun getList() =
      (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.getList()

  override fun updateUI() {
    if (cbml != null) {
      getList()?.also {
        it.removeMouseListener(cbml)
        it.removeMouseMotionListener(cbml)
      }
    }
    super.updateUI()
    setRenderer(ButtonsRenderer(this))
    getList()?.also {
      cbml = CellButtonsMouseListener()
      it.addMouseListener(cbml)
      it.addMouseMotionListener(cbml)
    }
  }
}

internal class CellButtonsMouseListener : MouseAdapter() {
  private var prevIndex = -1
  private var prevButton: JButton? = null

  private fun rectRepaint(c: JComponent, rect: Rectangle?) {
    rect?.also { c.repaint(it) }
  }

  override fun mouseMoved(e: MouseEvent) {
    val list = e.getComponent() as JList<*>
    val pt = e.getPoint()
    var index = list.locationToIndex(pt)
    if (!list.getCellBounds(index, index).contains(pt)) {
      if (prevIndex >= 0) {
        val r = list.getCellBounds(prevIndex, prevIndex)
        rectRepaint(list, r)
      }
      // index = -1
      prevButton = null
      return
    }
    if (index >= 0) {
      val button = getButton(list, pt, index)
      val renderer = list.getCellRenderer() as ButtonsRenderer<*>
      if (button != null) {
        renderer.rolloverIndex = index
        if (button != prevButton) {
          val r = list.getCellBounds(prevIndex, index)
          rectRepaint(list, r)
        }
      } else {
        renderer.rolloverIndex = -1
        var r: Rectangle? = null
        if (prevIndex == index) {
          if (prevIndex >= 0 && prevButton != null) {
            r = list.getCellBounds(prevIndex, prevIndex)
          }
        } else {
          r = list.getCellBounds(index, index)
        }
        rectRepaint(list, r)
        prevIndex = -1
      }
      prevButton = button
    }
    prevIndex = index
  }

  override fun mousePressed(e: MouseEvent) {
    val list = e.getComponent() as JList<*>
    val pt = e.getPoint()
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      if (getButton(list, pt, index) != null) {
        rectRepaint(list, list.getCellBounds(index, index))
      }
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val list = e.getComponent() as JList<*>
    val pt = e.getPoint()
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      getButton(list, pt, index)?.also {
        it.doClick()
        rectRepaint(list, list.getCellBounds(index, index))
      }
    }
  }

  override fun mouseExited(e: MouseEvent) {
    val list = e.getComponent() as JList<*>
    (list.getCellRenderer() as ButtonsRenderer<*>).rolloverIndex = -1
  }

  private fun <E> getButton(list: JList<E>, pt: Point, index: Int): JButton? {
    val proto = list.getPrototypeCellValue()
    val c = list.getCellRenderer().getListCellRendererComponent(list, proto, index, false, false)
    val r = list.getCellBounds(index, index)
    c.setBounds(r)
    // c.doLayout(); // may be needed for other layout managers (eg. FlowLayout) // *1
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? JButton
  }
}

internal class ButtonsRenderer<E>(comboBox: RemoveButtonComboBox<E>) : ListCellRenderer<E> {
  var targetIndex: Int = 0
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
    override fun getPreferredSize(): Dimension {
      return Dimension(16, 16)
    }

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
    val l = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
    if (index < 0) {
      return l
    }
    l.setOpaque(false)
    this.targetIndex = index
    if (isSelected) {
      panel.setBackground(list.getSelectionBackground())
    } else {
      panel.setBackground(if (index % 2 == 0) EVEN_COLOR else list.getBackground())
    }
    val showDeleteButton = list.getModel().getSize() > 1
    if (showDeleteButton) {
      val isRollover = index == rolloverIndex
      deleteButton.setVisible(true)
      deleteButton.getModel().setRollover(isRollover)
      deleteButton.setForeground(if (isRollover) Color.WHITE else list.getForeground())
    } else {
      deleteButton.setVisible(false)
    }
    panel.add(l)
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
