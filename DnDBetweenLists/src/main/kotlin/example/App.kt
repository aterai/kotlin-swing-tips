package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(1, 2, 10, 0))
  val h = ListItemTransferHandler()
  p.border = BorderFactory.createTitledBorder("Drag & Drop between JLists")
  p.add(JScrollPane(makeList(h)))
  p.add(JScrollPane(makeList(h)))
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeList(handler: TransferHandler): JList<Color> {
  val listModel = DefaultListModel<Color>().also {
    it.addElement(Color.RED)
    it.addElement(Color.BLUE)
    it.addElement(Color.GREEN)
    it.addElement(Color.CYAN)
    it.addElement(Color.ORANGE)
    it.addElement(Color.PINK)
    it.addElement(Color.MAGENTA)
  }
  val list = object : JList<Color>(listModel) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
      dropMode = DropMode.INSERT
      dragEnabled = true
      transferHandler = handler
      val renderer = cellRenderer
      setCellRenderer { list, value, index, isSelected, cellHasFocus ->
        val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        c.foreground = value
        c
      }
    }
  }

  // Disable row Cut, Copy, Paste
  val map = list.actionMap
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) { /* Dummy action */
    }
  }
  map.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)

  return list
}

class ListItemTransferHandler : TransferHandler() {
  private var source: JList<*>? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable? {
    val src = c as? JList<*> ?: return null
    source = src
    src.selectedIndices.forEach { selectedIndices.add(it) }
    val transferredObjects = src.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferredObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
    info.isDrop &&
      info.isDataFlavorSupported(FLAVOR) &&
      info.dropLocation is JList.DropLocation

  override fun getSourceActions(c: JComponent) = MOVE

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.dropLocation
    val target = info.component
    if (dl !is JList.DropLocation || target !is JList<*>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val listModel = target.model as DefaultListModel<Any>
    val max = listModel.size
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
    addCount = if (target == source) values.size else 0
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
      val selectedList = when {
        addCount > 0 -> selectedIndices.map { if (it >= addIndex) it + addCount else it }
        else -> selectedIndices.toList()
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
