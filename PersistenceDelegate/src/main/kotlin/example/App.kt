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
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B")
  val data = arrayOf(arrayOf("aaa", "********"), arrayOf("bbb", "☀☁☂☃"))
  val table = JTable(DefaultTableModel(data, columnNames))
  table.autoCreateRowSorter = true
  val textArea = JTextArea()
  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.resizeWeight = .5
    it.topComponent = JScrollPane(table)
    it.bottomComponent = JScrollPane(textArea)
  }
  val encodeButton = JButton("XMLEncoder")
  encodeButton.addActionListener {
    runCatching {
      val path = File.createTempFile("output", ".xml").toPath()
      XMLEncoder(BufferedOutputStream(Files.newOutputStream(path))).use { xe ->
        val d = DefaultTableModelPersistenceDelegate()
        xe.setPersistenceDelegate(DefaultTableModel::class.java, d)
        xe.writeObject(table.model)
      }
      Files.newBufferedReader(path, StandardCharsets.UTF_8).use { r ->
        textArea.read(r, "temp")
      }
    }.onFailure {
      textArea.text = it.message
    }
  }
  val decodeButton = JButton("XMLDecoder")
  decodeButton.addActionListener {
    val text = textArea.text
    if (text.isNotEmpty()) {
      val bytes = text.toByteArray(StandardCharsets.UTF_8)
      XMLDecoder(BufferedInputStream(ByteArrayInputStream(bytes))).use { xd ->
        (xd.readObject() as? DefaultTableModel)?.also {
          table.model = it
        }
      }
    }
  }
  val clearButton = JButton("clear")
  clearButton.addActionListener { table.model = DefaultTableModel() }
  val p = JPanel()
  p.add(encodeButton)
  p.add(decodeButton)
  p.add(clearButton)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

class DefaultTableModelPersistenceDelegate : DefaultPersistenceDelegate() {
  override fun initialize(
    type: Class<*>?,
    oldInstance: Any,
    newInstance: Any,
    encoder: Encoder,
  ) {
    super.initialize(type, oldInstance, newInstance, encoder)
    val m = oldInstance as? DefaultTableModel ?: return
    for (row in 0 until m.rowCount) {
      for (col in 0 until m.columnCount) {
        val o = arrayOf(m.getValueAt(row, col), row, col)
        encoder.writeStatement(Statement(oldInstance, "setValueAt", o))
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
