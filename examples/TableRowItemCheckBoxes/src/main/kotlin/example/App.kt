package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(JScrollPane(JTable(makeModel())))
  it.add(JScrollPane(CheckBoxTable(makeModel())))
  it.preferredSize = Dimension(320, 240)
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "CheckBox")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, false),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, false),
    arrayOf("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private class CheckBoxTable(
  model: TableModel,
) : JTable(model) {
  private var handler: MouseListener? = null
  private var listener: ListSelectionListener? = null
  private var checkedIndex = -1

  override fun updateUI() {
    addMouseListener(handler)
    getSelectionModel().removeListSelectionListener(listener)
    super.updateUI()
    setSelectionModel(CheckBoxListSelectionModel())
    handler = CheckBoxListener()
    addMouseListener(handler)
    listener = ListSelectionListener {
      val sm = it.source
      if (checkedIndex < 0 && sm is ListSelectionModel) {
        for (i in 0..<rowCount) {
          model.setValueAt(sm.isSelectedIndex(i), i, CHECKBOX_COLUMN)
        }
      }
    }
    getSelectionModel().addListSelectionListener(listener)
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    if (c is JCheckBox) {
      val selected = getSelectionModel().isSelectedIndex(row)
      c.background = if (selected) background else getSelectionBackground()
      checkedIndex = row
    }
    return c
  }

  private inner class CheckBoxListSelectionModel : DefaultListSelectionModel() {
    override fun setSelectionInterval(anchor: Int, lead: Int) {
      if (checkedIndex < 0) {
        super.setSelectionInterval(anchor, lead)
      } else {
        EventQueue.invokeLater {
          if (checkedIndex >= 0 && lead == anchor && checkedIndex == anchor) {
            super.addSelectionInterval(checkedIndex, checkedIndex)
          } else {
            super.setSelectionInterval(anchor, lead)
          }
        }
      }
    }

    override fun removeSelectionInterval(index0: Int, index1: Int) {
      if (checkedIndex < 0) {
        super.removeSelectionInterval(index0, index1)
      } else {
        EventQueue.invokeLater { super.removeSelectionInterval(index0, index1) }
      }
    }
  }

  private inner class CheckBoxListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val table = e.component as? JTable ?: return
      val pt = e.point
      if (table.columnAtPoint(pt) == CHECKBOX_COLUMN) {
        val row = table.rowAtPoint(pt)
        checkedIndex = row
        val sm = table.selectionModel
        if (sm.isSelectedIndex(row)) {
          sm.removeSelectionInterval(row, row)
        } else {
          sm.addSelectionInterval(row, row)
        }
      } else {
        checkedIndex = -1
      }
      table.repaint()
    }
  }

  companion object {
    private const val CHECKBOX_COLUMN = 2
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
