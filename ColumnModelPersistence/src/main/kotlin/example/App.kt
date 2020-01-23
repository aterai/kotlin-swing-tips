package example

import java.awt.* // ktlint-disable no-wildcard-imports
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
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("A", "B")
    val data = arrayOf(arrayOf("aaa", "ccc ccc"), arrayOf("bbb", "☀☁☂☃"))
    val table = JTable(DefaultTableModel(data, columnNames))
    table.autoCreateRowSorter = true
    table.tableHeader.componentPopupMenu = TableHeaderPopupMenu()
    val textArea = JTextArea()
    val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    sp.resizeWeight = .5
    sp.topComponent = JScrollPane(table)
    sp.bottomComponent = JScrollPane(textArea)
    val encButton = JButton("XMLEncoder")
    encButton.addActionListener {
      runCatching {
        val file = File.createTempFile("output", ".xml")
        XMLEncoder(BufferedOutputStream(Files.newOutputStream(file.toPath()))).use { xe ->
          val constructorPropertyNames = arrayOf("column", "sortOrder")
          xe.setPersistenceDelegate(RowSorter.SortKey::class.java, DefaultPersistenceDelegate(constructorPropertyNames))
          xe.writeObject(table.rowSorter.sortKeys)
          xe.setPersistenceDelegate(DefaultTableModel::class.java, DefaultTableModelPersistenceDelegate())
          xe.writeObject(table.model)
          xe.setPersistenceDelegate(DefaultTableColumnModel::class.java, DefaultTableColumnModelPersistenceDelegate())
          xe.writeObject(table.columnModel)
        }
        Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)
          .use { r -> textArea.read(r, "temp") }
      }.onFailure {
        it.printStackTrace()
        textArea.text = it.message
      }
    }
    val decButton = JButton("XMLDecoder")
    decButton.addActionListener {
      val text = textArea.text
      if (text.isEmpty()) {
        return@addActionListener
      }
      val bytes = text.toByteArray(StandardCharsets.UTF_8)
      XMLDecoder(BufferedInputStream(ByteArrayInputStream(bytes))).use { xd ->
        @Suppress("UNCHECKED_CAST")
        val keys = xd.readObject() as List<RowSorter.SortKey>
        val model = xd.readObject() as DefaultTableModel
        table.model = model
        table.autoCreateRowSorter = true
        table.rowSorter.sortKeys = keys
        val cm = xd.readObject() as DefaultTableColumnModel
        table.columnModel = cm
      }
    }
    val clearButton = JButton("clear")
    clearButton.addActionListener {
      table.model = DefaultTableModel()
    }
    val p = JPanel()
    p.add(encButton)
    p.add(decButton)
    p.add(clearButton)
    add(sp)
    add(p, BorderLayout.SOUTH)
    preferredSize = Dimension(320, 240)
  }
}

// http://web.archive.org/web/20090806075316/http://java.sun.com/products/jfc/tsc/articles/persistence4/
// http://www.oracle.com/technetwork/java/persistence4-140124.html
// https://ateraimemo.com/Swing/PersistenceDelegate.html
class DefaultTableModelPersistenceDelegate : DefaultPersistenceDelegate() {
  override fun initialize(
    type: Class<*>,
    oldInstance: Any,
    newInstance: Any,
    encoder: Encoder
  ) {
    super.initialize(type, oldInstance, newInstance, encoder)
    (oldInstance as? DefaultTableModel)?.also { m ->
      for (row in 0 until m.rowCount) {
        for (col in 0 until m.columnCount) {
          val o = arrayOf(m.getValueAt(row, col), row, col)
          encoder.writeStatement(Statement(oldInstance, "setValueAt", o))
        }
      }
    }
  }
}

class DefaultTableColumnModelPersistenceDelegate : DefaultPersistenceDelegate() {
  override fun initialize(
    type: Class<*>,
    oldInstance: Any,
    newInstance: Any,
    encoder: Encoder
  ) {
    super.initialize(type, oldInstance, newInstance, encoder)
    (oldInstance as? DefaultTableColumnModel)?.also { m ->
      for (col in 0 until m.columnCount) {
        val o = arrayOf(m.getColumn(col))
        encoder.writeStatement(Statement(oldInstance, "addColumn", o))
      }
    }
  }
}

class TableHeaderPopupMenu : JPopupMenu() {
  private var index = -1
  override fun show(c: Component, x: Int, y: Int) {
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

  init {
    val textField = JTextField()
    textField.addAncestorListener(FocusAncestorListener())
    add("Edit: setHeaderValue").addActionListener {
      val header = invoker as? JTableHeader ?: return@addActionListener
      val column = header.columnModel.getColumn(index)
      val name = column.headerValue.toString()
      textField.text = name
      val p = header.rootPane
      val ret = JOptionPane.showConfirmDialog(
        p, textField, "edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
      )
      if (ret == JOptionPane.OK_OPTION) {
        val str = textField.text.trim()
        if (str != name) {
          column.headerValue = str
          header.repaint(header.getHeaderRect(index))
        }
      }
    }
  }
}

class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
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
