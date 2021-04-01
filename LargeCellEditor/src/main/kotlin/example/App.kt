package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

fun makeUI() = JPanel(GridBagLayout()).also {
  val list = makeIconList()
  val model = makeIconTableModel(list)
  val table = IconTable(model, list)
  it.add(table, GridBagConstraints())
  it.background = Color.WHITE
  it.preferredSize = Dimension(320, 240)
}

private fun makeIconList() = DefaultListModel<IconItem>().also {
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

private fun makeIconTableModel(list: ListModel<IconItem>): TableModel {
  val data = arrayOf(
    arrayOf(list.getElementAt(0), list.getElementAt(1), list.getElementAt(2)),
    arrayOf(list.getElementAt(3), list.getElementAt(4), list.getElementAt(5)),
    arrayOf(list.getElementAt(6), list.getElementAt(7), list.getElementAt(8))
  )
  return object : DefaultTableModel(data, null) {
    override fun isCellEditable(row: Int, column: Int) = false

    override fun getColumnCount() = 3
  }
}

private data class IconItem(val str: String) {
  val large = ImageIcon(javaClass.getResource("$str-48.png"))
  val small = ImageIcon(javaClass.getResource("$str-24.png"))
}

private class IconTableCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    icon = (value as? IconItem)?.large
    horizontalAlignment = SwingConstants.CENTER
    return this
  }
}

private class IconTable(model: TableModel?, list: ListModel<IconItem>) : JTable(model) {
  private val editor = EditorFromList(list)
  private val glassPane = object : JComponent() {
    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }

    override fun paintComponent(g: Graphics) {
      g.color = Color(0x64_FF_FF_FF, true)
      g.fillRect(0, 0, width, height)
      val buffer = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val g2 = buffer.createGraphics()
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .15f)
      g2.paint = Color.BLACK
      val r = editor.bounds
      for (i in 0 until OFFSET) {
        g2.fillRoundRect(
          r.x - i,
          r.y + OFFSET,
          r.width + i + i,
          r.height - OFFSET + i,
          5,
          5
        )
      }
      g2.dispose()
      g.drawImage(buffer, 0, 0, this)
    }
  }

  init {
    setDefaultRenderer(Any::class.java, IconTableCellRenderer())
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    initCellSize(50)

    val ml1 = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        startEditing()
      }
    }
    addMouseListener(ml1)

    editor.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    val a1 = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        cancelEditing()
      }
    }
    editor.actionMap.put("cancel-editing", a1)

    val ml2 = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val p = e.point
        val item = editor.model.getElementAt(editor.locationToIndex(p))
        setValueAt(item, selectedRow, selectedColumn)
        cancelEditing()
      }
    }
    editor.addMouseListener(ml2)

    val ml = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        cancelEditing()
      }
    }
    glassPane.addMouseListener(ml)
    glassPane.focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
      public override fun accept(c: Component) = c == editor
    }
    glassPane.add(editor)
    glassPane.isVisible = false
  }

  fun initCellSize(size: Int) {
    setRowHeight(size)
    val tableHeader = getTableHeader()
    tableHeader.resizingAllowed = false
    tableHeader.reorderingAllowed = false
    val m = getColumnModel()
    for (i in 0 until m.columnCount) {
      val col = m.getColumn(i)
      col.minWidth = size
      col.maxWidth = size
    }
    border = BorderFactory.createLineBorder(Color.BLACK)
  }

  fun startEditing() {
    rootPane.glassPane = glassPane
    val d = editor.preferredSize
    editor.size = d
    val sr = selectedRow
    val sc = selectedColumn
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
    private const val OFFSET = 4
  }
}

private class EditorFromList(model: ListModel<IconItem>) : JList<IconItem>(model) {
  @Transient private var handler: RollOverListener? = null
  private var rollOverRowIndex = -1
  private val dim: Dimension

  init {
    val icon = model.getElementAt(0).small
    val iw = INS + icon.iconWidth
    val ih = INS + icon.iconHeight
    dim = Dimension(iw * 3 + INS, ih * 3 + INS)
    fixedCellWidth = iw
    fixedCellHeight = ih
  }

  override fun getPreferredSize() = dim

  override fun updateUI() {
    removeMouseMotionListener(handler)
    removeMouseListener(handler)
    super.updateUI()
    handler = RollOverListener()
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
          index == rollOverRowIndex -> it.background = selectionBackground
          isSelected -> it.background = selectedColor
          else -> it.background = background
        }
        it.icon = value.small
      }
    }
  }

  private inner class RollOverListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      rollOverRowIndex = -1
      e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = locationToIndex(e.point)
      if (row != rollOverRowIndex) {
        rollOverRowIndex = row
        e.component.repaint()
      }
    }
  }

  companion object {
    private const val INS = 2
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
