package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer

private val model = object : DefaultTableModel() {
  val columns = arrayOf(
    ColumnContext("Focus", String::class.java, false),
    ColumnContext("ActionName", String::class.java, false),
    ColumnContext("KeyDescription", String::class.java, false),
  )

  fun addBinding(t: Binding) {
    super.addRow(arrayOf(t.focusTypeName, t.actionName, t.keyDescription))
  }

  override fun isCellEditable(row: Int, col: Int) = columns[col].isEditable

  override fun getColumnClass(column: Int) = columns[column].columnClass

  override fun getColumnCount() = columns.size

  override fun getColumnName(column: Int) = columns[column].columnName
}

private data class ColumnContext(
  val columnName: String,
  val columnClass: Class<*>,
  val isEditable: Boolean,
)

private val components = arrayOf<JComponent>(
  JComboBox<Any>(),
  JDesktopPane(),
  JFormattedTextField(),
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
  JTextField(),
)
private val componentChoices = JComboBox(components)

private fun loadBindingMap(
  focusType: FocusType,
  im: InputMap,
  am: ActionMap,
) {
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
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
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
  val renderer = DefaultListCellRenderer()
  componentChoices.setRenderer { list, value, index, isSelected, hasFocus ->
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, hasFocus)
    (c as? JLabel)?.text = value.javaClass.name
    c
  }
  componentChoices.addItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      model.rowCount = 0
      val c = componentChoices.getItemAt(componentChoices.selectedIndex)
      for (f in FocusType.values()) {
        loadBindingMap(f, c.getInputMap(f.id), c.actionMap)
      }
    }
  }
  SwingUtilities.invokeLater {
    componentChoices.selectedIndex = componentChoices.itemCount - 1
  }
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(componentChoices)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private data class Binding(
  private val focusType: FocusType,
  val actionName: String,
  val keyDescription: String,
) {
  val focusTypeName get() = focusType.name
}

private enum class FocusType(val id: Int) {
  WHEN_FOCUSED(JComponent.WHEN_FOCUSED),
  WHEN_IN_FOCUSED_WINDOW(JComponent.WHEN_IN_FOCUSED_WINDOW),
  WHEN_ANCESTOR_OF_FOCUSED_COMPONENT(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT),
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
