package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.*

fun createUI(): Component {
  val p = JPanel(BorderLayout())
  p.add(JScrollPane(createList()))
  p.border = BorderFactory.createTitledBorder("Drag & Drop JList")
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createList(): JList<Color> {
  val listModel = DefaultListModel<Color>().also {
    it.addElement(Color.RED)
    it.addElement(Color.BLUE)
    it.addElement(Color.GREEN)
    it.addElement(Color.CYAN)
    it.addElement(Color.ORANGE)
    it.addElement(Color.PINK)
    it.addElement(Color.MAGENTA)
  }
  return object : JList<Color>(listModel) {
    override fun updateUI() {
      selectionBackground = null // Nimbus
      cellRenderer = null
      super.updateUI()
      selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
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
      transferHandler = ListItemTransferHandler()
    }
  }
}

private class ListItemTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    selectedIndices.clear()
    val source = (c as? JList<*>)?.also { s ->
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

  override fun canImport(support: TransferSupport) = support.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(support: TransferSupport): Boolean {
    val target = support.component as? JList<*> ?: return false
    var index = getInsertionIndex(support)
    addIndex = index
    val values = runCatching {
      support.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()

    @Suppress("UNCHECKED_CAST")
    (target.model as? DefaultListModel<Any>)?.also {
      for (o in values) {
        it.add(index, o)
        target.addSelectionInterval(index, index)
        index++
      }
    }
    // Only a drop within the same list shifts the indices of the items to be removed in
    // exportDone(...); a cut has already removed them before the paste.
    addCount = if (support.isDrop) values.size else 0
    return values.isNotEmpty()
  }

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
      // at or after the insertion point have moved.
      if (addCount > 0) {
        selectedIndices.replaceAll { if (it >= addIndex) it + addCount else it }
      }
      ((c as? JList<*>)?.model as? DefaultListModel<*>)?.also { model ->
        // Remove from the end so that the remaining indices stay valid
        for (i in selectedIndices.indices.reversed()) {
          model.remove(selectedIndices[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
  }

  private fun getInsertionIndex(support: TransferSupport): Int {
    val target = support.component as? JList<*> ?: return -1
    val location = if (support.isDrop) support.dropLocation else null
    val index = if (location is JList.DropLocation) { // Mouse Drag & Drop
      location.index
    } else { // Keyboard Copy & Paste
      target.selectedIndex
    }
    val max = target.model.size
    // If it is out of range, it is appended to the end
    return if (index < 0) max else index.coerceAtMost(max)
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
