package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(1, 2, 10, 0))
  val h = ListItemTransferHandler()
  p.border = BorderFactory.createTitledBorder("Drag & Drop(Copy, Cut, Paste) between JLists")
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
  return object : JList<Color>(listModel) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
      dropMode = DropMode.INSERT
      dragEnabled = true
      transferHandler = handler
      componentPopupMenu = ListPopupMenu(this)
      val renderer = cellRenderer
      setCellRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          it.foreground = value
        }
      }
    }
  }
}

private class ListPopupMenu(list: JList<*>) : JPopupMenu() {
  private val cutItem: JMenuItem
  private val copyItem: JMenuItem

  init {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val handler = list.transferHandler
    cutItem = add("cut")
    cutItem.addActionListener {
      handler.exportToClipboard(list, clipboard, TransferHandler.MOVE)
    }
    copyItem = add("copy")
    copyItem.addActionListener {
      handler.exportToClipboard(list, clipboard, TransferHandler.COPY)
    }
    add("paste").addActionListener {
      handler.importData(list, clipboard.getContents(null))
    }
    addSeparator()
    add("clearSelection").addActionListener { list.clearSelection() }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JList<*>) {
      val isSelected = !c.isSelectionEmpty
      cutItem.isEnabled = isSelected
      copyItem.isEnabled = isSelected
      super.show(c, x, y)
    }
  }
}

private class ListItemTransferHandler : TransferHandler() {
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
        val i = index++
        it.add(i, o)
        target.addSelectionInterval(i, i)
      }
    }
    addCount = if (target == source) values.size else 0
    return values.isNotEmpty()
  }

  override fun importData(comp: JComponent, t: Transferable) = importData(TransferSupport(comp, t))

  override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && selectedIndices.isNotEmpty()) {
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

  private fun getIndex(info: TransferSupport): Int {
    val target = info.component as? JList<*> ?: return -1
    var index = if (info.isDrop) { // Mouse Drag & Drop
      val tdl = info.dropLocation
      if (tdl is JList.DropLocation) {
        tdl.index
      } else {
        target.selectedIndex
      }
    } else { // Keyboard Copy & Paste
      target.selectedIndex
    }
    val max = (target.model as? DefaultListModel<*>)?.size ?: -1
    index = if (index < 0) max else index
    index = index.coerceAtMost(max)
    return index
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
