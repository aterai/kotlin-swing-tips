package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter

fun makeUI(): Component {
  val list = EditableList(makeModel())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ListModel<ListItem> {
  val m = DefaultListModel<ListItem>()
  m.addElement(ListItem("red", ColorIcon(Color.RED)))
  m.addElement(ListItem("green", ColorIcon(Color.GREEN)))
  m.addElement(ListItem("blue", ColorIcon(Color.BLUE)))
  m.addElement(ListItem("cyan", ColorIcon(Color.CYAN)))
  m.addElement(ListItem("darkGray", ColorIcon(Color.DARK_GRAY)))
  m.addElement(ListItem("gray", ColorIcon(Color.GRAY)))
  m.addElement(ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)))
  m.addElement(ListItem("magenta", ColorIcon(Color.MAGENTA)))
  m.addElement(ListItem("orange", ColorIcon(Color.ORANGE)))
  m.addElement(ListItem("pink", ColorIcon(Color.PINK)))
  m.addElement(ListItem("yellow", ColorIcon(Color.YELLOW)))
  m.addElement(ListItem("black", ColorIcon(Color.BLACK)))
  m.addElement(ListItem("white", ColorIcon(Color.WHITE)))
  return m
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label = object : JLabel("", null, CENTER) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (SELECTED_COLOR == background) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = SELECTED_COLOR
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
  }
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = getNoFocusBorder(focusBorder)

  init {
    label.verticalTextPosition = SwingConstants.BOTTOM
    label.horizontalTextPosition = SwingConstants.CENTER
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = noFocusBorder
    label.isOpaque = false
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.add(label)
    renderer.isOpaque = true
  }

  private fun getNoFocusBorder(focusBorder: Border): Border {
    val b = UIManager.getBorder("List.noFocusBorder")
    return b ?: focusBorder.getBorderInsets(renderer).let {
      BorderFactory.createEmptyBorder(it.top, it.left, it.bottom, it.right)
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    label.text = value.title
    label.icon = value.icon
    label.border = if (cellHasFocus) focusBorder else noFocusBorder
    if (isSelected) {
      label.foreground = list.selectionForeground
      renderer.background = SELECTED_COLOR
    } else {
      label.foreground = list.foreground
      renderer.background = list.background
    }
    return renderer
  }

  // fun getNoFocusBorder(): Border {
  //   val i = focusBorder.getBorderInsets(renderer)
  //   return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  // }

  companion object {
    val SELECTED_COLOR = Color(0xAE_16_64_FF.toInt(), true)
  }
}

private data class ListItem(
  var title: String,
  val icon: Icon,
)

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.paint = Color.BLACK
    g2.drawRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

private class ClearSelectionListener : MouseInputAdapter() {
  private var startOutside = false

  override fun mousePressed(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    startOutside = !contains(list, e.point)
    if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    startOutside = false
  }

  override fun mouseDragged(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    if (contains(list, e.point)) {
      startOutside = false
    } else if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  companion object {
    private fun <E> clearSelectionAndFocus(list: JList<E>) {
      list.clearSelection()
      list.selectionModel.anchorSelectionIndex = -1
      list.selectionModel.leadSelectionIndex = -1
    }

    private fun <E> contains(
      list: JList<E>,
      pt: Point,
    ): Boolean {
      for (i in 0..<list.model.size) {
        if (list.getCellBounds(i, i).contains(pt)) {
          return true
        }
      }
      return false
    }
  }
}

private class EditableList(
  model: ListModel<ListItem>,
) : JList<ListItem>(model) {
  private var handler: MouseInputAdapter? = null
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      rootPane.glassPane = glassPane
      val idx = selectedIndex
      val item = selectedValue ?: return
      val rect = getCellBounds(idx, idx)
      val p = SwingUtilities.convertPoint(this@EditableList, rect.location, glassPane)
      rect.location = p
      val h = editor.preferredSize.height
      rect.y += rect.height - h - 1
      rect.height = h
      rect.grow(-2, 0)
      editor.bounds = rect
      editor.text = item.title
      editor.selectAll()
      glassPane.add(editor)
      glassPane.isVisible = true
      editor.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.isVisible = false
    }
  }
  private val renameTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.text.trim()
      val index = selectedIndex
      if (title.isNotEmpty() && index >= 0) {
        val item = getModel().getElementAt(index)
        item.title = title
        selectedIndex = index
      }
      glassPane.isVisible = false
    }
  }

  init {
    editor.border = BorderFactory.createLineBorder(Color.BLACK)
    editor.horizontalAlignment = SwingConstants.CENTER
    // editor.setLineWrap(true)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), RENAME)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL)
    val am = editor.actionMap
    am.put(RENAME, renameTitle)
    am.put(CANCEL, cancelEditing)
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val idx = selectedIndex
        val rect = getCellBounds(idx, idx) ?: return
        val h = editor.preferredSize.height
        rect.y += rect.height - h
        rect.height = h
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick && rect.contains(e.point)) {
          startEditing.actionPerformed(
            ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""),
          )
        }
      }
    })
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), EDITING)
    actionMap.put(EDITING, startEditing)
  }

  override fun updateUI() {
    removeMouseListener(handler)
    selectionForeground = null
    selectionBackground = null
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    visibleRowCount = 0
    fixedCellWidth = 64
    fixedCellHeight = 56
    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    cellRenderer = ListItemListCellRenderer()
    handler = ClearSelectionListener()
    addMouseListener(handler)
  }

  private inner class EditorGlassPane : JComponent() {
    init {
      isOpaque = false
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        public override fun accept(c: Component) = c == editor
      }
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (!editor.bounds.contains(e.point)) {
            val c = e.component
            val id = ActionEvent.ACTION_PERFORMED
            renameTitle.actionPerformed(ActionEvent(c, id, ""))
          }
        }
      })
      addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          setVisible(false)
        }
      })
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }
  }

  companion object {
    const val RENAME = "rename-title"
    const val CANCEL = "cancel-editing"
    const val EDITING = "start-editing"
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
