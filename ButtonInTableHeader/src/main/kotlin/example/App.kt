package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("Boolean", "Integer", "String")
  private val data = arrayOf(
    arrayOf(true, 1, "BBB"),
    arrayOf(false, 12, "AAA"),
    arrayOf(true, 2, "DDD"),
    arrayOf(false, 5, "CCC"),
    arrayOf(true, 3, "EEE"),
    arrayOf(false, 6, "GGG"),
    arrayOf(true, 4, "FFF"),
    arrayOf(false, 7, "HHH")
  )
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  private val table = object : JTable(model) {
    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      super.updateUI()
      val m = getModel()
      for (i in 0 until m.getColumnCount()) {
        (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
          SwingUtilities.updateComponentTreeUI(it)
        }
      }
    }

    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int
    ): Component {
      val c = super.prepareEditor(editor, row, column)
      if (c is JCheckBox) {
        c.setBackground(getSelectionBackground())
        c.setBorderPainted(true)
      }
      return c
    }
  }

  init {
    val pop = JPopupMenu()
    pop.add("000")
    pop.add("11111")
    pop.add("2222222")
    val r = HeaderRenderer(table.getTableHeader(), pop)
    table.getColumnModel().getColumn(0).setHeaderRenderer(r)
    table.getColumnModel().getColumn(1).setHeaderRenderer(r)
    table.getColumnModel().getColumn(2).setHeaderRenderer(r)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class HeaderRenderer(header: JTableHeader, private val pop: JPopupMenu) : JButton(), TableCellRenderer {
  private var rolloverIndex = -1
  @Transient
  private val handler = object : MouseInputAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val h = e.getComponent() as? JTableHeader ?: return
      val table = h.getTable()
      val columnModel = table.getColumnModel()
      val vci = columnModel.getColumnIndexAtX(e.x)
      val r = h.getHeaderRect(vci)
      val c = getTableCellRendererComponent(table, "", isSelected = true, hasFocus = true, row = -1, column = vci)
      (c as? Container)?.also {
        r.translate(r.width - BUTTON_WIDTH, 0)
        r.setSize(BUTTON_WIDTH, r.height)
        if (it.getComponentCount() > 0 && r.contains(e.getPoint())) {
          pop.show(h, r.x, r.height)
          (it.getComponent(0) as? JButton)?.doClick()
          e.consume()
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      rolloverIndex = -1
    }

    override fun mouseMoved(e: MouseEvent) {
      val h = e.getComponent() as? JTableHeader ?: return
      val table = h.getTable()
      val columnModel = table.getColumnModel()
      val vci = columnModel.getColumnIndexAtX(e.x)
      val mci = table.convertColumnIndexToModel(vci)
      rolloverIndex = mci
    }
  }

  override fun updateUI() {
    super.updateUI()
    setBorder(BorderFactory.createEmptyBorder())
    setContentAreaFilled(false)
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
    val r = table.getTableHeader().getDefaultRenderer()
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    (c as? JLabel)?.also {
      setIcon(MenuArrowIcon())
      it.removeAll()
      val mci = table.convertColumnIndexToModel(column)
      if (rolloverIndex == mci) {
        val w = table.getColumnModel().getColumn(mci).getWidth()
        val h = table.getTableHeader().getHeight()
        val outside = it.getBorder()
        val inside = BorderFactory.createEmptyBorder(0, 0, 0, BUTTON_WIDTH)
        val b = BorderFactory.createCompoundBorder(outside, inside)
        it.setBorder(b)
        it.add(this)
        setBounds(w - BUTTON_WIDTH, 0, BUTTON_WIDTH, h - 2)
        setBackground(BUTTON_BGC)
        setOpaque(true)
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY))
      }
    }
    return c
  }

  companion object {
    const val BUTTON_WIDTH = 16
    val BUTTON_BGC = Color(0x64C8C8C8, true)
  }

  init {
    header.addMouseListener(handler)
    header.addMouseMotionListener(handler)
  }
}

class MenuArrowIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(Color.BLACK)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
