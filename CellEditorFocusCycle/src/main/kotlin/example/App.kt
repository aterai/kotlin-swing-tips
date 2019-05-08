package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.EnumSet
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val table = makeTable()
    val am = table.getActionMap()
    val sncc = am.get("selectNextColumnCell")
    val action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        if (!table.isEditing() || !isEditorFocusCycle(table.getEditorComponent())) {
          sncc.actionPerformed(e)
        }
      }
    }
    am.put("selectNextColumnCell2", action)

    val im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "selectNextColumnCell2")

    add(JScrollPane(makeTable()))
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  protected fun isEditorFocusCycle(editor: Component) =
      CheckBoxesEditor.getEditorFocusCycleAfter(editor)?.let {
        it.requestFocus()
        true
      } ?: false

  private fun makeTable(): JTable {
    val columnNames = arrayOf("user", "rwx")
    val data = arrayOf(
        arrayOf<Any>("owner", EnumSet.allOf(Permissions::class.java)),
        arrayOf<Any>("group", EnumSet.of(Permissions.READ)),
        arrayOf<Any>("other", EnumSet.noneOf(Permissions::class.java)))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    return object : JTable(model) {
      override fun updateUI() {
        super.updateUI()
        // putClientProperty("terminateEditOnFocusLost", Boolean.TRUE)
        setSelectionForeground(Color.BLACK)
        setSelectionBackground(Color(220, 220, 255))
        getColumnModel().getColumn(1)?.also {
          it.setCellRenderer(CheckBoxesRenderer())
          it.setCellEditor(CheckBoxesEditor())
        }
      }
    }
  }
}

internal enum class Permissions {
  EXECUTE, WRITE, READ
}

internal class CheckBoxesPanel : JPanel() {
  val titles = arrayOf("r", "w", "x")
  val buttons = mutableListOf<JCheckBox>()
  var focusBorder: Border? = null
  var noFocusBorder: Border? = null

  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
    setFocusTraversalPolicyProvider(true)
    setFocusCycleRoot(true)
    setLayout(BoxLayout(this, BoxLayout.X_AXIS))
    val fb: Border? = UIManager.getBorder("Table.focusCellHighlightBorder")
    var nfb: Border? = UIManager.getBorder("Table.noFocusBorder")
    if (nfb == null) { // Nimbus???
      val i = fb?.getBorderInsets(this) ?: Insets(1, 1, 1, 1)
      nfb = BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
    }
    this.focusBorder = fb
    this.noFocusBorder = nfb
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

  private fun makeCheckBox(title: String) = JCheckBox(" $title ").also {
    it.setOpaque(false)
    // it.setFocusPainted(false)
    // it.setFocusable(false)
    // it.setRolloverEnabled(false)
  }

  fun updateButtons(v: Any?) {
    initButtons()
    val f = v as? Set<*> ?: EnumSet.noneOf(Permissions::class.java)
    buttons[0].setSelected(f.contains(Permissions.READ))
    buttons[1].setSelected(f.contains(Permissions.WRITE))
    buttons[2].setSelected(f.contains(Permissions.EXECUTE))
  }
}

internal class CheckBoxesRenderer : TableCellRenderer {
  private val renderer = CheckBoxesPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (isSelected) {
      renderer.setOpaque(true)
      renderer.setBackground(table.getSelectionBackground())
    } else {
      renderer.setOpaque(true)
      renderer.setBackground(Color(0x0, true))
    }
    renderer.setBorder(if (hasFocus) renderer.focusBorder else renderer.noFocusBorder)
    renderer.updateButtons(value)
    return renderer
  }
}

internal class CheckBoxesEditor : AbstractCellEditor(), TableCellEditor {
  protected val renderer = CheckBoxesPanel()

  init {
    val am = renderer.getActionMap()
    renderer.titles.forEach { title ->
      am.put(title, object : AbstractAction(title) {
        override fun actionPerformed(e: ActionEvent) {
          renderer.buttons.filter { it.getText().trim() == title }.firstOrNull()?.doClick()
          // fireEditingStopped();
        }
      })
    }

    val im = renderer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), renderer.titles[0])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), renderer.titles[1])
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), renderer.titles[2])
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ) = renderer.also {
    it.setOpaque(true)
    it.setBackground(table.getSelectionBackground())
    it.updateButtons(value)
  }

  override fun getCellEditorValue() = EnumSet.noneOf(Permissions::class.java).also {
    if (renderer.buttons[0].isSelected()) {
      it.add(Permissions.READ)
    }
    if (renderer.buttons[1].isSelected()) {
      it.add(Permissions.WRITE)
    }
    if (renderer.buttons[2].isSelected()) {
      it.add(Permissions.EXECUTE)
    }
  }

  override fun isCellEditable(e: EventObject): Boolean {
    EventQueue.invokeLater {
      getEditorFocusCycleAfter(e.getSource() as Component)?.requestFocus()
    }
    return super.isCellEditable(e)
  }

  companion object {
    fun getEditorFocusCycleAfter(editor: Component): Component? {
      val fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
      val root: Container = (editor as? Container)?.let {
        if (it.isFocusCycleRoot()) it else it.getFocusCycleRootAncestor()
      } ?: return null
      return root.getFocusTraversalPolicy().getComponentAfter(root, fo)?.takeIf {
        SwingUtilities.isDescendingFrom(it, editor)
      }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
