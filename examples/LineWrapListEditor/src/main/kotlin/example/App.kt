package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultEditorKit
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
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.verticalTextPosition = SwingConstants.BOTTOM
    label.horizontalTextPosition = SwingConstants.CENTER
    label.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
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
    icon.icon = value.icon
    label.text = value.title
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
    private val SELECTED_COLOR = Color(0xAE_16_64_FF.toInt(), true)
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
  private var handler: MouseAdapter? = null
  private var editingIndex = -1
  private val window = JFrame()
  private val editor = JTextArea()
  private val startEditing = StartEditingAction()
  private val renameTitle = RenameAction()

  init {
    window.isUndecorated = true
    window.setAlwaysOnTop(true)
    window.addWindowListener(object : WindowAdapter() {
      override fun windowDeactivated(e: WindowEvent?) {
        if (editingIndex >= 0) {
          val ae = ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "")
          renameTitle.actionPerformed(ae)
        }
        editingIndex = -1
      }
    })
    window.add(editor)
    editor.setBorder(BorderFactory.createLineBorder(Color.BLACK))
    // editor.setHorizontalAlignment(SwingConstants.CENTER);
    editor.setLineWrap(true)
    editor.setFont(UIManager.getFont("TextField.font"))
    editor.setComponentPopupMenu(TextComponentPopupMenu())
    editor.document.addDocumentListener(ResizeHandler())

    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(enterKey, RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_EDITING)

    val am = editor.actionMap
    am.put(RENAME_TITLE, renameTitle)
    val cancelEditing: Action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent?) {
        window.isVisible = false
        editingIndex = -1
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
        if (isDoubleClick && rect.contains(e.getPoint())) {
          val c = e.component
          val ae = ActionEvent(c, ActionEvent.ACTION_PERFORMED, "")
          startEditing.actionPerformed(ae)
        }
      }
    }
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  // Bounds of a single-line editor aligned with the title label of the cell
  private fun getEditorBounds(index: Int): Rectangle {
    val rect = getCellBounds(index, index)
    if (rect != null) {
      val i = editor.getInsets()
      val h = editor.getFontMetrics(editor.getFont()).height + i.top + i.bottom
      rect.y += rect.height - h - 1
      rect.height = h
    }
    return rect
  }

  private inner class StartEditingAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent?) {
      val idx = selectedIndex
      val rect = getEditorBounds(idx)
      editingIndex = idx
      editor.text = model.getElementAt(idx)?.title
      editor.bounds = rect
      editor.selectAll()
      val p = Point(rect.location)
      SwingUtilities.convertPointToScreen(p, this@EditableList)
      window.location = p
      window.pack()
      window.isVisible = true
      editor.requestFocusInWindow()
    }
  }

  private inner class RenameAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.text.trim()
      val index = editingIndex
      editingIndex = -1
      window.isVisible = false
      if (title.isNotEmpty() && index >= 0) {
        model.getElementAt(index)?.title = title
        setSelectedIndex(index)
      }
    }
  }

  private inner class ResizeHandler : DocumentListener {
    private var prevHeight = -1

    fun update() {
      val h = editor.getPreferredSize().height
      if (prevHeight != h) {
        prevHeight = h
        window.pack()
        editor.requestFocusInWindow()
      }
    }

    override fun insertUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { this.update() }
    }

    override fun removeUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { this.update() }
    }

    override fun changedUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { this.update() }
    }
  }

  companion object {
    private const val RENAME_TITLE = "rename-title"
    private const val CANCEL_EDITING = "cancel-editing"
    private const val START_EDITING = "start-editing"
  }
}

private class TextComponentPopupMenu : JPopupMenu() {
  init {
    add(DefaultEditorKit.CutAction())
    add(DefaultEditorKit.CopyAction())
    add(DefaultEditorKit.PasteAction())
    add("delete").addActionListener {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText != null
      for (menuElement in subElements) {
        val m = menuElement.component
        if (m is JMenuItem && m.action is DefaultEditorKit.PasteAction) {
          continue
        }
        m.isEnabled = hasSelectedText
      }
      super.show(c, x, y)
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
