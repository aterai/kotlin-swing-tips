package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p = JPanel(GridLayout(2, 1))
    val h = TableRowTransferHandler()
    p.border = BorderFactory.createTitledBorder("Drag & Drop JTable")
    p.add(JScrollPane(makeDnDTable(h)))
    p.add(JScrollPane(makeDnDTable(h)))
    add(p)
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    preferredSize = Dimension(320, 240)
  }

  private fun makeDnDTable(handler: TableRowTransferHandler): JTable {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
      arrayOf("AAA", 12, true),
      arrayOf("aaa", 1, false),
      arrayOf("BBB", 13, true),
      arrayOf("bbb", 2, false),
      arrayOf("CCC", 15, true),
      arrayOf("ccc", 3, false),
      arrayOf("DDD", 17, true),
      arrayOf("ddd", 4, false),
      arrayOf("EEE", 18, true),
      arrayOf("eee", 5, false),
      arrayOf("FFF", 19, true),
      arrayOf("fff", 6, false),
      arrayOf("GGG", 92, true),
      arrayOf("ggg", 0, false)
    )
    val table = JTable(object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int): Class<*> {
        return when (column) {
          0 -> String::class.java
          1 -> Number::class.java
          2 -> Boolean::class.java
          else -> super.getColumnClass(column)
        }
      }
    })
    table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    table.transferHandler = handler
    table.dropMode = DropMode.INSERT_ROWS
    table.dragEnabled = true
    table.fillsViewportHeight = true
    val am = table.actionMap
    val dummy: Action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        /* Dummy action */
      }
    }
    am.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
    am.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
    am.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)
    return table
  }
}

class TableRowTransferHandler : TransferHandler() {
  private var source: JComponent? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    source = c
    val table = c as JTable
    val model = table.model as DefaultTableModel
    table.selectedRows.forEach { selectedIndices.add(it) }
    val transferData = table.selectedRows.map { model.dataVector[it] }.toList()
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferData
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport): Boolean {
    val canDrop = info.isDrop && info.isDataFlavorSupported(FLAVOR)
    val glassPane = (info.component as JComponent).rootPane.glassPane
    glassPane.cursor = if (canDrop) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    return canDrop
  }

  override fun getSourceActions(c: JComponent) = MOVE

  override fun importData(info: TransferSupport): Boolean {
    val tdl = info.dropLocation as? JTable.DropLocation ?: return false
    val target = info.component as JTable
    val model = target.model as DefaultTableModel
    val max = model.rowCount
    var index = tdl.row
    index = if (index in 0 until max) index else max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()

    for (o in values) {
      val row = index++
      model.insertRow(row, (o as List<*>).toTypedArray())
      target.selectionModel.addSelectionInterval(row, row)
    }
    addCount = if (target == source) values.size else 0
    return values.isNotEmpty()
  }

  override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    c.rootPane.glassPane.isVisible = false
    if (remove && selectedIndices.isNotEmpty()) {
      val selectedList = when {
        addCount > 0 -> selectedIndices.map { if (it >= addIndex) it + addCount else it }
        else -> selectedIndices.toList()
      }
      ((c as? JTable)?.getModel() as? DefaultTableModel)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.removeRow(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
  }

  companion object {
    private val FLAVOR = DataFlavor(MutableList::class.java, "List of items")
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
