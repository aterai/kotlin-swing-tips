package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.EnumSet
import java.util.EventObject
import javax.swing.*
import javax.swing.border.Border
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val table = makeTable()
  val am = table.actionMap
  val selectNextAction = am.get("selectNextColumnCell")
  val action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (!table.isEditing || !isEditorFocusCycle(table.editorComponent)) {
        selectNextAction.actionPerformed(e)
      }
    }
  }
  am.put("selectNextColumnCell2", action)

  val im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "selectNextColumnCell2")

  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(makeTable()))
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun isEditorFocusCycle(editor: Component): Boolean {
  val c = CheckBoxesEditor.getEditorFocusCycleAfter(editor)
  return c != null && c.requestFocusInWindow()
}

private fun makeTable(): JTable {
  val columnNames = arrayOf("user", "rwx")
  val data = arrayOf(
    arrayOf("owner", EnumSet.allOf(Permissions::class.java)),
    arrayOf("group", EnumSet.of(Permissions.READ)),
    arrayOf("other", EnumSet.noneOf(Permissions::class.java))
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  return object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      // putClientProperty("terminateEditOnFocusLost", true)
      setSelectionForeground(Color.BLACK)
      setSelectionBackground(Color(220, 220, 255))
      getColumnModel().getColumn(1)?.also {
        it.cellRenderer = CheckBoxesRenderer()
        it.cellEditor = CheckBoxesEditor()
      }
    }
  }
}

private enum class Permissions {
  EXECUTE, WRITE, READ
}

private class CheckBoxesPanel : JPanel() {
  val titles = arrayOf("r", "w", "x")
  val buttons = mutableListOf<JCheckBox>()
  var focusBorder: Border? = null
  var noFocusBorder: Border? = null

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    isFocusTraversalPolicyProvider = true
    isFocusCycleRoot = true
    layout = BoxLayout(this, BoxLayout.X_AXIS)
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
    it.isOpaque = false
    // it.setFocusPainted(false)
    // it.setFocusable(false)
    // it.setRolloverEnabled(false)
  }

  fun updateButtons(v: Any?) {
    initButtons()
    val f = v as? Set<*> ?: EnumSet.noneOf(Permissions::class.java)
    buttons[0].isSelected = f.contains(Permissions.READ)
    buttons[1].isSelected = f.contains(Permissions.WRITE)
    buttons[2].isSelected = f.contains(Permissions.EXECUTE)
  }
}

private class CheckBoxesRenderer : TableCellRenderer {
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
      renderer.isOpaque = true
      renderer.background = table.selectionBackground
    } else {
      renderer.isOpaque = true
      renderer.background = Color(0x0, true)
    }
    renderer.border = if (hasFocus) renderer.focusBorder else renderer.noFocusBorder
    renderer.updateButtons(value)
    return renderer
  }
}

private class CheckBoxesEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = CheckBoxesPanel()

  init {
    val am = renderer.actionMap
    renderer.titles.forEach { title ->
      val a = object : AbstractAction(title) {
        override fun actionPerformed(e: ActionEvent) {
          renderer.buttons.firstOrNull { it.text.trim() == title }?.doClick()
          // fireEditingStopped()
        }
      }
      am.put(title, a)
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
    it.isOpaque = true
    it.background = table.selectionBackground
    it.updateButtons(value)
  }

  override fun getCellEditorValue(): Any = EnumSet.noneOf(Permissions::class.java).also {
    if (renderer.buttons[0].isSelected) {
      it.add(Permissions.READ)
    }
    if (renderer.buttons[1].isSelected) {
      it.add(Permissions.WRITE)
    }
    if (renderer.buttons[2].isSelected) {
      it.add(Permissions.EXECUTE)
    }
  }

  override fun isCellEditable(e: EventObject): Boolean {
    EventQueue.invokeLater {
      getEditorFocusCycleAfter(e.source as? Component)?.requestFocusInWindow()
    }
    return super.isCellEditable(e)
  }

  companion object {
    fun getEditorFocusCycleAfter(editor: Component?): Component? {
      val fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
      val root = (editor as? Container)?.let {
        if (it.isFocusCycleRoot) it else it.focusCycleRootAncestor
      } ?: return null
      return root.focusTraversalPolicy.getComponentAfter(root, fo)?.takeIf {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
