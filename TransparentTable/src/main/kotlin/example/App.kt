package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

private val alphaZero = Color(0x0, true)
private val color = Color(255, 0, 0, 50)

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      super.updateUI()
      val m = getModel()
      for (i in 0 until m.columnCount) {
        (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
          SwingUtilities.updateComponentTreeUI(it)
        }
      }
    }

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      if (c is JTextField) {
        c.isOpaque = false
      } else if (c is JCheckBox) {
        c.setBackground(getSelectionBackground())
      }
      return c
    }
  }
  table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.setDefaultRenderer(java.lang.Boolean::class.java, TranslucentBooleanRenderer())
  table.isOpaque = false
  table.background = alphaZero

  val texture = makeImageTexture()
  val scroll = object : JScrollPane(table) {
    override fun createViewport() = object : JViewport() {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = texture
        g2.fillRect(0, 0, width, height)
        g2.dispose()
        super.paintComponent(g)
      }
    }
  }
  scroll.viewport.isOpaque = false
  scroll.viewport.background = alphaZero

  val check = JCheckBox("setBackground(new Color(255, 0, 0, 50))")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected ?: false
    table.background = if (b) color else alphaZero
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeImageTexture(): TexturePaint {
  // unkaku_w.png http://www.viva-edo.com/komon/edokomon.html
  val path = "example/unkaku_w.png"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val bi = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  return TexturePaint(bi, Rectangle(bi.width, bi.height))
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class TranslucentBooleanRenderer : JCheckBox(), TableCellRenderer {
  override fun updateUI() {
    super.updateUI()
    horizontalAlignment = SwingConstants.CENTER
    isBorderPainted = true
    border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    horizontalAlignment = SwingConstants.CENTER
    if (isSelected) {
      isOpaque = true
      foreground = table.selectionForeground
      super.setBackground(table.selectionBackground)
    } else {
      isOpaque = false
      foreground = table.foreground
      background = table.background
    }
    setSelected(value == true)
    return this
  }

  override fun paintComponent(g: Graphics) {
    if (!isOpaque) {
      g.color = background
      g.fillRect(0, 0, width, height)
    }
    super.paintComponent(g)
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
