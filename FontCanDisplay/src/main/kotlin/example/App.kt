package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val code = 0x1F512
  val label = JLabel(String(Character.toChars(code)))
  label.font = label.font.deriveFont(24f)
  label.horizontalAlignment = SwingConstants.CENTER
  label.verticalAlignment = SwingConstants.CENTER
  val columnNames = arrayOf("family", "name", "postscript name", "canDisplay", "isEmpty")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false

    override fun getColumnClass(column: Int) =
      if (column > 2) Boolean::class.javaObjectType else String::class.java
  }
  val table = JTable(model)
  val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  fonts.map {
    val txt = String(Character.toChars(code))
    val frc = label.getFontMetrics(it).fontRenderContext
    arrayOf(
      it.family,
      it.name,
      it.psName,
      it.canDisplay(code),
      it.createGlyphVector(frc, txt).visualBounds.isEmpty,
    )
  }.forEach { model.addRow(it) }
  table.selectionModel.addListSelectionListener { e ->
    if (!e.valueIsAdjusting && table.selectedRowCount == 1) {
      label.font = fonts[table.selectedRow].deriveFont(24f)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(label, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
