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

fun makeUI(): Component {
  val columnNames = arrayOf("user", "rwx")
  val data = arrayOf(
    arrayOf("owner", EnumSet.allOf(Permissions::class.java)),
    arrayOf("group", EnumSet.of(Permissions.READ)),
    arrayOf("other", EnumSet.noneOf(Permissions::class.java)),
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
  val map = EnumMap<Permissions, Int>(Permissions::class.java)
  map[Permissions.READ] = 1 shl 2
  map[Permissions.WRITE] = 1 shl 1
  map[Permissions.EXECUTE] = 1
  val label = JLabel()
  val button = JButton("ls -l (chmod)")
  button.addActionListener {
    val numBuf = StringBuilder(3)
    val buf = StringBuilder(9)
    for (i in 0..<model.rowCount) {
      var flg = 0
      val v = model.getValueAt(i, 1) as? Set<*> ?: continue
      if (v.contains(Permissions.READ)) {
        flg = map[Permissions.READ] ?: 0
        buf.append('r')
      } else {
        buf.append('-')
      }
      if (v.contains(Permissions.WRITE)) {
        flg = flg or (map[Permissions.WRITE] ?: 0)
        buf.append('w')
      } else {
        buf.append('-')
      }
      if (v.contains(Permissions.EXECUTE)) {
        flg = flg or (map[Permissions.EXECUTE] ?: 0)
        buf.append('x')
      } else {
        buf.append('-')
      }
      numBuf.append(flg)
    }
    label.text = " $numBuf -$buf"
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

private enum class Permissions {
  EXECUTE,
  WRITE,
  READ,
}

private class CheckBoxesPanel : JPanel() {
  val titles = arrayOf("r", "w", "x")
  private val buttons = titles.map { makeCheckBox(it) }
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
    for (b in buttons) {
      add(b)
      add(Box.createHorizontalStrut(5))
    }
  }

  fun updateButtons(v: Any?) {
    initButtons()
    val f = v as? Set<*> ?: EnumSet.noneOf(Permissions::class.java)
    buttons[0].isSelected = f.contains(Permissions.READ)
    buttons[1].isSelected = f.contains(Permissions.WRITE)
    buttons[2].isSelected = f.contains(Permissions.EXECUTE)
  }

  fun doClickCheckBox(title: String) {
    buttons.firstOrNull { it.text == title }?.doClick()
  }

  fun getPermissionsValue(): Set<Permissions> {
    val f = EnumSet.noneOf(Permissions::class.java)
    if (buttons[0].isSelected) {
      f.add(Permissions.READ)
    }
    if (buttons[1].isSelected) {
      f.add(Permissions.WRITE)
    }
    if (buttons[2].isSelected) {
      f.add(Permissions.EXECUTE)
    }
    return f
  }

  private fun makeCheckBox(title: String): JCheckBox {
    val b = JCheckBox(title)
    b.isOpaque = false
    b.isFocusable = false
    b.isRolloverEnabled = false
    b.background = alphaZero
    return b
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
    renderer.updateButtons(value)
    return renderer
  }
}

private class CheckBoxesEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = CheckBoxesPanel()

  init {
    val am = renderer.actionMap
    renderer.titles.forEach {
      val a = object : AbstractAction(it) {
        override fun actionPerformed(e: ActionEvent) {
          renderer.doClickCheckBox(it)
          fireEditingStopped()
        }
      }
      am.put(it, a)
    }
    val im = renderer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), renderer.titles[0])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), renderer.titles[1])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), renderer.titles[2])
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    renderer.updateButtons(value)
    return renderer
  }

  override fun getCellEditorValue() = renderer.getPermissionsValue()
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
