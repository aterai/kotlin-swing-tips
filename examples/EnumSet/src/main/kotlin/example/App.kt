package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.EnumMap
import java.util.EnumSet
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun createUI(): Component {
  val columnNames = arrayOf("user", "rwx")
  val data = arrayOf(
    arrayOf("owner", EnumSet.allOf(Permission::class.java)),
    arrayOf("group", EnumSet.of(Permission.READ)),
    arrayOf("other", EnumSet.noneOf(Permission::class.java)),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val c = getColumnModel().getColumn(1)
      c.cellRenderer = CheckBoxesRenderer()
      c.cellEditor = CheckBoxesEditor()
    }
  }
  table.putClientProperty("terminateEditOnFocusLost", true)
  val bitFlags = EnumMap<Permission, Int>(Permission::class.java)
  bitFlags[Permission.READ] = 1 shl 2
  bitFlags[Permission.WRITE] = 1 shl 1
  bitFlags[Permission.EXECUTE] = 1
  val label = JLabel()
  val button = JButton("ls -l (chmod)")
  button.addActionListener {
    val octalBuf = StringBuilder(3)
    val rwxBuf = StringBuilder(9)
    for (i in 0..<model.rowCount) {
      var bits = 0
      val v = model.getValueAt(i, 1) as? Set<*> ?: continue
      if (v.contains(Permission.READ)) {
        bits = bitFlags[Permission.READ] ?: 0
        rwxBuf.append('r')
      } else {
        rwxBuf.append('-')
      }
      if (v.contains(Permission.WRITE)) {
        bits = bits or (bitFlags[Permission.WRITE] ?: 0)
        rwxBuf.append('w')
      } else {
        rwxBuf.append('-')
      }
      if (v.contains(Permission.EXECUTE)) {
        bits = bits or (bitFlags[Permission.EXECUTE] ?: 0)
        rwxBuf.append('x')
      } else {
        rwxBuf.append('-')
      }
      octalBuf.append(bits)
    }
    label.text = " $octalBuf -$rwxBuf"
  }
  val p = JPanel(BorderLayout())
  p.add(label)
  p.add(button, BorderLayout.EAST)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class Permission {
  EXECUTE,
  WRITE,
  READ,
}

private class CheckBoxesPanel : JPanel() {
  val titles = arrayOf("r", "w", "x")
  private val checkBoxes = titles.map { createCheckBox(it) }
  private val alphaZero = Color(0x0, true)

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    background = alphaZero
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    EventQueue.invokeLater { initButtons() }
  }

  private fun initButtons() {
    removeAll()
    for (b in checkBoxes) {
      add(b)
      add(Box.createHorizontalStrut(5))
    }
  }

  fun setSelectedPermissions(v: Any?) {
    initButtons()
    val f = v as? Set<*> ?: EnumSet.noneOf(Permission::class.java)
    checkBoxes[0].isSelected = f.contains(Permission.READ)
    checkBoxes[1].isSelected = f.contains(Permission.WRITE)
    checkBoxes[2].isSelected = f.contains(Permission.EXECUTE)
  }

  fun doClickCheckBox(text: String) {
    checkBoxes.firstOrNull { it.text == text }?.doClick()
  }

  fun getPermissions(): Set<Permission> {
    val f = EnumSet.noneOf(Permission::class.java)
    if (checkBoxes[0].isSelected) {
      f.add(Permission.READ)
    }
    if (checkBoxes[1].isSelected) {
      f.add(Permission.WRITE)
    }
    if (checkBoxes[2].isSelected) {
      f.add(Permission.EXECUTE)
    }
    return f
  }

  private fun createCheckBox(title: String): JCheckBox {
    val b = JCheckBox(title)
    b.isOpaque = false
    b.isFocusable = false
    b.isRolloverEnabled = false
    b.background = alphaZero
    return b
  }
}

private class CheckBoxesRenderer : TableCellRenderer {
  private val panel = CheckBoxesPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    panel.setSelectedPermissions(value)
    return panel
  }
}

private class CheckBoxesEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val panel = CheckBoxesPanel()

  init {
    val am = panel.actionMap
    panel.titles.forEach {
      val a = object : AbstractAction(it) {
        override fun actionPerformed(e: ActionEvent) {
          panel.doClickCheckBox(it)
          fireEditingStopped()
        }
      }
      am.put(it, a)
    }
    val im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), panel.titles[0])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), panel.titles[1])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), panel.titles[2])
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    panel.setSelectedPermissions(value)
    return panel
  }

  override fun getCellEditorValue() = panel.getPermissions()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
