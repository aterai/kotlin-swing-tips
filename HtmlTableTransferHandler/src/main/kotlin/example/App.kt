package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.lang.StringBuilder
import java.nio.charset.Charset
import java.util.Date
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("Type", "Value")
  val data = arrayOf(
    arrayOf<Any>("String", "text"),
    arrayOf<Any>("Date", Date()),
    arrayOf<Any>("Integer", 12),
    arrayOf<Any>("Double", 3.45),
    arrayOf<Any>("Boolean", true),
    arrayOf<Any>("Color", Color.RED)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table1 = PropertyTable(model)
  val table2 = PropertyTable(model)
  table2.transferHandler = HtmlTableTransferHandler()

  return JPanel(GridLayout(3, 1)).also {
    it.add(JScrollPane(table1))
    it.add(JScrollPane(table2))
    it.add(JScrollPane(JEditorPane("text/html", "")))
    it.preferredSize = Dimension(320, 240)
  }
}

private class PropertyTable(model: TableModel?) : JTable(model) {
  private var editingClass: Class<*>? = null

  private fun getClassAt(row: Int, column: Int): Class<*> {
    val mc = convertColumnIndexToModel(column)
    val mr = convertRowIndexToModel(row)
    return model.getValueAt(mr, mc).javaClass
  }

  override fun updateUI() {
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    setDefaultRenderer(Color::class.java, ColorRenderer())
    setDefaultEditor(Color::class.java, ColorEditor())
    setDefaultEditor(Date::class.java, DateEditor())
  }

  override fun getCellRenderer(row: Int, column: Int): TableCellRenderer =
    if (convertColumnIndexToModel(column) == TARGET_COL_IDX) {
      getDefaultRenderer(getClassAt(row, column))
    } else {
      super.getCellRenderer(row, column)
    }

  override fun getCellEditor(row: Int, column: Int): TableCellEditor =
    if (convertColumnIndexToModel(column) == TARGET_COL_IDX) {
      editingClass = getClassAt(row, column)
      getDefaultEditor(editingClass)
    } else {
      editingClass = null
      super.getCellEditor(row, column)
    }

  override fun getColumnClass(column: Int) = if (convertColumnIndexToModel(column) == TARGET_COL_IDX) {
    editingClass
  } else {
    super.getColumnClass(column)
  }

  companion object {
    private const val TARGET_COL_IDX = 1
  }
}

private class DateEditor : AbstractCellEditor(), TableCellEditor {
  private val spinner = JSpinner(SpinnerDateModel())

  init {
    val editor = JSpinner.DateEditor(spinner, "yyyy/MM/dd")
    spinner.editor = editor
    setArrowButtonEnabled(false)
    editor.textField.horizontalAlignment = SwingConstants.LEFT
    editor.textField.addFocusListener(object : FocusListener {
      override fun focusLost(e: FocusEvent) {
        setArrowButtonEnabled(false)
      }

      override fun focusGained(e: FocusEvent) {
        setArrowButtonEnabled(true)
        EventQueue.invokeLater {
          (e.component as? JTextField)?.also {
            it.caretPosition = 8
            it.selectionStart = 8
            it.selectionEnd = 10
          }
        }
      }
    })
    spinner.border = BorderFactory.createEmptyBorder()
  }

  private fun setArrowButtonEnabled(flag: Boolean) {
    for (c in spinner.components) {
      (c as? JButton)?.isEnabled = flag
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    spinner.value = value
    return spinner
  }

  override fun getCellEditorValue(): Any = spinner.value

  override fun stopCellEditing(): Boolean {
    runCatching {
      spinner.commitEdit()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
      return false
    }
    return super.stopCellEditing()
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
    val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (value is Color && c is JLabel) {
      c.icon = ColorIcon(value)
      c.text = "(${value.red}, ${value.green}, ${value.blue})"
    }
    return c
  }
}

private class ColorEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
  private val button = JButton()
  private val colorChooser: JColorChooser
  private val dialog: JDialog
  private var currentColor: Color? = null

  init {
    button.actionCommand = EDIT
    button.addActionListener(this)
    button.isContentAreaFilled = false
    button.isFocusPainted = false
    button.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    button.isOpaque = false
    button.horizontalAlignment = SwingConstants.LEFT
    button.horizontalTextPosition = SwingConstants.RIGHT
    colorChooser = JColorChooser()
    dialog = JColorChooser.createDialog(button, "Pick a Color", true, colorChooser, this, null)
  }

  override fun actionPerformed(e: ActionEvent) {
    if (EDIT == e.actionCommand) {
      button.background = currentColor
      button.icon = ColorIcon(currentColor)
      colorChooser.color = currentColor
      dialog.isVisible = true
      fireEditingStopped()
    } else {
      currentColor = colorChooser.color
    }
  }

  override fun getCellEditorValue(): Any? = currentColor

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    currentColor = (value as? Color)?.also {
      button.icon = ColorIcon(it)
      button.text = "(${it.red}, ${it.green}, ${it.blue})"
    }
    return button
  }

  companion object {
    private const val EDIT = "edit"
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

private class HtmlTableTransferHandler : TransferHandler() {
  fun canStartDrag(c: JComponent?): Boolean {
    if (c is JTable) {
      return c.rowSelectionAllowed || c.columnSelectionAllowed
    }
    return false
  }

  fun appendTag(buf: StringBuilder, o: Any?) {
    when (o) {
      is Date -> buf.append("  <td><time>").append(o.toString()).append("</time></td>\n")
      is Color -> buf.append("  <td style='background-color:#%06X'>&nbsp;</td>%n".format(o.rgb and 0xFF_FF_FF))
      else -> buf.append("  <td>").append(o?.toString() ?: "").append("</td>\n")
    }
  }

  override fun createTransferable(c: JComponent): Transferable? {
    return if (canStartDrag(c) && c is JTable) {
      val rows = getSelectedRows(c)
      val cols = getSelectedColumns(c)
      if (rows.isEmpty() || cols.isEmpty()) {
        return null
      }
      val plainBuf = StringBuilder()
      val htmlBuf = StringBuilder(64)
      htmlBuf.append("<html>\n<body>\n<table border='1'>\n")
      for (row in rows) {
        htmlBuf.append("<tr>\n")
        for (col in cols) {
          val obj = c.getValueAt(row, col)
          val v = obj?.toString() ?: ""
          plainBuf.append(v + "\t")
          appendTag(htmlBuf, obj)
        }
        plainBuf.deleteCharAt(plainBuf.length - 1).append('\n')
        htmlBuf.append("</tr>\n")
      }

      plainBuf.deleteCharAt(plainBuf.length - 1)
      htmlBuf.append("</table>\n</body>\n</html>")
      BasicTransferable(plainBuf.toString(), htmlBuf.toString())
    } else {
      null
    }
  }

  override fun getSourceActions(c: JComponent) = COPY

  private fun getSelectedRows(table: JTable): IntArray {
    val rows: IntArray
    if (table.rowSelectionAllowed) {
      rows = table.selectedRows
    } else {
      val rowCount = table.rowCount
      rows = IntArray(rowCount)
      for (counter in 0 until rowCount) {
        rows[counter] = counter
      }
    }
    return rows
  }

  private fun getSelectedColumns(table: JTable): IntArray {
    val cols: IntArray
    if (table.columnSelectionAllowed) {
      cols = table.selectedColumns
    } else {
      val colCount = table.columnCount
      cols = IntArray(colCount)
      for (counter in 0 until colCount) {
        cols[counter] = counter
      }
    }
    return cols
  }
}

private class BasicTransferable(
  private var plainData: String,
  private var htmlData: String
) : Transferable {
  private val htmlFlavors = arrayOf(
    DataFlavor("text/html;class=java.lang.String"),
    DataFlavor("text/html;class=java.io.Reader"),
    DataFlavor("text/html;charset=unicode;class=java.io.InputStream")
  )
  private val plainFlavors = arrayOf(
    DataFlavor("text/plain;class=java.lang.String"),
    DataFlavor("text/plain;class=java.io.Reader"),
    DataFlavor("text/plain;charset=unicode;class=java.io.InputStream")
  )
  private val stringFlavors = arrayOf(
    DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.String"),
    DataFlavor.stringFlavor
  )
  private val getRicherData get() = null
  private val richerFlavors: Array<DataFlavor> get() = arrayOf()
  private val isHtmlSupported = true
  private val isPlainSupported = true

  fun getTextCharset(flavor: DataFlavor): String =
    flavor.getParameter("charset") ?: Charset.defaultCharset().name()

  override fun getTransferDataFlavors(): Array<DataFlavor> {
    val flavors = mutableListOf<DataFlavor>()
    if (isHtmlSupported) {
      flavors.addAll(htmlFlavors)
    }
    if (isPlainSupported) {
      flavors.addAll(plainFlavors)
      flavors.addAll(stringFlavors)
    }
    return flavors.toTypedArray()
  }

  override fun isDataFlavorSupported(flavor: DataFlavor) = transferDataFlavors.any { it.equals(flavor) }

  @Throws(UnsupportedFlavorException::class, IOException::class)
  override fun getTransferData(flavor: DataFlavor): Any? {
    return when {
      richerFlavors.any { it.equals(flavor) } -> getRicherData
      htmlFlavors.any { it.equals(flavor) } -> getHtmlTransferData(flavor)
      plainFlavors.any { it.equals(flavor) } -> getPlaneTransferData(flavor)
      stringFlavors.any { it.equals(flavor) } -> plainData
      else -> UnsupportedFlavorException(flavor)
    }
  }

  private fun createInputStream(flavor: DataFlavor, data: String) =
    ByteArrayInputStream(data.toByteArray(charset(getTextCharset(flavor))))

  @Throws(IOException::class, UnsupportedFlavorException::class)
  private fun getHtmlTransferData(flavor: DataFlavor): Any = when (flavor.representationClass) {
    String::class.java -> htmlData
    Reader::class.java -> StringReader(htmlData)
    InputStream::class.java -> createInputStream(flavor, htmlData)
    else -> UnsupportedFlavorException(flavor)
  }

  @Throws(IOException::class, UnsupportedFlavorException::class)
  fun getPlaneTransferData(flavor: DataFlavor): Any = when (flavor.representationClass) {
    String::class.java -> plainData
    Reader::class.java -> StringReader(plainData)
    InputStream::class.java -> createInputStream(flavor, plainData)
    else -> UnsupportedFlavorException(flavor)
  }

  // private fun isRicherFlavor(flavor: DataFlavor) = richerFlavors.any { it.equals(flavor) }

  // private fun isHtmlFlavor(flavor: DataFlavor) = htmlFlavors.any { it.equals(flavor) }

  // private fun isPlainFlavor(flavor: DataFlavor) = plainFlavors.any { it.equals(flavor) }

  // private fun isStringFlavor(flavor: DataFlavor) = stringFlavors.any { it.equals(flavor) }
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
      minimumSize = Dimension(256, 200)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
