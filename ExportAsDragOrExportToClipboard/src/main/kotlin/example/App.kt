package example

import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.InputEvent
import javax.swing.*

fun makeUI(): Component {
  val list = makeList(makeModel())

  val check1 = JCheckBox("canExportAsDrag")
  check1.addActionListener {
    val b = check1.isSelected
    list.putClientProperty(check1.text, b)
  }

  val check2 = JCheckBox("canExportToClipboard")
  check2.addActionListener {
    val b = check2.isSelected
    list.putClientProperty(check2.text, b)
  }

  val check3 = JCheckBox("canImportFromClipboard")
  check3.addActionListener {
    val b = check3.isSelected
    list.putClientProperty(check3.text, b)
  }

  val box1 = Box.createHorizontalBox()
  box1.add(check1)
  val box2 = Box.createHorizontalBox()
  box2.add(check2)
  box2.add(check3)
  val p = JPanel(BorderLayout())
  p.add(box1, BorderLayout.NORTH)
  p.add(box2, BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultListModel<Color>().also {
  it.addElement(Color.RED)
  it.addElement(Color.BLUE)
  it.addElement(Color.GREEN)
  it.addElement(Color.CYAN)
  it.addElement(Color.ORANGE)
  it.addElement(Color.PINK)
  it.addElement(Color.MAGENTA)
}

private fun makeList(model: ListModel<Color>): JList<Color> {
  return object : JList<Color>(model) {
    override fun updateUI() {
      selectionBackground = null // Nimbus
      cellRenderer = null
      super.updateUI()
      selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
      dropMode = DropMode.INSERT
      dragEnabled = true
      val renderer = cellRenderer
      setCellRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          it.foreground = value
        }
      }
      transferHandler = ListItemTransferHandler()
      componentPopupMenu = ListPopupMenu(this)
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

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JList<*>) {
      val isSelected = !c.isSelectionEmpty
      cutItem.isEnabled = isSelected
      copyItem.isEnabled = isSelected
      super.show(c, x, y)
    }
  }
}

private class ListItemTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    val source = (c as? JList<*>)?.also { s ->
      s.selectedIndices.forEach { selectedIndices.add(it) }
    }
    val selectedValues = source?.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor) && selectedValues != null) {
          selectedValues
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) = info.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    // println("importData(TransferSupport)")
    val target = info.component as? JList<*>
    val v = target?.getClientProperty("canImportFromClipboard")
    val b = !info.isDrop && (v == null || v == false)
    if (target == null || b) {
      return false
    }

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
    addCount = if (info.isDrop) values.size else 0
    // target.requestFocusInWindow()
    return values.isNotEmpty()
  }

  override fun importData(
    c: JComponent?,
    t: Transferable?,
  ) = importData(TransferSupport(c, t))

  override fun exportAsDrag(
    comp: JComponent,
    e: InputEvent?,
    action: Int,
  ) {
    // println("exportAsDrag")
    if (comp.getClientProperty("canExportAsDrag") == true) {
      super.exportAsDrag(comp, e, action)
    }
  }

  override fun exportToClipboard(
    comp: JComponent,
    clip: Clipboard?,
    action: Int,
  ) {
    // println("exportToClipboard")
    if (comp.getClientProperty("canExportToClipboard") == true) {
      super.exportToClipboard(comp, clip, action)
    }
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
    if (remove && selectedIndices.isNotEmpty()) {
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
      }
      ((c as? JList<*>)?.model as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.reversed()) {
          model.remove(i)
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
