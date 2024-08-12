package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import javax.swing.*
import javax.swing.plaf.UIResource
import javax.swing.plaf.synth.SynthTableUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeScrollPane(RoundedCellSelectionTable(makeModel())))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(view: Component): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.background = Color.WHITE
  scroll.viewport.setOpaque(false)
  scroll.viewportBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2)
  return scroll
}

fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
    arrayOf("eee", 32, true),
    arrayOf("fff", 8, false),
    arrayOf("ggg", 64, true),
    arrayOf("hhh", 1, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private class RoundedCellSelectionTable(
  model: TableModel,
) : JTable(model) {
  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    isFocusable = false
    setCellSelectionEnabled(true)
    setShowGrid(false)
    intercellSpacing = Dimension()
    autoCreateRowSorter = true
    background = Color(0x0, true)
    setRowHeight(20)
    if (getUI() is SynthTableUI) {
      setDefaultRenderer(
        Boolean::class.javaObjectType,
        SynthBooleanTableCellRenderer2(),
      )
    }
  }

  override fun prepareRenderer(
    renderer: TableCellRenderer,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if (c is JComponent) {
      c.isOpaque = false
    }
    return c
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    if (c is JComponent) {
      c.isOpaque = false
    }
    return c
  }

  override fun changeSelection(
    rowIndex: Int,
    columnIndex: Int,
    toggle: Boolean,
    extend: Boolean,
  ) {
    super.changeSelection(rowIndex, columnIndex, toggle, extend)
    repaint()
  }

  override fun paintComponent(g: Graphics) {
    if (selectedColumnCount != 0 && selectedRowCount != 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = getSelectionBackground()
      val area = Area()
      for (row in 0..<rowCount) {
        for (col in 0..<columnCount) {
          addArea(area, row, col)
        }
      }
      // if (!area.isEmpty()) {
      val arc = 8
      for (a in singularization(area)) {
        val r = a.bounds
        g2.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, arc, arc)
      }
      g2.dispose()
    }
    super.paintComponent(g)
  }

  private fun addArea(area: Area, row: Int, col: Int) {
    if (isCellSelected(row, col)) {
      area.add(Area(getCellRect(row, col, true)))
    }
  }

  private fun singularization(rect: Area): List<Area> {
    val list = mutableListOf<Area>()
    val path = Path2D.Double()
    val pi = rect.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO -> {
          path.reset()
          path.moveTo(coords[0], coords[1])
        }

        PathIterator.SEG_LINETO -> {
          path.lineTo(coords[0], coords[1])
        }

        PathIterator.SEG_CLOSE -> {
          path.closePath()
          list.add(Area(path))
        }
      }
      pi.next()
    }
    return list
  }
}

private class SynthBooleanTableCellRenderer2 :
  JCheckBox(),
  TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    horizontalAlignment = CENTER
    name = "Table.cellRenderer"
    if (isSelected) {
      foreground = unwrap(table.selectionForeground)
      background = unwrap(table.selectionBackground)
    } else {
      foreground = unwrap(table.foreground)
      background = unwrap(table.background)
    }
    setSelected(value as? Boolean == true)
    return this
  }

  override fun isOpaque() = false

  private fun unwrap(c: Color) = if (c is UIResource) Color(c.rgb) else c
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
