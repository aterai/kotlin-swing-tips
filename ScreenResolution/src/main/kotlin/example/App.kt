package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight((getRowHeight() * dpiScaling).toInt())
    }
  }
  table.autoCreateRowSorter = true

  val tree = object : JTree() {
    override fun updateUI() {
      super.updateUI()
      if (isFixedRowHeight) {
        setRowHeight((getRowHeight() * dpiScaling).toInt())
        // println("Tree.rowHeight: " + getRowHeight())
      }
    }
  }

  val split = JSplitPane().also {
    it.leftComponent = JScrollPane(table)
    it.rightComponent = JScrollPane(tree)
    it.resizeWeight = .5
  }

  val p = object : JPanel(BorderLayout()) {
    private val defaultSize = Dimension(320, 240)
    private var resolutionSize: Dimension? = null

    override fun getPreferredSize(): Dimension? {
      if (resolutionSize == null) {
        val sot = dpiScaling
        val w = (defaultSize.width * sot).toInt()
        val h = (defaultSize.height * sot).toInt()
        resolutionSize = Dimension(w, h)
      }
      preferredSize = resolutionSize
      return resolutionSize
    }
  }
  p.add(split)
  return p
}

val dpiScaling: Float
  get() {
    val sr = Toolkit.getDefaultToolkit().screenResolution
    val dpi = if (System.getProperty("os.name").startsWith("Windows")) 96f else 72f
    return sr / dpi
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
