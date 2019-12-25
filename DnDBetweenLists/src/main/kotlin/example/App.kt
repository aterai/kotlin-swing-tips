package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.io.IOException
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p = JPanel(GridLayout(1, 2, 10, 0))
    val h = ListItemTransferHandler()
    p.setBorder(BorderFactory.createTitledBorder("Drag & Drop between JLists"))
    p.add(JScrollPane(makeList(h)))
    p.add(JScrollPane(makeList(h)))
    add(p)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
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
        setCellRenderer(null)
        super.updateUI()
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        setDropMode(DropMode.INSERT)
        setDragEnabled(true)
        setTransferHandler(handler)
        val renderer = getCellRenderer()
        setCellRenderer { list, value, index, isSelected, cellHasFocus ->
          val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          (c as? JLabel)?.setForeground(value)
          return@setCellRenderer c
        }
      }
    }

    // Disable row Cut, Copy, Paste
    val map = list.getActionMap()
    val dummy = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) { /* Dummy action */ }
    }
    map.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
    map.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
    map.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)

    return list
  }
}

// Demo - BasicDnD (The Java? Tutorials > Creating a GUI With JFC/Swing > Drag and Drop and Data Transfer)
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/basicdemo.html
class ListItemTransferHandler : TransferHandler() {
  private val localObjectFlavor = DataFlavor(List::class.java, "List of items")
  private var source: JList<*>? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable? {
    val src = c as? JList<*> ?: return null
    source = src
    src.getSelectedIndices().forEach { selectedIndices.add(it) }
    val transferObjects = src.getSelectedValuesList()
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
    info.isDrop() &&
        info.isDataFlavorSupported(localObjectFlavor) &&
        info.getDropLocation() is JList.DropLocation

  override fun getSourceActions(c: JComponent) = MOVE // COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.getDropLocation()
    val target = info.getComponent()
    if (dl !is JList.DropLocation || target !is JList<*>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val listModel = target.getModel() as DefaultListModel<Any>
    val max = listModel.getSize()
    // var index = minOf(maxOf(0, dl.getIndex()), max)
    var index = dl.getIndex().takeIf { it in 0 until max } ?: max
    addIndex = index
    val values = runCatching {
      info.getTransferable().getTransferData(localObjectFlavor) as? List<*>
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
      ((c as? JList<*>)?.getModel() as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.remove(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
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
