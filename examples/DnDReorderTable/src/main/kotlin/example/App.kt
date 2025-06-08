package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("AAA", 12, true),
    arrayOf("BBB", 1, false),
    arrayOf("ccc", 13, true),
    arrayOf("DDD", 2, false),
    arrayOf("eee", 15, true),
    arrayOf("FFF", 3, false),
    arrayOf("GGG", 17, true),
    arrayOf("hhh", 4, false),
    arrayOf("III", 18, true),
    arrayOf("jjj", 5, false),
    arrayOf("KKK", 19, true),
    arrayOf("LLL", 6, false),
    arrayOf("mmm", 92, true),
    arrayOf("nnn", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  val handler = TableRowTransferHandler()
  val table = JTable(model)
  table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  table.transferHandler = handler
  table.dropMode = DropMode.INSERT_ROWS
  table.dragEnabled = true
  table.fillsViewportHeight = true

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(table))
  p.border = BorderFactory.createTitledBorder("Drag & Drop JTable")

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TableRowTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    val table = c as? JTable
    val model = table?.model as? DefaultTableModel
    val transferredRows = mutableListOf<Any>()
    selectedIndices.clear()
    if (model != null) {
      table.selectedRows.forEach {
        selectedIndices.add(it)
        transferredRows.add(model.dataVector[it])
      }
    }
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(
        flavor: DataFlavor,
      ) = if (isDataFlavorSupported(flavor)) {
        transferredRows
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(info: TransferSupport): Boolean {
    val canDrop = info.isDrop && info.isDataFlavorSupported(FLAVOR)
    (info.component as? JComponent)?.rootPane?.glassPane?.also {
      it.cursor = if (canDrop) {
        DragSource.DefaultMoveDrop
      } else {
        DragSource.DefaultMoveNoDrop
      }
    }
    return canDrop
  }

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val target = info.component as? JTable
    val model = target?.model as? DefaultTableModel ?: return false
    val max = model.rowCount
    var index = if (info.isDrop) {
      (info.dropLocation as? JTable.DropLocation)?.row ?: -1
    } else {
      target.selectedRow
    }
    index = if (index in 0..<max) index else max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    values.filterIsInstance<List<*>>().forEach {
      val i = index++
      model.insertRow(i, it.toTypedArray())
      target.selectionModel.addSelectionInterval(i, i)
      target.requestFocusInWindow()
    }
    addCount = if (info.isDrop) values.size else 0
    return values.isNotEmpty()
  }

  override fun exportDone(
    c: JComponent,
    data: Transferable?,
    action: Int,
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(
    c: JComponent,
    remove: Boolean,
  ) {
    c.rootPane.glassPane.isVisible = false
    if (remove && selectedIndices.isNotEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
      }
      ((c as? JTable)?.model as? DefaultTableModel)?.also { model ->
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
    private val FLAVOR = DataFlavor(List::class.java, "List of items")
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
