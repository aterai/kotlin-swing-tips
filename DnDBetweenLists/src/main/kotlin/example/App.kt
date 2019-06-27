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
    val list = JList<Color>(listModel)
    list.setCellRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        (c as? JLabel)?.setForeground(value as? Color)
        return c
      }
    })
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    list.setDropMode(DropMode.INSERT)
    list.setDragEnabled(true)
    list.setTransferHandler(handler)

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

// Demo - BasicDnD (The Java™ Tutorials > Creating a GUI With JFC/Swing > Drag and Drop and Data Transfer)
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/basicdemo.html
internal class ListItemTransferHandler : TransferHandler() {
  protected val localObjectFlavor: DataFlavor
  protected var source: JList<*>? = null
  protected val selectedIndices = mutableListOf<Int>()
  protected var addIndex = -1 // Location where items were added
  protected var addCount = 0 // Number of items added.

  init {
    localObjectFlavor = DataFlavor(List::class.java, "List of items")
  }

  protected override fun createTransferable(c: JComponent): Transferable? {
    val src = c as JList<*>
    source = src
    src.getSelectedIndices().forEach { selectedIndices.add(it) }
    val transferedObjects = src.getSelectedValuesList()
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf<DataFlavor>(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferedObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferHandler.TransferSupport) =
    info.isDrop() &&
    info.isDataFlavorSupported(localObjectFlavor) &&
    info.getDropLocation() is JList.DropLocation

  override fun getSourceActions(c: JComponent) = TransferHandler.MOVE // TransferHandler.COPY_OR_MOVE

  override fun importData(info: TransferHandler.TransferSupport): Boolean {
    if (!canImport(info)) {
      return false
    }
    val dl = info.getDropLocation() as JList.DropLocation
    val target = info.getComponent() as JList<*>
    @Suppress("UNCHECKED_CAST")
    val listModel = target.getModel() as DefaultListModel<Any>
    val max = listModel.getSize()
    var index = dl.getIndex().takeIf { it >= 0 && it < max } ?: max
    addIndex = index
    return runCatching {
      val values = info.getTransferable().getTransferData(localObjectFlavor) as List<*>
      for (o in values) {
        val i = index++
        listModel.add(i, o)
        target.addSelectionInterval(i, i)
      }
      addCount = if (target == source) values.size else 0
    }.isSuccess
  }

  protected override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    cleanup(c, action == TransferHandler.MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && !selectedIndices.isEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      val selectedList = if (addCount > 0) selectedIndices.map { if (it >= addIndex) it + addCount else it }
          else selectedIndices.toList()
      val model = (c as JList<*>).getModel() as DefaultListModel<*>
      for (i in selectedList.indices.reversed()) {
        model.remove(selectedList[i])
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
