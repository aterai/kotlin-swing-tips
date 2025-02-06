package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import javax.swing.*

fun makeUI(): Component {
  val model = makeModel()
  val list = makeList(model)
  val tb = JToolBar()
  tb.isFloatable = false
  tb.add(makeUpButton(list, model))
  tb.add(makeDownButton(list, model))
  return JPanel(BorderLayout()).also {
    it.add(tb, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> makeUpButton(
  list: JList<E>,
  m: DefaultListModel<E>,
): JButton {
  val button = JButton("▲")
  button.isFocusable = false
  button.addActionListener { e ->
    val pos = list.selectedIndices
    if (pos.isNotEmpty()) {
      val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
      val index0 = if (isShiftDown) 0 else maxOf(0, pos[0] - 1)
      var idx = index0
      for (i in pos) {
        m.add(idx, m.remove(i))
        list.addSelectionInterval(idx, idx)
        idx++
      }
      val r = list.getCellBounds(index0, index0 + pos.size)
      list.scrollRectToVisible(r)
    }
  }
  return button
}

private fun <E> makeDownButton(
  list: JList<E>,
  m: DefaultListModel<E>,
): JButton {
  val button = JButton("▼")
  button.isFocusable = false
  button.addActionListener { e ->
    val pos = list.selectedIndices
    if (pos.isNotEmpty()) {
      val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
      val max = m.size
      var index = if (isShiftDown) max else minOf(max, pos[pos.size - 1] + 1)
      val index0 = index
      // copy
      for (i in pos) {
        val idx = minOf(m.size, ++index)
        m.add(idx, m[i])
        list.addSelectionInterval(idx, idx)
      }
      // clean
      pos.reversed().forEach { m.remove(it) }
      // for (i in pos.reversed()) {
      //   m.remove(i)
      // }
      val r = list.getCellBounds(index0 - pos.size, index0)
      list.scrollRectToVisible(r)
    }
  }
  return button
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

private fun makeList(model: ListModel<Color>) = object : JList<Color>(model) {
  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    dropMode = DropMode.INSERT
    dragEnabled = true
    transferHandler = ListItemTransferHandler()
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
        val i = index++
        it.add(i, o)
        target.addSelectionInterval(i, i)
      }
    }
    addCount = if (info.isDrop) values.size else 0
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
