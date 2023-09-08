package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.KeyEvent
import java.util.EventObject
import javax.swing.*

fun makeUI(): Component {
  val table = JTable(8, 4)
  table.columnModel.getColumn(0).cellEditor = CustomComponentCellEditor(JTextField())
  table.columnModel.getColumn(1).cellEditor = CustomCellEditor(JTextField())
  table.columnModel.getColumn(2).cellEditor = CustomComponentCellEditor2(CustomComponent())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class CustomCellEditor(field: JTextField) : DefaultCellEditor(field) {
  private val button = JButton()

  init {
    field.border = BorderFactory.createEmptyBorder(0, 2, 0, BUTTON_WIDTH)
    field.addHierarchyListener { e ->
      val c = e.component
      val b = e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
      if (b && c is JTextField && c.isShowing()) {
        c.removeAll()
        c.add(button)
        val r = c.bounds
        button.setBounds(
          r.width - BUTTON_WIDTH,
          0,
          BUTTON_WIDTH,
          r.height,
        )
      }
    }
  }

  override fun getComponent(): Component {
    SwingUtilities.updateComponentTreeUI(button)
    return super.getComponent()
  }

  companion object {
    private const val BUTTON_WIDTH = 20
  }
}

private class CustomComponentCellEditor(
  private val field: JTextField,
) : DefaultCellEditor(field) {
  private val panel = JPanel(BorderLayout())

  init {
    val button = object : JButton() {
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.width = 25
        return d
      }
    }
    field.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
    panel.add(field)
    panel.add(button, BorderLayout.EAST)
    panel.isFocusable = false
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    field.text = value?.toString() ?: ""
    EventQueue.invokeLater {
      field.caretPosition = field.text.length
      field.requestFocusInWindow()
    }
    return panel
  }

  override fun isCellEditable(e: EventObject): Boolean {
    EventQueue.invokeLater {
      if (e is KeyEvent) {
        val kc = e.keyChar
        // val kc = ke.getKeyCode()
        if (Character.isUnicodeIdentifierStart(kc)) {
          field.text = field.text + kc
        }
      }
    }
    return super.isCellEditable(e)
  }

  override fun getComponent() = panel
}

private class CustomComponent : JPanel(BorderLayout()) {
  val field = JTextField()

  init {
    // this.setFocusable(false)
    this.add(field)
    val button = JButton()
    this.add(button, BorderLayout.EAST)
  }

  override fun processKeyBinding(
    ks: KeyStroke,
    e: KeyEvent,
    condition: Int,
    pressed: Boolean,
  ): Boolean {
    if (!field.isFocusOwner && !pressed) {
      field.requestFocusInWindow()
      EventQueue.invokeLater {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(field, e)
      }
    }
    return super.processKeyBinding(ks, e, condition, pressed)
    // field.requestFocusInWindow()
    // return field.processKeyBinding(ks, e, condition, pressed)
  }
}

private class CustomComponentCellEditor2(
  private val component: CustomComponent,
) : DefaultCellEditor(component.field) {
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    component.field.text = value?.toString() ?: ""
    return component
  }

  override fun getComponent() = component
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
