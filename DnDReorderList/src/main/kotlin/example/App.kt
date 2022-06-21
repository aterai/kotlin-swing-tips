package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(BorderLayout())
  p.add(JScrollPane(makeList()))
  p.border = BorderFactory.createTitledBorder("Drag & Drop JList")
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeList(): JList<Color> {
  val listModel = DefaultListModel<Color>()
  listModel.addElement(Color.RED)
  listModel.addElement(Color.BLUE)
  listModel.addElement(Color.GREEN)
  listModel.addElement(Color.CYAN)
  listModel.addElement(Color.ORANGE)
  listModel.addElement(Color.PINK)
  listModel.addElement(Color.MAGENTA)
  val list = object : JList<Color>(listModel) {
    override fun updateUI() {
      selectionBackground = null // Nimbus
      cellRenderer = null
      super.updateUI()
      val renderer = cellRenderer
      setCellRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          it.foreground = value
        }
      }
      // setVisibleRowCount(-1)
      selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
      dropMode = DropMode.INSERT
      dragEnabled = true
      transferHandler = ListItemTransferHandler()
    }
  }

  // Disable row Cut, Copy, Paste
  val map = list.actionMap
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      /* Dummy action */
    }
  }
  map.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)
  return list
}

private class ListItemTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent) = object : Transferable {
    override fun getTransferDataFlavors() = arrayOf(FLAVOR)

    override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
      val src = c as? JList<*>
      return if (isDataFlavorSupported(flavor) && src != null) {
        src.selectedIndices.forEach { selectedIndices.add(it) }
        src.selectedValuesList
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(info: TransferSupport) = info.isDrop &&
    info.isDataFlavorSupported(FLAVOR) &&
    info.dropLocation is JList.DropLocation

  override fun getSourceActions(c: JComponent) = MOVE

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.dropLocation
    val target = info.component

    @Suppress("UNCHECKED_CAST")
    val listModel = (target as? JList<Any>)?.model as? DefaultListModel<Any>
    if (dl !is JList.DropLocation || listModel == null) {
      return false
    }
    val max = listModel.size
    // var index = if (dl.index in 0 until max) dl.index else max
    var index = dl.index.takeIf { it in 0 until max } ?: max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    for (o in values) {
      val i = index++
      listModel.add(i, o)
      target.addSelectionInterval(i, i)
    }
    addCount = values.size
    return values.isNotEmpty()
  }

  override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && selectedIndices.isNotEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
      }
      ((c as? JList<*>)?.model as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.remove(selectedList[i])
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
