package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

fun createUI() = JPanel(GridBagLayout()).also {
  val list = createIconList()
  val gap = 2
  val icon = list.getElementAt(0).small
  val iw = gap + icon.iconWidth
  val ih = gap + icon.iconHeight
  val d = Dimension(iw * 3 + gap, ih * 3 + gap)
  val editor = EditorFromList(list, d)
  editor.setFixedCellWidth(iw)
  editor.setFixedCellHeight(ih)
  val model = createIconTableModel(list)
  val table = IconTable(model, editor)
  it.add(table, GridBagConstraints())
  it.background = Color.WHITE
  it.preferredSize = Dimension(320, 240)
}

private fun createIconList() = DefaultListModel<IconItem>().also {
  it.addElement(IconItem("wi0009"))
  it.addElement(IconItem("wi0054"))
  it.addElement(IconItem("wi0062"))
  it.addElement(IconItem("wi0063"))
  it.addElement(IconItem("wi0064"))
  it.addElement(IconItem("wi0096"))
  it.addElement(IconItem("wi0111"))
  it.addElement(IconItem("wi0122"))
  it.addElement(IconItem("wi0124"))
}

private fun createIconTableModel(list: ListModel<IconItem>): TableModel {
  val data = arrayOf(
    arrayOf(list.getElementAt(0), list.getElementAt(1), list.getElementAt(2)),
    arrayOf(list.getElementAt(3), list.getElementAt(4), list.getElementAt(5)),
    arrayOf(list.getElementAt(6), list.getElementAt(7), list.getElementAt(8)),
  )
  val columnNames = arrayOf("A", "B", "C")
  return object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
}

private data class IconItem(
  val name: String,
) {
  val large = createIcon("example/$name-48.png")
  val small = createIcon("example/$name-24.png")
}

private fun createIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon("html.missingImage")
}

private class IconTableCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel && value is IconItem) {
      c.icon = value.large
      c.horizontalAlignment = CENTER
    }
    return c
  }
}

private class IconTable(
  model: TableModel?,
  private val editor: JList<IconItem>,
) : JTable(model) {
  private val glassPane = object : JComponent() {
    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color(0x64_FF_FF_FF, true)
      g2.fillRect(0, 0, width, height)
      // Draw the translucent drop shadow directly instead of allocating
      // a full-size off-screen BufferedImage on every repaint.
      // SRC_OVER compositing gives the same result as the intermediate buffer.
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .15f)
      g2.paint = Color.BLACK
      val r = editor.bounds
      for (i in 0..<OFFSET) {
        g2.fillRoundRect(
          r.x - i,
          r.y + OFFSET,
          r.width + i + i,
          r.height - OFFSET + i,
          5,
          5,
        )
      }
      g2.dispose()
    }
  }
  private var handler: MouseListener? = null

  init {
    val key = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    editor.getInputMap(WHEN_FOCUSED).put(key, "cancel-editing")
    val a = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        cancelEditing()
      }
    }
    editor.actionMap.put("cancel-editing", a)
    editor.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val idx = editor.locationToIndex(e.point)
        val row = selectedRow
        val col = selectedColumn
        if (idx >= 0 && row >= 0 && col >= 0) {
          setValueAt(editor.model.getElementAt(idx), row, col)
        }
        cancelEditing()
      }
    })

    glassPane.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        // Clicks on the editor JList are consumed by the JList itself,
        // so any click reaching the glass pane is outside the editor.
        cancelEditing()
      }
    })
    glassPane.focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
      override fun accept(c: Component) = c == editor
    }
    glassPane.add(editor)
    glassPane.isVisible = false
  }

  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    setRowHeight(CELL_SIZE)
    val tableHeader = getTableHeader()
    tableHeader.resizingAllowed = false
    tableHeader.reorderingAllowed = false
    val m = getColumnModel()
    for (i in 0..<m.columnCount) {
      val col = m.getColumn(i)
      col.minWidth = CELL_SIZE
      col.maxWidth = CELL_SIZE
    }
    border = BorderFactory.createLineBorder(Color.BLACK)
    setDefaultRenderer(Any::class.java, IconTableCellRenderer())
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    handler = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          startEditing()
        }
      }
    }
    addMouseListener(handler)
  }

  fun startEditing() {
    val sr = selectedRow
    val sc = selectedColumn
    if (sr < 0 || sc < 0) {
      return
    }
    rootPane.glassPane = glassPane
    val d = editor.preferredSize
    editor.size = d
    val r = getCellRect(sr, sc, true)
    val p = SwingUtilities.convertPoint(this, r.location, glassPane)
    p.translate((r.width - d.width) / 2, (r.height - d.height) / 2)
    editor.location = p
    glassPane.isVisible = true
    editor.setSelectedValue(getValueAt(sr, sc), true)
    editor.requestFocusInWindow()
  }

  fun cancelEditing() {
    glassPane.isVisible = false
  }

  companion object {
    const val CELL_SIZE = 50
    const val OFFSET = 4
  }
}

private class EditorFromList(
  model: ListModel<IconItem>,
  private val fixedSize: Dimension,
) : JList<IconItem>(model) {
  private var handler: RolloverListener? = null
  private var rolloverIndex = -1

  override fun getPreferredSize() = fixedSize

  override fun updateUI() {
    removeMouseMotionListener(handler)
    removeMouseListener(handler)
    super.updateUI()
    handler = RolloverListener()
    addMouseMotionListener(handler)
    addMouseListener(handler)
    border = BorderFactory.createLineBorder(Color.BLACK)
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = 0
    val selectedColor = Color(0xC8_C8_FF)
    setCellRenderer { _, value, index, isSelected, _ ->
      JLabel().also {
        it.isOpaque = true
        it.horizontalAlignment = SwingConstants.CENTER
        when {
          index == rolloverIndex -> it.background = selectionBackground
          isSelected -> it.background = selectedColor
          else -> it.background = background
        }
        it.icon = value.small
      }
    }
  }

  private inner class RolloverListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      rolloverIndex = -1
      e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val idx = locationToIndex(e.point)
      if (idx != rolloverIndex) {
        rolloverIndex = idx
        e.component.repaint()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
