package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val model = makeModel()
  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isCellSelected(row, column)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }
  }
  table.cellSelectionEnabled = true

  val actionMapKey = "clear-selection"
  val action = object : AbstractAction(actionMapKey) {
    override fun actionPerformed(e: ActionEvent) {
      table.clearSelection()
      table.requestFocusInWindow()
    }
  }
  table.actionMap.put(actionMapKey, action)

  val im = table.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionMapKey)

  val rowField = SpinnerNumberModel(1, 0, model.rowCount - 1, 1)
  val colField = SpinnerNumberModel(2, 0, model.columnCount - 1, 1)
  val toggle = JCheckBox("toggle", false)
  val extend = JCheckBox("extend", false)

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(JLabel("row:"))
  box.add(JSpinner(rowField))
  box.add(JLabel(" col:"))
  box.add(JSpinner(colField))
  box.add(toggle)
  box.add(extend)

  val changeSelection = JButton("changeSelection")
  changeSelection.addActionListener {
    val row = rowField.number.toInt()
    val col = colField.number.toInt()
    table.changeSelection(row, col, toggle.isSelected, extend.isSelected)
    table.requestFocusInWindow()
    table.repaint()
  }

  val clear = JButton("clear(Esc)")
  clear.addActionListener {
    table.clearSelection()
    table.requestFocusInWindow()
  }

  val p = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 5))
  p.add(changeSelection)
  p.add(clear)

  val title = "JTable#changeSelection(int, int, boolean, boolean)"
  val panel = JPanel(BorderLayout())
  panel.border = BorderFactory.createTitledBorder(title)
  panel.add(box, BorderLayout.NORTH)
  panel.add(p, BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("0, 0", "0, 1", "0, 2"),
    arrayOf("1, 0", "1, 1", "1, 2"),
    arrayOf("2, 0", "2, 1", "2, 2"),
    arrayOf("3, 0", "3, 1", "3, 2"),
    arrayOf("4, 0", "4, 1", "4, 2"),
    arrayOf("5, 0", "5, 1", "5, 2"),
    arrayOf("6, 0", "6, 1", "6, 2"),
    arrayOf("7, 0", "7, 1", "7, 2"),
    arrayOf("8, 0", "8, 1", "8, 2"),
    arrayOf("9, 0", "9, 1", "9, 2"),
  )
  return DefaultTableModel(data, columnNames)
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
