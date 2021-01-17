package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter

fun makeUI(): Component {
  val model = DefaultListModel<ListItem>().also {
    it.addElement(ListItem("red", ColorIcon(Color.RED)))
    it.addElement(ListItem("green", ColorIcon(Color.GREEN)))
    it.addElement(ListItem("blue", ColorIcon(Color.BLUE)))
    it.addElement(ListItem("cyan", ColorIcon(Color.CYAN)))
    it.addElement(ListItem("darkGray", ColorIcon(Color.DARK_GRAY)))
    it.addElement(ListItem("gray", ColorIcon(Color.GRAY)))
    it.addElement(ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)))
    it.addElement(ListItem("magenta", ColorIcon(Color.MAGENTA)))
    it.addElement(ListItem("orange", ColorIcon(Color.ORANGE)))
    it.addElement(ListItem("pink", ColorIcon(Color.PINK)))
    it.addElement(ListItem("yellow", ColorIcon(Color.YELLOW)))
    it.addElement(ListItem("black", ColorIcon(Color.BLACK)))
    it.addElement(ListItem("white", ColorIcon(Color.WHITE)))
  }
  val list = EditableList(model)
  return JPanel(BorderLayout()).also {
    it.add(list)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label = object : JLabel("", null, CENTER) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (SELECTED_COLOR == background) {
        val g2 = g.create() as Graphics2D
        g2.paint = SELECTED_COLOR
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
  }
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNoFocusBorder()

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

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
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

  fun getNoFocusBorder(): Border {
    val i = focusBorder.getBorderInsets(renderer)
    return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }

  companion object {
    val SELECTED_COLOR = Color(0xAE_16_64_FF.toInt(), true)
  }
}

private data class ListItem(val title: String, val icon: Icon)

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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

    private fun <E> contains(list: JList<E>, pt: Point): Boolean {
      for (i in 0 until list.model.size) {
        if (list.getCellBounds(i, i).contains(pt)) {
          return true
        }
      }
      return false
    }
  }
}

private class EditableList(model: DefaultListModel<ListItem>) : JList<ListItem>(model) {
  private var handler: MouseInputAdapter? = null
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      rootPane.glassPane = glassPane
      val idx = selectedIndex
      val item: ListItem? = selectedValue
      val rect = getCellBounds(idx, idx)
      val p = SwingUtilities.convertPoint(this@EditableList, rect.location, glassPane)
      rect.location = p
      val h = editor.preferredSize.height
      rect.y = rect.y + rect.height - h - 1
      rect.height = h
      rect.grow(-2, 0)
      editor.bounds = rect
      editor.text = item!!.title
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
      val m = getModel()
      val title = editor.text.trim()
      val index = selectedIndex
      val item = m.getElementAt(index)
      if (title.isNotEmpty() && item != null) {
        (m as? DefaultListModel<ListItem>)?.also {
          it.remove(index)
          it.add(index, ListItem(editor.text, item.icon))
          selectedIndex = index
        }
      }
      glassPane.isVisible = false
    }
  }

  init {
    editor.border = BorderFactory.createLineBorder(Color.BLACK, 1)
    editor.horizontalAlignment = SwingConstants.CENTER
    // editor.setOpaque(false)
    // editor.setLineWrap(true)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-title")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    val am = editor.actionMap
    am.put("rename-title", renameTitle)
    am.put("cancel-editing", cancelEditing)
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val idx = selectedIndex
        val rect = getCellBounds(idx, idx) ?: return
        val h = editor.preferredSize.height
        rect.y = rect.y + rect.height - h
        rect.height = h
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick && rect.contains(e.point)) {
          startEditing.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
        }
      }
    })
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start-editing")
    actionMap.put("start-editing", startEditing)
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
    fixedCellWidth = 56
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
            renameTitle.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
          }
        }
      })
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
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
