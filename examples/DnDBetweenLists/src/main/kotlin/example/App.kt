package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.*

fun createUI(): Component {
  val p = JPanel(GridLayout(1, 2, 10, 0))
  // Share a single handler so that it can tell whether the drop target is the drag source
  val handler = ListItemTransferHandler()
  p.border = BorderFactory.createTitledBorder("Drag & Drop between JLists")
  p.add(JScrollPane(createList(createModel(), handler)))
  p.add(JScrollPane(createList(createModel(), handler)))
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel() = DefaultListModel<Color>().also {
  it.addElement(Color.RED)
  it.addElement(Color.BLUE)
  it.addElement(Color.GREEN)
  it.addElement(Color.CYAN)
  it.addElement(Color.ORANGE)
  it.addElement(Color.PINK)
  it.addElement(Color.MAGENTA)
}

private fun createList(
  model: ListModel<Color>,
  handler: TransferHandler,
): JList<Color> = object : JList<Color>(model) {
  override fun updateUI() {
    selectionBackground = null // Nimbus
    cellRenderer = null
    super.updateUI()
    selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    dropMode = DropMode.INSERT
    dragEnabled = true
    val renderer = cellRenderer
    setCellRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          it.foreground = value
        }
    }
    transferHandler = handler
  }
}

private class ListItemTransferHandler : TransferHandler() {
  private var source: JList<*>? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added to the drag source list

  override fun createTransferable(c: JComponent): Transferable {
    selectedIndices.clear()
    source = (c as? JList<*>)?.also { s ->
      s.selectedIndices.forEach { selectedIndices.add(it) }
    }
    val selectedValues = source?.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(
        flavor: DataFlavor,
      ) = if (isDataFlavorSupported(flavor) && selectedValues != null) {
        selectedValues
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(info: TransferSupport) = info.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val target = info.component as? JList<*> ?: return false
    var index = getIndex(info)
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()

    @Suppress("UNCHECKED_CAST")
    (target.model as? DefaultListModel<Any>)?.also {
      for (o in values) {
        it.add(index, o)
        target.addSelectionInterval(index, index)
        index += 1
      }
    }
    addCount = if (info.isDrop && target == source) values.size else 0
    target.requestFocusInWindow()
    return values.isNotEmpty()
  }

  override fun importData(
    comp: JComponent,
    t: Transferable,
  ) = importData(TransferSupport(comp, t))

  override fun exportDone(
    c: JComponent,
    data: Transferable,
    action: Int,
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(
    c: JComponent,
    remove: Boolean,
  ) {
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

  private fun getIndex(info: TransferSupport): Int {
    val target = info.component as? JList<*> ?: return -1
    // Keyboard Copy & Paste: insert at the selected index
    var index = target.selectedIndex
    if (info.isDrop) { // Mouse Drag & Drop
      val location = info.dropLocation
      if (location is JList.DropLocation) {
        index = location.index
      }
    }
    val max = target.model.size
    // If it is out of range, it is appended to the end
    return if (index < 0 || index > max) max else index
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
