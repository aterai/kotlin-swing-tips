package example

import java.awt.*
import java.awt.event.MouseWheelEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel

private val TEXT = """
  aaa
  a
  a
  a
  a
  aaa
  a
  a
  a
  aaa
""".trimIndent()

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true), arrayOf("zzz", 6, false),
    arrayOf("bbb", 22, true), arrayOf("nnn", 9, false),
    arrayOf("ccc", 32, true), arrayOf("ooo", 8, false),
    arrayOf("ddd", 42, true), arrayOf("ppp", 9, false),
    arrayOf("eee", 52, true), arrayOf("qqq", 8, false),
    arrayOf("fff", 62, true), arrayOf("rrr", 7, false),
    arrayOf("ggg", 51, true), arrayOf("sss", 6, false),
    arrayOf("hhh", 41, true), arrayOf("ttt", 5, false),
    arrayOf("iii", 51, true), arrayOf("uuu", 4, false),
    arrayOf("jjj", 61, true), arrayOf("vvv", 3, false),
    arrayOf("kkk", 72, true), arrayOf("www", 2, false),
    arrayOf("lll", 82, true), arrayOf("xxx", 1, false),
    arrayOf("mmm", 92, true), arrayOf("yyy", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  val textPane = JTextPane()
  textPane.isEditable = false
  textPane.margin = Insets(5, 10, 5, 5)
  val c = JTextArea(TEXT)
  c.isEditable = false
  val doc = textPane.document
  runCatching {
    doc.insertString(doc.length, TEXT, null)
    doc.insertString(doc.length, TEXT, null)
    doc.insertString(doc.length, TEXT, null)
    textPane.insertComponent(createChildScrollPane(c))
    doc.insertString(doc.length, "\n", null)
    doc.insertString(doc.length, TEXT, null)
    textPane.insertComponent(createChildScrollPane(table))
    doc.insertString(doc.length, "\n", null)
    doc.insertString(doc.length, TEXT, null)
    textPane.insertComponent(JScrollPane(JTree()))
    doc.insertString(doc.length, "\n", null)
    doc.insertString(doc.length, TEXT, null)
  }
  return JPanel(BorderLayout()).also {
    it.add(JLayer(JScrollPane(textPane), WheelScrollLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createChildScrollPane(view: Component): JScrollPane {
  return object : JScrollPane(view) {
    override fun getPreferredSize() = Dimension(240, 120)

    override fun getMaximumSize() = super.getMaximumSize().also {
      it.height = preferredSize.height
    }
  }
}

private class WheelScrollLayerUI : LayerUI<JScrollPane>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_WHEEL_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseWheelEvent(e: MouseWheelEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    val dir = e.wheelRotation
    val main = l.view
    if (c is JScrollPane && c != main) {
      val m = c.verticalScrollBar.model
      val extent = m.extent
      val minimum = m.minimum
      val maximum = m.maximum
      val value = m.value
      if (value + extent >= maximum && dir > 0) {
        main.dispatchEvent(SwingUtilities.convertMouseEvent(c, e, main))
      } else if (value <= minimum && dir < 0) {
        main.dispatchEvent(SwingUtilities.convertMouseEvent(c, e, main))
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
