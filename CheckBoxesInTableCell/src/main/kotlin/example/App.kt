package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("user", "rwx")
    val data = arrayOf(
        arrayOf<Any>("owner", 7),
        arrayOf<Any>("group", 6),
        arrayOf<Any>("other", 5))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = object : JTable(model) {
      override fun updateUI() {
        super.updateUI()
        getColumnModel().getColumn(1).setCellRenderer(CheckBoxesRenderer())
        getColumnModel().getColumn(1).setCellEditor(CheckBoxesEditor())
      }
    }
    table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)

    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

open class CheckBoxesPanel : JPanel() {
  private val bgc = Color(0x0, true)
  protected val titles = arrayOf("r", "w", "x")
  val buttons = mutableListOf<JCheckBox>()

  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
    setBackground(bgc)
    setLayout(BoxLayout(this, BoxLayout.X_AXIS))
    EventQueue.invokeLater { initButtons() }
  }

  private fun initButtons() {
    removeAll()
    buttons.clear()
    for (t in titles) {
      val b = makeCheckBox(t)
      buttons.add(b)
      add(b)
      add(Box.createHorizontalStrut(5))
    }
  }

  fun updateButtons(v: Any) {
    initButtons()
    val i = v as? Int ?: 0
    buttons[0].setSelected(i and (1 shl 2) != 0)
    buttons[1].setSelected(i and (1 shl 1) != 0)
    buttons[2].setSelected(i and (1 shl 0) != 0)
  }

  private fun makeCheckBox(title: String) = JCheckBox(title).also {
    it.setOpaque(false)
    it.setFocusable(false)
    it.setRolloverEnabled(false)
    it.setBackground(bgc)
  }
}

class CheckBoxesRenderer : CheckBoxesPanel(), TableCellRenderer {
  override fun updateUI() {
    super.updateUI()
    setName("Table.cellRenderer")
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    updateButtons(value)
    return this
  }
  // public static class UIResource extends CheckBoxesRenderer implements UIResource {}
}

class CheckBoxesEditor : AbstractCellEditor(), TableCellEditor {
  private val panel = object : CheckBoxesPanel() {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        val am = getActionMap()
        for (i in buttons.indices) {
          val t = titles[i]
          am.put(t, object : AbstractAction(t) {
            override fun actionPerformed(e: ActionEvent) {
              buttons.filter { it.getText() == t }.first().let { it.doClick() }
              fireEditingStopped()
            }
          })
        }
        val im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), titles[0])
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), titles[1])
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), titles[2])
      }
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    panel.updateButtons(value)
    return panel
  }

  override fun getCellEditorValue(): Any {
    var i = 0
    i = if (panel.buttons.get(0).isSelected()) 1 shl 2 or i else i
    i = if (panel.buttons.get(1).isSelected()) 1 shl 1 or i else i
    i = if (panel.buttons.get(2).isSelected()) 1 shl 0 or i else i
    return i
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
