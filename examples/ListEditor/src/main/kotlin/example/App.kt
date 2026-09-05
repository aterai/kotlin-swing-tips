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
import javax.swing.text.JTextComponent

fun createUI(): Component {
  val list = EditableList(createSampleModel())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSampleModel(): ListModel<ListItem> {
  val model = DefaultListModel<ListItem>()
  model.addElement(ListItem("red", ColorIcon(Color.RED)))
  model.addElement(ListItem("green", ColorIcon(Color.GREEN)))
  model.addElement(ListItem("blue", ColorIcon(Color.BLUE)))
  model.addElement(ListItem("cyan", ColorIcon(Color.CYAN)))
  model.addElement(ListItem("darkGray", ColorIcon(Color.DARK_GRAY)))
  model.addElement(ListItem("gray", ColorIcon(Color.GRAY)))
  model.addElement(ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)))
  model.addElement(ListItem("magenta", ColorIcon(Color.MAGENTA)))
  model.addElement(ListItem("orange", ColorIcon(Color.ORANGE)))
  model.addElement(ListItem("pink", ColorIcon(Color.PINK)))
  model.addElement(ListItem("yellow", ColorIcon(Color.YELLOW)))
  model.addElement(ListItem("black", ColorIcon(Color.BLACK)))
  model.addElement(ListItem("white", ColorIcon(Color.WHITE)))
  return model
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val icon = JLabel(null, null, SwingConstants.CENTER)
  private val label = JLabel(" ", SwingConstants.CENTER)
  private val renderer = object : JPanel(BorderLayout()) {
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
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = getNoFocusBorder(focusBorder)

  init {
    renderer.border = noFocusBorder
    renderer.isOpaque = true
    label.verticalTextPosition = SwingConstants.BOTTOM
    label.horizontalTextPosition = SwingConstants.CENTER
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.isOpaque = false
    icon.isOpaque = false
    renderer.add(icon)
    renderer.add(label, BorderLayout.SOUTH)
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
    renderer.border = if (cellHasFocus) focusBorder else noFocusBorder
    if (isSelected) {
      label.foreground = list.selectionForeground
      renderer.background = SELECTED_COLOR
    } else {
      label.foreground = list.foreground
      renderer.background = list.background
    }
    return renderer
  }

  companion object {
    val SELECTED_COLOR = Color(0xAE_16_64_FF.toInt(), true)
  }
}

@Suppress("DataClassShouldBeImmutable")
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

private open class ClearSelectionListener : MouseAdapter() {
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
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing = StartEditingAction()
  private val renameTitle = RenameAction()
  private var handler: MouseAdapter? = null

  init {
    editor.setBorder(
      BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.BLACK),
        BorderFactory.createEmptyBorder(0, 2, 0, 0),
      ),
    )
    editor.setHorizontalAlignment(SwingConstants.CENTER)

    // editor.setLineWrap(true);
    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(enterKey, RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_EDITING)

    val am = editor.actionMap
    am.put(RENAME_TITLE, renameTitle)
    val cancelEditing: Action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent?) {
        glassPane.setVisible(false)
      }
    }
    am.put(CANCEL_EDITING, cancelEditing)

    getInputMap(WHEN_FOCUSED).put(enterKey, START_EDITING)
    actionMap.put(START_EDITING, startEditing)
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    setSelectionForeground(null)
    setSelectionBackground(null)
    setCellRenderer(null)
    super.updateUI()
    setLayoutOrientation(HORIZONTAL_WRAP)
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    setVisibleRowCount(0)
    setFixedCellWidth(64)
    setFixedCellHeight(64)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setCellRenderer(ListItemListCellRenderer())
    handler = object : ClearSelectionListener() {
      override fun mouseClicked(e: MouseEvent) {
        val rect = getEditorBounds(selectedIndex)
        val isDoubleClick = e.getClickCount() >= 2
        if (isDoubleClick && rect != null && rect.contains(e.getPoint())) {
          val c = e.component
          val ae = ActionEvent(c, ActionEvent.ACTION_PERFORMED, "")
          startEditing.actionPerformed(ae)
        }
      }
    }
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  val editorTextField: JTextComponent
    get() = editor

  // Bounds of a single-line editor aligned with the title label of the cell
  private fun getEditorBounds(index: Int): Rectangle? {
    val rect = getCellBounds(index, index)
    if (rect != null) {
      val rowHeight = editor.getFontMetrics(editor.getFont()).height
      val h = rowHeight + editor.getInsets().top + editor.getInsets().bottom
      rect.y += rect.height - h - 1
      rect.height = h
    }
    return rect
  }

  private inner class StartEditingAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val idx = selectedIndex
      val rect = getEditorBounds(idx) ?: return
      rootPane.setGlassPane(glassPane)
      editor.text = model.getElementAt(idx)?.title
      val p = SwingUtilities.convertPoint(
        this@EditableList,
        rect.location,
        glassPane,
      )
      rect.location = p
      editor.bounds = rect
      editor.selectAll()
      glassPane.add(editor)
      glassPane.setVisible(true)
      editor.requestFocusInWindow()
    }
  }

  private inner class RenameAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.getText().trim { it <= ' ' }
      val index = selectedIndex
      if (title.isNotEmpty() && index >= 0) {
        model.getElementAt(index)?.title = title
      }
      glassPane.setVisible(false)
    }
  }

  private inner class EditorGlassPane : JComponent() {
    init {
      isOpaque = false
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        override fun accept(c: Component) = c == editorTextField
      }
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (!editorTextField.bounds.contains(e.point)) {
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
    private const val RENAME_TITLE = "rename-title"
    private const val CANCEL_EDITING = "cancel-editing"
    private const val START_EDITING = "start-editing"
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
