package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val alphaZero = Color(0x0, true)
  private val color = Color(255, 0, 0, 50)

  init {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
      arrayOf("aaa", 12, true),
      arrayOf("bbb", 5, false),
      arrayOf("CCC", 92, true),
      arrayOf("DDD", 0, false))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun isCellEditable(row: Int, column: Int) = column == 2

      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = object : JTable(model) {
      override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int) =
        super.prepareEditor(editor, row, column).also { (it as? JComponent)?.setOpaque(false) }

      override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int) =
        super.prepareRenderer(renderer, row, column).also { it.setForeground(Color.BLACK) }
    }
    // table.setAutoCreateRowSorter(true);
    table.setRowSelectionAllowed(true)
    table.setFillsViewportHeight(true)
    table.setShowVerticalLines(false)
    // table.setShowHorizontalLines(false);
    table.setFocusable(false)
    // table.setCellSelectionEnabled(false);
    table.setIntercellSpacing(Dimension(0, 1))
    table.setRowHeight(24)
    table.setSelectionForeground(table.getForeground())
    table.setSelectionBackground(Color(0, 0, 100, 50))

    val checkBox = object : JCheckBox() {
      override fun paintComponent(g: Graphics) {
        g.setColor(Color(0, 0, 100, 50))
        g.fillRect(0, 0, getWidth(), getHeight())
        super.paintComponent(g)
      }
    }
    checkBox.setOpaque(false)
    checkBox.setHorizontalAlignment(SwingConstants.CENTER)
    table.setDefaultEditor(java.lang.Boolean::class.java, DefaultCellEditor(checkBox))

    table.setDefaultRenderer(Any::class.java, TranslucentObjectRenderer())
    table.setDefaultRenderer(java.lang.Boolean::class.java, TranslucentBooleanRenderer())
    table.setOpaque(false)
    table.setBackground(alphaZero)
    // table.setGridColor(alphaZero);
    table.getTableHeader().setDefaultRenderer(TransparentHeader())
    table.getTableHeader().setOpaque(false)
    table.getTableHeader().setBackground(alphaZero)

    val texture = makeImageTexture()
    val scroll = object : JScrollPane(table) {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setPaint(texture)
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
        super.paintComponent(g)
      }
    }
    scroll.setOpaque(false)
    scroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    scroll.setBackground(alphaZero)
    scroll.getViewport().setOpaque(false)
    scroll.getViewport().setBackground(alphaZero)
    scroll.setColumnHeader(JViewport())
    scroll.getColumnHeader().setOpaque(false)
    scroll.getColumnHeader().setBackground(alphaZero)

    val check = JCheckBox("setBackground(new Color(255, 0, 0, 50))")
    check.addActionListener { e ->
      val b = (e.getSource() as? JCheckBox)?.isSelected() ?: false
      table.setBackground(if (b) color else alphaZero)
    }

    add(check, BorderLayout.NORTH)
    add(scroll)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeImageTexture(): TexturePaint {
    // unkaku_w.png http://www.viva-edo.com/komon/edokomon.html
    val bi = runCatching {
      ImageIO.read(javaClass.getResource("unkaku_w.png"))
    }.getOrNull() ?: makeMissingImage()
    return TexturePaint(bi, Rectangle(bi.getWidth(), bi.getHeight()))
  }

  private fun makeMissingImage(): BufferedImage {
    val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
    val w = missingIcon.getIconWidth()
    val h = missingIcon.getIconHeight()
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    return bi
  }
}

internal class TransparentHeader : JLabel(), TableCellRenderer {
  private val border = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
    BorderFactory.createEmptyBorder(2, 2, 1, 2))
  private val alphaZero = Color(0x0, true)

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    this.setText(value?.toString() ?: "")
    this.setHorizontalAlignment(SwingConstants.CENTER)
    this.setOpaque(false)
    this.setBackground(alphaZero)
    this.setForeground(Color.BLACK)
    this.setBorder(border)
    return this
  }
}

internal class TranslucentObjectRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ) = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)?.also {
    // it.setOpaque(true);
    (it as? JComponent)?.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
  }
}

internal class TranslucentBooleanRenderer : JCheckBox(), TableCellRenderer {
  override fun updateUI() {
    super.updateUI()
    setHorizontalAlignment(SwingConstants.CENTER)
    setBorderPainted(true)
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    setOpaque(false)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    setHorizontalAlignment(SwingConstants.CENTER)
    if (isSelected) {
      setForeground(table.getSelectionForeground())
      setBackground(SELECTION_BACKGROUND)
    } else {
      setForeground(table.getForeground())
      setBackground(table.getBackground())
    }
    setSelected(value == true)
    return this
  }

  override fun paintComponent(g: Graphics) {
    if (!isOpaque()) {
      g.setColor(getBackground())
      g.fillRect(0, 0, getWidth(), getHeight())
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
