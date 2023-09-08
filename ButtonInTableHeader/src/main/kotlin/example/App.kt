package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

private val columnNames = arrayOf("Boolean", "Integer", "String")
private val data = arrayOf(
  arrayOf(true, 1, "BBB"),
  arrayOf(false, 12, "AAA"),
  arrayOf(true, 2, "DDD"),
  arrayOf(false, 5, "CCC"),
  arrayOf(true, 3, "EEE"),
  arrayOf(false, 6, "GGG"),
  arrayOf(true, 4, "FFF"),
  arrayOf(false, 7, "HHH"),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val table = object : JTable(model) {
  override fun updateUI() {
    selectionForeground = ColorUIResource(Color.RED)
    selectionBackground = ColorUIResource(Color.RED)
    super.updateUI()
    val m = model
    for (i in 0 until m.columnCount) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int
  ) = super.prepareEditor(editor, row, column).also {
    if (it is JCheckBox) {
      it.background = selectionBackground
      it.isBorderPainted = true
    }
  }
}

fun makeUI(): Component {
  val pop = JPopupMenu()
  pop.add("000")
  pop.add("11111")
  pop.add("2222222")
  val r = HeaderRenderer(table.tableHeader, pop)
  table.columnModel.getColumn(0).headerRenderer = r
  table.columnModel.getColumn(1).headerRenderer = r
  table.columnModel.getColumn(2).headerRenderer = r
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HeaderRenderer(
  header: JTableHeader,
  private val pop: JPopupMenu
) : JButton(), TableCellRenderer {
  private var rolloverIndex = -1
  private val handler = object : MouseInputAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val hdr = e.component as? JTableHeader ?: return
      val table = hdr.table
      val pt = e.point
      val vci = table.columnAtPoint(pt)
      val r = hdr.getHeaderRect(vci)
      val isSelected = true
      val hasFocus = true
      val c = getTableCellRendererComponent(table, "", isSelected, hasFocus, -1, vci)
      (c as? Container)?.also {
        r.translate(r.width - BUTTON_WIDTH, 0)
        r.setSize(BUTTON_WIDTH, r.height)
        if (it.componentCount > 0 && r.contains(pt)) {
          pop.show(hdr, r.x, r.height)
          (it.getComponent(0) as? JButton)?.doClick()
          e.consume()
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      val h = e.component as? JTableHeader ?: return
      rolloverIndex = -1
      h.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val h = e.component as? JTableHeader ?: return
      val table = h.table
      val vci = table.columnAtPoint(e.point)
      val mci = table.convertColumnIndexToModel(vci)
      rolloverIndex = mci
      h.repaint()
    }
  }

  init {
    header.addMouseListener(handler)
    header.addMouseMotionListener(handler)
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder()
    isContentAreaFilled = false
    EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(pop) }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    (c as? JComponent)?.also {
      icon = MenuArrowIcon()
      it.removeAll()
      val mci = table.convertColumnIndexToModel(column)
      if (rolloverIndex == mci) {
        val w = table.columnModel.getColumn(mci).width
        val h = table.tableHeader.height
        val outside = it.border
        val inside = BorderFactory.createEmptyBorder(0, 0, 0, BUTTON_WIDTH)
        val b = BorderFactory.createCompoundBorder(outside, inside)
        it.border = b
        it.add(this)
        setBounds(w - BUTTON_WIDTH, 0, BUTTON_WIDTH, h - 2)
        background = BUTTON_BGC
        isOpaque = true
        border = BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY)
      }
    }
    return c
  }

  companion object {
    const val BUTTON_WIDTH = 16
    val BUTTON_BGC = Color(0x64_C8_C8_C8, true)
  }
}

private class MenuArrowIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.BLACK
    g2.translate(x, y)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 10

  override fun getIconHeight() = 10
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
