package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table0 = JTable(model)
  table0.autoCreateRowSorter = true
  table0.putClientProperty("terminateEditOnFocusLost", true)

  val table1 = makeTable(model)
  table1.autoCreateRowSorter = true
  table1.putClientProperty("terminateEditOnFocusLost", true)

  return JSplitPane(
    JSplitPane.VERTICAL_SPLIT,
    JScrollPane(table0),
    JScrollPane(table1),
  ).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(model: TableModel) = object : JTable(model) {
  override fun updateUI() {
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    updateRenderer()
    val editor = DefaultCellEditor(BooleanCellEditor())
    setDefaultEditor(Boolean::class.javaObjectType, editor)
  }

  private fun updateRenderer() {
    val m = getModel()
    for (i in 0..<m.columnCount) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    c.background = getSelectionBackground()
    if (c is JCheckBox) {
      c.isBorderPainted = true
      c.horizontalAlignment = SwingConstants.CENTER
    }
    return c
  }
}

private class BooleanCellEditor : JCheckBox() {
  private var handler: MouseAdapter? = null

  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    isOpaque = true
    handler = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        val c = e.component
        val table = SwingUtilities.getAncestorOfClass(JTable::class.java, c) as? JTable
        (e.component as? JCheckBox)?.also { cb ->
          val m = cb.model
          val editingRow = table?.editingRow ?: return
          if (m.isPressed && table.isRowSelected(editingRow) && e.isControlDown) {
            if (editingRow % 2 == 0) {
              cb.isOpaque = false
            } else {
              cb.isOpaque = true
              cb.background = UIManager.getColor("Table.alternateRowColor")
            }
          } else {
            cb.background = table.selectionBackground
            cb.isOpaque = true
          }
        }
      }

      override fun mouseExited(e: MouseEvent) {
        val table = SwingUtilities.getAncestorOfClass(JTable::class.java, e.component)
        if (table is JTable && table.isEditing && !table.cellEditor.stopCellEditing()) {
          table.cellEditor.cancelCellEditing()
        }
      }
    }
    addMouseListener(handler)
  }
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
