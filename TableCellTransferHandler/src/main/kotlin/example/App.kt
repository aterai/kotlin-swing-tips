package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val handler = CellIconTransferHandler()
  val columnNames = arrayOf("String", "Icon", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", ColorIcon(Color.RED), true),
    arrayOf("bbb", ColorIcon(Color.GREEN), false),
    arrayOf("ccc", ColorIcon(Color.BLUE), true),
    arrayOf("ddd", ColorIcon(Color.ORANGE), true),
    arrayOf("eee", ColorIcon(Color.PINK), false),
    arrayOf("fff", ColorIcon(Color.CYAN), true)
  )
  val tableModel = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Icon::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  val table = JTable(tableModel)
  table.cellSelectionEnabled = true
  table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
  table.transferHandler = handler
  table.dragEnabled = true
  table.fillsViewportHeight = true
  val sorter = TableRowSorter(table.model)
  table.rowSorter = sorter

  val model = DefaultListModel<Icon>()
  val list = JList(model)
  list.layoutOrientation = JList.HORIZONTAL_WRAP
  list.visibleRowCount = 0
  list.fixedCellWidth = 16
  list.fixedCellHeight = 16
  list.cellRenderer = IconListCellRenderer()
  list.transferHandler = handler

  val clearButton = JButton("clear")
  clearButton.addActionListener {
    model.clear()
    sorter.setRowFilter(null)
  }

  val filterButton = JButton("filter")
  filterButton.addActionListener {
    val f = object : RowFilter<TableModel, Int>() {
      override fun include(entry: Entry<out TableModel, out Int>): Boolean {
        val o = entry.model.getValueAt(entry.identifier, 1)
        return model.isEmpty || model.contains(o)
      }
    }
    sorter.setRowFilter(f)
  }
  val box = Box.createHorizontalBox()
  box.add(clearButton)
  box.add(filterButton)

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(JScrollPane(list))
  p.add(box, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class CellIconTransferHandler : TransferHandler() {
  override fun createTransferable(c: JComponent): Transferable {
    val data = (c as? JTable)?.takeIf {
      Icon::class.java.isAssignableFrom(it.getColumnClass(it.selectedColumn))
    }?.let {
      it.getValueAt(it.selectedRow, it.selectedColumn)
    }
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(ICON_FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = ICON_FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        if (data != null && isDataFlavorSupported(flavor)) {
          return data
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
    info.component is JList<*> && info.isDataFlavorSupported(ICON_FLAVOR)

  override fun getSourceActions(c: JComponent) = COPY

  override fun importData(info: TransferSupport) = runCatching {
    val icon = info.transferable.getTransferData(ICON_FLAVOR)
    val model = (info.component as? JList<*>)?.model
    if (icon is Icon && model is DefaultListModel<*>) {
      @Suppress("UNCHECKED_CAST")
      (model as? DefaultListModel<Icon>)?.addElement(icon)
      true
    } else {
      false
    }
  }.isSuccess

  companion object {
    val ICON_FLAVOR = DataFlavor(Icon::class.java, "Icon")
  }
}

private class IconListCellRenderer<E : Icon> : ListCellRenderer<E> {
  private val renderer = JLabel()
  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ) = renderer.also { renderer.icon = value }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
