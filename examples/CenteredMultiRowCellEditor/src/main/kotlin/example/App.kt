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
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.JTextComponent
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

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

private class EditableList(
  model: ListModel<ListItem>,
) : JList<ListItem>(model) {
  private var editingIndex = -1
  private var editorWidth = -1
  private var handler: MouseAdapter? = null

  // private val glassPane = EditorGlassPane() // LightWeightEditor
  private var window: Window? = null // HeavyWeightEditor
  private val editor = object : JTextPane() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = editorWidth
      return d
    }
  }
  private val startEditing = StartEditingAction()
  private val renameTitle = RenameAction()

  init {
    initEditor()
    initAction()
    EventQueue.invokeLater(this::initHandler)
  }

  fun initAction() {
    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(enterKey, RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), RENAME_TITLE)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_EDITING)
    val am = editor.actionMap
    am.put(RENAME_TITLE, renameTitle)
    val cancelEditing = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        window?.isVisible = false
        editingIndex = -1
      }
    }
    am.put(CANCEL_EDITING, cancelEditing)
    getInputMap(WHEN_FOCUSED).put(enterKey, START_EDITING)
    actionMap.put(START_EDITING, startEditing)
  }

  fun initEditor() {
    editor.border = BorderFactory.createLineBorder(Color.GRAY)
    editor.editorKit = WrapEditorKit()
    editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    editor.font = UIManager.getFont("TextField.font")
    val doc = editor.styledDocument
    val center = SimpleAttributeSet()
    StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER)
    doc.setParagraphAttributes(0, doc.length, center, false)
    editor.componentPopupMenu = TextComponentPopupMenu()
    editor.document.addDocumentListener(ResizeHandler())
  }

  fun initHandler() {
    val windowAncestor = SwingUtilities.getWindowAncestor(this)
    windowAncestor.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        resetEditor(editor)
      }
    })
    windowAncestor.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        resetEditor(editor)
      }

      override fun componentMoved(e: ComponentEvent) {
        resetEditor(editor)
      }
    })
    val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this)
    c?.addMouseWheelListener {
      if (window?.isVisible == true && editingIndex >= 0) {
        val id = ActionEvent.ACTION_PERFORMED
        renameTitle.actionPerformed(ActionEvent(editor, id, ""))
      }
    }
  }

  private fun resetEditor(editor: Component?) {
    val windowAncestor = SwingUtilities.getWindowAncestor(editor)
    windowAncestor?.dispose()
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    removeMouseWheelListener(handler)
    selectionForeground = null
    selectionBackground = null
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    visibleRowCount = 0
    fixedCellWidth = 72
    fixedCellHeight = 64
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    cellRenderer = ListItemListCellRenderer()
    handler = EditingHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  private fun getEditorHeight(): Int {
    val rowHeight = editor.getFontMetrics(editor.getFont()).height
    return rowHeight + editor.getInsets().top + editor.getInsets().bottom
  }

  private inner class StartEditingAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val idx = selectedIndex
      val rect = getCellBounds(idx, idx) ?: return
      editingIndex = idx
      editorWidth = rect.width
      val h = getEditorHeight()
      rect.y += rect.height - h - 1
      rect.height = h
      editor.text = model.getElementAt(idx).title
      editor.bounds = rect
      editor.selectAll()
      val p = Point(rect.location)
      val c = this@EditableList
      SwingUtilities.convertPointToScreen(p, c)
      val w = window ?: JWindow(SwingUtilities.getWindowAncestor(c)).also {
        it.focusableWindowState = true
        it.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
        it.add(editor)
      }
      w.location = p
      w.pack()
      w.isVisible = true
      window = w
      editor.requestFocusInWindow()
    }
  }

  private inner class RenameAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.getText().trim()
      val index = editingIndex
      editingIndex = -1
      window?.isVisible = false
      if (title.isNotEmpty() && index >= 0) {
        model.getElementAt(index)?.title = title
        selectedIndex = index
      }
    }
  }

  private inner class ResizeHandler : DocumentListener {
    private var prevHeight = -1

    private fun update() {
      EventQueue.invokeLater {
        val h = editor.preferredSize.height
        if (prevHeight != h) {
          prevHeight = h
          window?.pack()
          editor.requestFocusInWindow()
        }
      }
    }

    override fun insertUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { update() }
    }

    override fun removeUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { update() }
    }

    override fun changedUpdate(e: DocumentEvent) {
      EventQueue.invokeLater { update() }
    }
  }

  private inner class EditingHandler : MouseAdapter() {
    private var startOutside = false

    override fun mouseClicked(e: MouseEvent) {
      val idx = selectedIndex
      val rect = getCellBounds(idx, idx) ?: return
      val h = editor.preferredSize.height
      rect.y = rect.y + rect.height - h - 2 - 1
      rect.height = h
      val isDoubleClick = e.clickCount >= 2
      if (isDoubleClick && rect.contains(e.point)) {
        val c = e.component
        val id = ActionEvent.ACTION_PERFORMED
        startEditing.actionPerformed(ActionEvent(c, id, ""))
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val list = e.component as? JList<*> ?: return
      startOutside = !contains(list, e.point)
      if (window != null && window?.isVisible == true && editingIndex >= 0) {
        val id = ActionEvent.ACTION_PERFORMED
        renameTitle.actionPerformed(ActionEvent(editor, id, ""))
      } else if (startOutside) {
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

    private fun clearSelectionAndFocus(list: JList<*>) {
      list.clearSelection()
      list.selectionModel.anchorSelectionIndex = -1
      list.selectionModel.leadSelectionIndex = -1
    }

    private fun contains(
      list: JList<*>,
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

private class WrapEditorKit : StyledEditorKit() {
  override fun getViewFactory() = WrapColumnFactory()
}

private class WrapColumnFactory : ViewFactory {
  override fun create(element: Element) =
    when (element.name) {
      AbstractDocument.ContentElementName -> WrapLabelView(element)
      AbstractDocument.ParagraphElementName -> ParagraphView(element)
      AbstractDocument.SectionElementName -> BoxView(element, View.Y_AXIS)
      StyleConstants.ComponentElementName -> ComponentView(element)
      StyleConstants.IconElementName -> IconView(element)
      else -> LabelView(element)
    }
}

private class WrapLabelView(
  element: Element?,
) : LabelView(element) {
  override fun getMinimumSpan(axis: Int) = if (axis == X_AXIS) {
    0f
  } else {
    super.getMinimumSpan(axis)
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
