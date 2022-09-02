package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
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
    override fun isCellEditable(row: Int, column: Int) = column == 2

    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int) =
      super.prepareEditor(editor, row, column).also { (it as? JComponent)?.isOpaque = false }

    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(renderer, row, column).also { it.foreground = Color.BLACK }
  }
  // table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.showVerticalLines = false
  // table.showHorizontalLines = false
  table.isFocusable = false
  // table.cellSelectionEnabled = false
  table.intercellSpacing = Dimension(0, 1)
  table.rowHeight = 24
  table.selectionForeground = table.foreground
  table.selectionBackground = Color(0, 0, 100, 50)

  val checkBox = object : JCheckBox() {
    override fun paintComponent(g: Graphics) {
      g.color = Color(0, 0, 100, 50)
      g.fillRect(0, 0, width, height)
      super.paintComponent(g)
    }
  }
  checkBox.isOpaque = false
  checkBox.horizontalAlignment = SwingConstants.CENTER
  table.setDefaultEditor(Boolean::class.javaObjectType, DefaultCellEditor(checkBox))

  table.setDefaultRenderer(Any::class.java, TranslucentObjectRenderer())
  table.setDefaultRenderer(Boolean::class.javaObjectType, TranslucentBooleanRenderer())
  table.isOpaque = false
  table.background = alphaZero
  // table.gridColor = alphaZero
  table.tableHeader.defaultRenderer = TransparentHeader()
  table.tableHeader.isOpaque = false
  table.tableHeader.background = alphaZero

  val texture = makeImageTexture()
  val scroll = object : JScrollPane(table) {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  scroll.isOpaque = false
  scroll.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  scroll.background = alphaZero
  scroll.viewport.isOpaque = false
  scroll.viewport.background = alphaZero
  scroll.columnHeader = JViewport()
  scroll.columnHeader.isOpaque = false
  scroll.columnHeader.background = alphaZero

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
  // unkaku_w.png https://www.viva-edo.com/komon/edokomon.html
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

private class TransparentHeader : TableCellRenderer {
  private val border = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
    BorderFactory.createEmptyBorder(2, 2, 1, 2)
  )
  private val alphaZero = Color(0x0, true)
  private val cr = DefaultTableCellRenderer()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = cr.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel) {
      c.text = value?.toString() ?: ""
      c.horizontalAlignment = SwingConstants.CENTER
      c.isOpaque = false
      c.background = alphaZero
      c.foreground = Color.BLACK
      c.setBorder(border)
    }
    return c
  }
}

private class TranslucentObjectRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ) = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)?.also {
    // it.setOpaque(true);
    (it as? JComponent)?.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
  }
}

private class TranslucentBooleanRenderer : JCheckBox(), TableCellRenderer {
  override fun updateUI() {
    super.updateUI()
    horizontalAlignment = SwingConstants.CENTER
    isBorderPainted = true
    border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    isOpaque = false
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
      foreground = table.selectionForeground
      background = SELECTION_BACKGROUND
    } else {
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

  companion object {
    private val SELECTION_BACKGROUND = Color(0, 0, 100, 50)
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
