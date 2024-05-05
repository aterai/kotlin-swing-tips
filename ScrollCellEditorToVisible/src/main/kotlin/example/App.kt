package example

import java.awt.*
import javax.swing.*
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val table1 = JTable(50, 50)
  table1.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val table2: JTable = object : JTable(50, 50) {
    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ): Component {
      val r = getCellRect(row, column, true)
      val p = SwingUtilities.getAncestorOfClass(JViewport::class.java, this)
      if (p is JViewport) {
        val viewRect = p.viewRect
        if (viewRect.intersects(r)) {
          r.grow(r.width / 4, 0)
        } else {
          r.grow((viewRect.width - r.width) / 2, 0)
        }
        scrollRectToVisible(r)
      }
      return super.prepareEditor(editor, row, column)
    }

    override fun changeSelection(
      rowIndex: Int,
      columnIndex: Int,
      toggle: Boolean,
      extend: Boolean,
    ) {
      super.changeSelection(rowIndex, columnIndex, toggle, extend)
      val r = getCellRect(rowIndex, columnIndex, true)
      r.grow(r.width / 4, 0)
      scrollRectToVisible(r)
    }
  }
  table2.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val help1 = "Default: F2:startEditing not scroll"
  val s1 = makeTitledPane(JScrollPane(table1), help1)
  val help2 = "F2:startEditing scrollRectToVisible(...)"
  val s2 = makeTitledPane(JScrollPane(table2), help2)
  val split = JSplitPane(JSplitPane.VERTICAL_SPLIT, s1, s2)
  split.resizeWeight = .5

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPane(c: Component, title: String): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
