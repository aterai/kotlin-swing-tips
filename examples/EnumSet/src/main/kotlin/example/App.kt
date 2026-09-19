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
import javax.swing.table.TableModel

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
  val label = JLabel()
  val button = JButton("ls -l (chmod)")
  button.addActionListener { label.text = createPermissionsText(model) }

  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val c = getColumnModel().getColumn(1)
      c.cellRenderer = CheckBoxesRenderer()
      c.cellEditor = CheckBoxesEditor()
      putClientProperty("terminateEditOnFocusLost", true)
    }
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

// e.g. " 740 -rwxr-----"
private fun createPermissionsText(model: TableModel): String {
  val octalBuf = StringBuilder(3)
  val rwxBuf = StringBuilder(9)
  for (i in 0..<model.rowCount) {
    val permissions = model.getValueAt(i, 1) as? Set<*> ?: continue
    var mode = 0
    Permission.entries.forEach { perm ->
      val granted = permissions.contains(perm)
      mode = mode or if (granted) perm.mode else 0
      rwxBuf.append(if (granted) perm.symbol else '-')
    }
    octalBuf.append(mode)
  }
  return " $octalBuf -$rwxBuf"
}

// Declared in "rwx" display order
private enum class Permission(
  val symbol: Char,
  val mode: Int,
) {
  READ('r', 1 shl 2),
  WRITE('w', 1 shl 1),
  EXECUTE('x', 1),
}

private class CheckBoxesPanel : JPanel() {
  private val checkBoxes = Permission.entries
    .associateWithTo(EnumMap(Permission::class.java)) { createCheckBox(it) }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    background = TRANSPARENT
    layout = BoxLayout(this, BoxLayout.X_AXIS)
  }

  // Re-add the check boxes on every update to avoid ghost images on Windows Aero
  private fun initCheckBoxes() {
    removeAll()
    checkBoxes.values.forEach {
      add(it)
      add(Box.createHorizontalStrut(5))
    }
  }

  fun updateCheckBoxes(value: Any?) {
    initCheckBoxes()
    val permissions = value as? Set<*> ?: emptySet<Permission>()
    checkBoxes.forEach { (perm, b) -> b.isSelected = permissions.contains(perm) }
  }

  fun toggleCheckBox(perm: Permission) {
    checkBoxes[perm]?.doClick()
  }

  fun getPermissions(): Set<Permission> = checkBoxes.entries
    .filter { it.value.isSelected }
    .mapTo(EnumSet.noneOf(Permission::class.java)) { it.key }

  companion object {
    private val TRANSPARENT = Color(0x0, true)

    private fun createCheckBox(perm: Permission) = JCheckBox(
      perm.symbol.toString(),
    ).also {
      it.isOpaque = false
      it.isFocusable = false
      it.isRolloverEnabled = false
      it.background = TRANSPARENT
    }
  }
}

private class CheckBoxesRenderer : TableCellRenderer {
  private val renderer = CheckBoxesPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    renderer.updateCheckBoxes(value)
    return renderer
  }
}

private class CheckBoxesEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val editor = CheckBoxesPanel()

  init {
    val am = editor.actionMap
    val im = editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    Permission.entries.forEach { perm ->
      val key = perm.name
      am.put(key, createToggleAction(perm))
      // 'r' -> KeyEvent.VK_R, 'w' -> KeyEvent.VK_W, 'x' -> KeyEvent.VK_X
      val keyCode = KeyEvent.getExtendedKeyCodeForChar(perm.symbol.code)
      im.put(KeyStroke.getKeyStroke(keyCode, 0), key)
    }
  }

  private fun createToggleAction(perm: Permission) = object : AbstractAction(perm.name) {
    override fun actionPerformed(e: ActionEvent) {
      editor.toggleCheckBox(perm)
      fireEditingStopped()
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    editor.updateCheckBoxes(value)
    return editor
  }

  override fun getCellEditorValue() = editor.getPermissions()
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
