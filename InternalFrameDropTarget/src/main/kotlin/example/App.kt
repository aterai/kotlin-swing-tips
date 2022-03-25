package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val dp = JDesktopPane()
  val handler = TableRowTransferHandler()
  val f1 = JInternalFrame("11111111", true, true, true, true)
  f1.add(JScrollPane(makeDnDTable(handler)))
  f1.isOpaque = false
  dp.add(f1, 1, 1)
  f1.setBounds(0, 0, 240, 160)

  val f2 = JInternalFrame("22222222", true, true, true, true)
  f2.add(JScrollPane(makeDnDTable(handler)))
  dp.add(f2, 1, 0)
  f2.setBounds(50, 50, 240, 160)
  f2.isOpaque = false

  EventQueue.invokeLater { dp.allFrames.forEach { it.isVisible = true } }
  return JPanel(BorderLayout()).also {
    it.add(dp)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeDnDTable(handler: TransferHandler): JTable {
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
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  val table = JTable(model)
  table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  table.transferHandler = handler
  table.dropMode = DropMode.INSERT_ROWS
  table.dragEnabled = true
  table.fillsViewportHeight = true
  table.tableHeader.reorderingAllowed = false

  val am = table.actionMap
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      /* Dummy action */
    }
  }
  am.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
  am.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
  am.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)
  return table
}

private class TableRowTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.
  private var source: Component? = null

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    source = c
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        val table = c as? JTable
        val model = table?.model
        return if (isDataFlavorSupported(flavor) && model is DefaultTableModel) {
          table.selectedRows.forEach { selectedIndices.add(it) }
          table.selectedRows.map { model.dataVector[it] }
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  private fun getInternalFrame(c: Component?) =
    SwingUtilities.getAncestorOfClass(JInternalFrame::class.java, c) as? JInternalFrame

  private fun canDropTableIntersection(info: TransferSupport): Boolean {
    val target = info.component // as? JTable ?: return false
    if (target != source) {
      val dp = SwingUtilities.getAncestorOfClass(JDesktopPane::class.java, target)
      val sf = getInternalFrame(source)
      val tf = getInternalFrame(target)
      return (dp as? JDesktopPane)
        ?.takeIf { sf != null && tf != null && it.getIndexOf(tf) > it.getIndexOf(sf) }
        ?.let {
          val pt = SwingUtilities.convertPoint(target, info.dropLocation.dropPoint, it)
          if (sf != null && tf != null) {
            val rect = sf.bounds.intersection(tf.bounds)
            !rect.contains(pt)
          } else {
            false
          }
        } ?: false
    }
    return true
  }

  override fun canImport(info: TransferSupport): Boolean {
    val isSupported = info.isDataFlavorSupported(FLAVOR) && canDropTableIntersection(info)
    val canDrop = info.isDrop && isSupported
    val dp = SwingUtilities.getAncestorOfClass(JDesktopPane::class.java, info.component)
    val glassPane = (dp as? JComponent)?.rootPane?.glassPane ?: return false
    glassPane.cursor = if (canDrop) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    return canDrop
  }

  override fun getSourceActions(c: JComponent?) = MOVE

  override fun importData(info: TransferSupport): Boolean {
    val tdl = info.dropLocation
    val target = info.component as? JTable
    val model = target?.model
    if (tdl !is JTable.DropLocation || model !is DefaultTableModel) {
      return false
    }
    val max = model.rowCount
    var index = tdl.row
    index = if (index in 0 until max) index else max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    addCount = values.size
    for (o in values) {
      val row = index++
      val list = o as? List<*> ?: continue
      val array = arrayOfNulls<Any?>(list.size)
      for ((i, v) in list.withIndex()) {
        array[i] = v
      }
      model.insertRow(row, array)
      target.selectionModel.addSelectionInterval(row, row)
    }
    return values.isNotEmpty()
  }

  override fun exportDone(
    c: JComponent,
    data: Transferable?,
    action: Int
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    c.rootPane.glassPane.isVisible = false
    if (remove && selectedIndices.isNotEmpty()) {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
