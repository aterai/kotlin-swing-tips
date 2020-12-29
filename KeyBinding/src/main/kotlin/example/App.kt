package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer

private val model = BindingMapModel()
private val components = arrayOf<JComponent>(
  JComboBox<Any>(),
  JDesktopPane(),
  JFormattedTextField(), // new JFileChooser(),
  JInternalFrame(),
  JLabel(),
  JLayeredPane(),
  JList<Any>(),
  JMenuBar(),
  JOptionPane(),
  JPanel(),
  JPopupMenu(),
  JProgressBar(),
  JRootPane(),
  JScrollBar(),
  JScrollPane(),
  JSeparator(),
  JSlider(),
  JSpinner(),
  JSplitPane(),
  JTabbedPane(),
  JTable(),
  JTableHeader(),
  JToolBar(),
  JToolTip(),
  JTree(),
  JEditorPane(),
  JTextArea(),
  JTextField()
)
private val componentChoices = JComboBox(components)
private val focusTypes = listOf(
  JComponent.WHEN_FOCUSED,
  JComponent.WHEN_IN_FOCUSED_WINDOW,
  JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
)

private fun loadBindingMap(focusType: Int, im: InputMap, am: ActionMap) {
  if (im.allKeys() == null) {
    return
  }
  val tmpAm = ActionMap()
  for (actionMapKey in am.allKeys()) {
    tmpAm.put(actionMapKey, am[actionMapKey])
  }
  for (ks in im.allKeys()) {
    val actionMapKey = im[ks]
    val action = am[actionMapKey]
    if (action == null) {
      model.addBinding(Binding(focusType, "____$actionMapKey", ks.toString()))
    } else {
      model.addBinding(Binding(focusType, actionMapKey.toString(), ks.toString()))
    }
    tmpAm.remove(actionMapKey)
  }
  if (tmpAm.allKeys() == null) {
    return
  }
  for (actionMapKey in tmpAm.allKeys()) {
    model.addBinding(Binding(focusType, actionMapKey.toString(), ""))
  }
}

fun makeUI(): Component {
  val table = object : JTable(model) {
    private val evenColor = Color(0xFAFAFA)
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isRowSelected(row)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }
  }
  table.autoCreateRowSorter = true
  val renderer = JLabel()
  componentChoices.setRenderer { list, value, index, isSelected, _ ->
    renderer.isOpaque = index >= 0
    renderer.text = value.javaClass.name
    if (isSelected) {
      renderer.background = list.selectionBackground
      renderer.foreground = list.selectionForeground
    } else {
      renderer.background = list.background
      renderer.foreground = list.foreground
    }
    renderer
  }
  val button = JButton("show")
  button.addActionListener {
    model.rowCount = 0
    val c = componentChoices.getItemAt(componentChoices.selectedIndex)
    for (f in focusTypes) {
      loadBindingMap(f, c.getInputMap(f), c.actionMap)
    }
  }
  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(componentChoices)
  p.add(button)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class BindingMapModel : DefaultTableModel() {
  fun addBinding(t: Binding) {
    super.addRow(arrayOf(t.focusTypeName, t.actionName, t.keyDescription))
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(
    val columnName: String,
    val columnClass: Class<*>,
    val isEditable: Boolean
  )

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("Focus", String::class.java, false),
      ColumnContext("ActionName", String::class.java, false),
      ColumnContext("KeyDescription", String::class.java, false)
    )
  }
}

private data class Binding(private val focusType: Int, val actionName: String, val keyDescription: String) {
  val focusTypeName get() = when (focusType) {
    JComponent.WHEN_FOCUSED -> "WHEN_FOCUSED"
    JComponent.WHEN_IN_FOCUSED_WINDOW -> "WHEN_IN_FOCUSED_WINDOW"
    else -> "WHEN_ANCESTOR_OF_FOCUSED_COMPONENT"
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
