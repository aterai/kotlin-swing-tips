package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TableModelEvent
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

private const val KEY_COL = 0
private const val COLOR_COL = 1

fun makeUI(): Component {
  val columnNames = arrayOf("Key", "Color")
  val data = arrayOf(
    arrayOf("activeCaption", UIManager.getColor("activeCaption")),
    arrayOf("activeCaptionBorder", UIManager.getColor("activeCaptionBorder")),
    arrayOf("activeCaptionText", UIManager.getColor("activeCaptionText")),
    arrayOf("control", UIManager.getColor("control")),
    arrayOf("controlDkShadow", UIManager.getColor("controlDkShadow")),
    arrayOf("controlHighlight", UIManager.getColor("controlHighlight")),
    arrayOf("controlLtHighlight", UIManager.getColor("controlLtHighlight")),
    arrayOf("controlShadow", UIManager.getColor("controlShadow")),
    arrayOf("controlText", UIManager.getColor("controlText")),
    arrayOf("desktop", UIManager.getColor("desktop")),
    arrayOf("inactiveCaption", UIManager.getColor("inactiveCaption")),
    arrayOf("inactiveCaptionBorder", UIManager.getColor("inactiveCaptionBorder")),
    arrayOf("inactiveCaptionText", UIManager.getColor("inactiveCaptionText")),
    arrayOf("info", UIManager.getColor("info")),
    arrayOf("infoText", UIManager.getColor("infoText")),
    arrayOf("menu", UIManager.getColor("menu")),
    arrayOf("menuPressedItemB", UIManager.getColor("menuPressedItemB")),
    arrayOf("menuPressedItemF", UIManager.getColor("menuPressedItemF")),
    arrayOf("menuText", UIManager.getColor("menuText")),
    arrayOf("scrollbar", UIManager.getColor("scrollbar")),
    arrayOf("text", UIManager.getColor("text")),
    arrayOf("textHighlight", UIManager.getColor("textHighlight")),
    arrayOf("textHighlightText", UIManager.getColor("textHighlightText")),
    arrayOf("textInactiveText", UIManager.getColor("textInactiveText")),
    arrayOf("textText", UIManager.getColor("textText")),
    arrayOf("window", UIManager.getColor("window")),
    arrayOf("windowBorder", UIManager.getColor("windowBorder")),
    arrayOf("windowText", UIManager.getColor("windowText"))
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int) = column == COLOR_COL

    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  val table = JTable(model)
  table.setDefaultRenderer(Color::class.java, ColorRenderer())
  table.setDefaultEditor(Color::class.java, ColorEditor())
  model.addTableModelListener { e: TableModelEvent ->
    if (e.type == TableModelEvent.UPDATE && e.column == COLOR_COL) {
      val row = e.firstRow
      val key = model.getValueAt(row, KEY_COL).toString()
      val color = model.getValueAt(row, COLOR_COL) as? Color
      UIManager.put(key, ColorUIResource(color))
      EventQueue.invokeLater {
        table.topLevelAncestor?.also { SwingUtilities.updateComponentTreeUI(it) }
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColorRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (l is JLabel && value is Color) {
      l.icon = ColorIcon(value)
      l.text = "(${value.red}, ${value.green}, ${value.blue})"
    }
    return l
  }
}

private class ColorEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
  private val button = JButton()
  private val colorChooser: JColorChooser
  private val dialog: JDialog
  private var currentColor: Color? = null

  /**
   * Handles events from the editor button and from
   * the dialog's OK button.
   */
  override fun actionPerformed(e: ActionEvent) {
    if (EDIT == e.actionCommand) {
      // The user has clicked the cell, so
      // bring up the dialog.
      button.background = currentColor
      button.icon = ColorIcon(currentColor)
      colorChooser.color = currentColor
      dialog.isVisible = true

      // Make the renderer reappear.
      fireEditingStopped()
    } else { // User pressed dialog's "OK" button.
      currentColor = colorChooser.color
    }
  }

  // Implement the one CellEditor method that AbstractCellEditor doesn't.
  override fun getCellEditorValue(): Any? = currentColor

  // Implement the one method defined by TableCellEditor.
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    currentColor = (value as? Color)?.also {
      button.text = "(${it.red}, ${it.green}, ${it.blue})"
    }
    button.icon = ColorIcon(currentColor)
    return button
  }

  companion object {
    private const val EDIT = "edit"
  }

  init {
    // Set up the editor (from the table's point of view),
    // which is a button.
    // This button brings up the color chooser dialog,
    // which is the editor from the user's point of view.
    button.actionCommand = EDIT
    button.addActionListener(this)
    // button.setBorderPainted(false);
    button.isContentAreaFilled = false
    button.isFocusPainted = false
    button.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    button.isOpaque = false
    button.horizontalAlignment = SwingConstants.LEFT
    button.horizontalTextPosition = SwingConstants.RIGHT

    // Set up the dialog that the button brings up.
    colorChooser = JColorChooser()
    dialog = JColorChooser.createDialog(button, "Pick a Color", true, colorChooser, this, null)
  }
}

private class ColorIcon(private val color: Color?) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 10

  override fun getIconHeight() = 10
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
