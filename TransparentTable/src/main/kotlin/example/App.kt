package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val alphaZero = Color(0x0, true)
  private val color = Color(255, 0, 0, 50)

  init {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
        arrayOf<Any>("aaa", 12, true),
        arrayOf<Any>("bbb", 5, false),
        arrayOf<Any>("CCC", 92, true),
        arrayOf<Any>("DDD", 0, false))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = object : JTable(model) {
      override fun updateUI() {
        setSelectionForeground(ColorUIResource(Color.RED))
        setSelectionBackground(ColorUIResource(Color.RED))
        super.updateUI()
        val m = getModel()
        for (i in 0 until m.getColumnCount()) {
          (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
            SwingUtilities.updateComponentTreeUI(it)
          }
        }
      }

      override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
        val c = super.prepareEditor(editor, row, column)
        if (c is JTextField) {
          c.setOpaque(false)
        } else if (c is JCheckBox) {
          c.setBackground(getSelectionBackground())
        }
        return c
      }
    }
    table.setAutoCreateRowSorter(true)
    table.setRowSelectionAllowed(true)
    table.setFillsViewportHeight(true)
    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
    table.setDefaultRenderer(java.lang.Boolean::class.java, TranslucentBooleanRenderer())
    table.setOpaque(false)
    table.setBackground(alphaZero)

    val texture = makeImageTexture()
    val scroll = object : JScrollPane(table) {
      protected override fun createViewport() = object : JViewport() {
        protected override fun paintComponent(g: Graphics) {
          val g2 = g.create() as Graphics2D
          g2.setPaint(texture)
          g2.fillRect(0, 0, getWidth(), getHeight())
          g2.dispose()
          super.paintComponent(g)
        }
      }
    }
    scroll.getViewport().setOpaque(false)
    scroll.getViewport().setBackground(alphaZero)

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

internal class TranslucentBooleanRenderer : JCheckBox(), TableCellRenderer {
  override fun updateUI() {
    super.updateUI()
    setHorizontalAlignment(SwingConstants.CENTER)
    setBorderPainted(true)
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
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
      setOpaque(true)
      setForeground(table.getSelectionForeground())
      super.setBackground(table.getSelectionBackground())
    } else {
      setOpaque(false)
      setForeground(table.getForeground())
      setBackground(table.getBackground())
    }
    setSelected(value == true)
    return this
  }

  protected override fun paintComponent(g: Graphics) {
    if (!isOpaque()) {
      g.setColor(getBackground())
      g.fillRect(0, 0, getWidth(), getHeight())
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
