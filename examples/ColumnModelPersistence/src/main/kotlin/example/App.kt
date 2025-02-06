package example

import java.awt.*
import java.beans.DefaultPersistenceDelegate
import java.beans.Encoder
import java.beans.Statement
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

private val columnNames = arrayOf("A", "B")
private val data = arrayOf(arrayOf("aaa", "ccc ccc"), arrayOf("bbb", "☀☁☂☃"))
private val table = JTable(DefaultTableModel(data, columnNames)).also {
  it.autoCreateRowSorter = true
  it.tableHeader.componentPopupMenu = TableHeaderPopupMenu()
}
private val textArea = JTextArea()
private val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
  it.resizeWeight = .5
  it.topComponent = JScrollPane(table)
  it.bottomComponent = JScrollPane(textArea)
}
private val encButton = JButton("XMLEncoder")
private val decButton = JButton("XMLDecoder")
private val clearButton = JButton("clear")

fun makeUI(): Component {
  encButton.addActionListener {
    runCatching {
      val path = File.createTempFile("output", ".xml").toPath()
      XMLEncoder(BufferedOutputStream(Files.newOutputStream(path))).use {
        val d1 = DefaultPersistenceDelegate(arrayOf("column", "sortOrder"))
        it.setPersistenceDelegate(RowSorter.SortKey::class.java, d1)
        it.writeObject(table.rowSorter.sortKeys)
        val d2 = DefaultTableModelPersistenceDelegate()
        it.setPersistenceDelegate(DefaultTableModel::class.java, d2)
        it.writeObject(table.model)
        val d3 = DefaultTableColumnModelPersistenceDelegate()
        it.setPersistenceDelegate(DefaultTableColumnModel::class.java, d3)
        it.writeObject(table.columnModel)
      }
      Files.newBufferedReader(path, StandardCharsets.UTF_8).use {
        textArea.read(it, "temp")
      }
    }.onFailure {
      it.printStackTrace()
      textArea.text = it.message
    }
  }

  decButton.addActionListener {
    val text = textArea.text
    if (text.isNotEmpty()) {
      val bytes = text.toByteArray(StandardCharsets.UTF_8)
      XMLDecoder(BufferedInputStream(ByteArrayInputStream(bytes))).use { xd ->
        val keys = (xd.readObject() as? List<*>)?.filterIsInstance<RowSorter.SortKey>()
        val model = xd.readObject() as? DefaultTableModel
        if (keys != null && model != null) {
          table.model = model
          table.autoCreateRowSorter = true
          table.rowSorter.sortKeys = keys
          table.columnModel = xd.readObject() as? DefaultTableColumnModel
        }
      }
    }
  }

  clearButton.addActionListener { table.model = DefaultTableModel() }

  val p = JPanel()
  p.add(encButton)
  p.add(decButton)
  p.add(clearButton)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DefaultTableModelPersistenceDelegate : DefaultPersistenceDelegate() {
  override fun initialize(
    type: Class<*>,
    oldInstance: Any,
    newInstance: Any,
    encoder: Encoder,
  ) {
    super.initialize(type, oldInstance, newInstance, encoder)
    (oldInstance as? DefaultTableModel)?.also { m ->
      for (row in 0..<m.rowCount) {
        for (col in 0..<m.columnCount) {
          val o = arrayOf(m.getValueAt(row, col), row, col)
          encoder.writeStatement(Statement(oldInstance, "setValueAt", o))
        }
      }
    }
  }
}

private class DefaultTableColumnModelPersistenceDelegate : DefaultPersistenceDelegate() {
  override fun initialize(
    type: Class<*>,
    oldInstance: Any,
    newInstance: Any,
    encoder: Encoder,
  ) {
    super.initialize(type, oldInstance, newInstance, encoder)
    (oldInstance as? DefaultTableColumnModel)?.also { m ->
      for (col in 0..<m.columnCount) {
        val o = arrayOf(m.getColumn(col))
        encoder.writeStatement(Statement(oldInstance, "addColumn", o))
      }
    }
  }
}

private class TableHeaderPopupMenu : JPopupMenu() {
  private var index = -1

  init {
    val textField = JTextField()
    textField.addAncestorListener(FocusAncestorListener())
    add("Edit: setHeaderValue").addActionListener {
      (invoker as? JTableHeader)?.also {
        val column = it.columnModel.getColumn(index)
        val name = column.headerValue.toString()
        textField.text = name
        val p = it.rootPane
        val ret = JOptionPane.showConfirmDialog(
          p,
          textField,
          "edit",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE,
        )
        if (ret == JOptionPane.OK_OPTION) {
          val str = textField.text.trim()
          if (str != name) {
            column.headerValue = str
            it.repaint(it.getHeaderRect(index))
          }
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    (c as? JTableHeader)?.also { header ->
      header.draggedColumn = null
      header.repaint()
      header.table.repaint()
      index = header.columnAtPoint(Point(x, y))
      if (index >= 0) {
        super.show(c, x, y)
      }
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    // not needed
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    // not needed
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
