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
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
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

private class EditableList(model: DefaultListModel<ListItem>) : JList<ListItem>(model) {
  private var editingIndex = -1
  private val window = JFrame()
  private val editor = JTextArea()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val idx = selectedIndex
      editingIndex = idx
      val rect = getCellBounds(idx, idx)
      editor.text = selectedValue.title
      val rowHeight = editor.getFontMetrics(editor.font).height
      rect.y += rect.height - rowHeight - 2 - 2 - 1
      editor.bounds = rect
      editor.selectAll()
      window.pack()
      val p = Point(rect.location)
      SwingUtilities.convertPointToScreen(p, this@EditableList)
      window.location = p
      window.isVisible = true
      editor.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      window.isVisible = false
      editingIndex = -1
    }
  }
  private val renameTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val m = getModel()
      val title = editor.text.trim()
      val index = editingIndex // selectedIndex
      val item = m.getElementAt(index)
      if (title.isNotEmpty() && index >= 0 && m is DefaultListModel<ListItem>) {
        m.remove(index)
        m.add(index, ListItem(editor.text, item.icon))
        EventQueue.invokeLater { selectedIndex = index }
      }
      window.isVisible = false
      editingIndex = -1
    }
  }

  init {
    window.isUndecorated = true
    window.isAlwaysOnTop = true
    val wl = object : WindowAdapter() {
      override fun windowDeactivated(e: WindowEvent) {
        if (editingIndex >= 0) {
          renameTitle.actionPerformed(ActionEvent(editor, ActionEvent.ACTION_PERFORMED, ""))
        }
        editingIndex = -1
      }
    }
    window.addWindowListener(wl)
    window.add(editor)
    editor.border = BorderFactory.createLineBorder(Color.GRAY)
    editor.lineWrap = true
    editor.font = UIManager.getFont("TextField.font")
    editor.componentPopupMenu = TextComponentPopupMenu()
    val dl = object : DocumentListener {
      private var prev = -1

      private fun update() {
        EventQueue.invokeLater {
          val h = editor.preferredSize.height
          if (prev != h) {
            window.pack()
            editor.requestFocusInWindow()
          }
          prev = h
        }
      }

      override fun insertUpdate(e: DocumentEvent) {
        update()
      }

      override fun removeUpdate(e: DocumentEvent) {
        update()
      }

      override fun changedUpdate(e: DocumentEvent) {
        update()
      }
    }
    editor.document.addDocumentListener(dl)

    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), RENAME)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), RENAME)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL)
    val am = editor.actionMap
    am.put(RENAME, renameTitle)
    am.put(CANCEL, cancelEditing)
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val idx = selectedIndex
        val rect = getCellBounds(idx, idx) ?: return
        val rowHeight = editor.getFontMetrics(editor.font).height
        rect.y += rect.height - rowHeight - 2 - 2 - 1
        rect.height = rowHeight
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick && rect.contains(e.point)) {
          startEditing.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
        }
      }
    })
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), EDITING)
    actionMap.put(EDITING, startEditing)
  }

  override fun updateUI() {
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
  }

  companion object {
    const val RENAME = "rename-title"
    const val CANCEL = "cancel-editing"
    const val EDITING = "start-editing"
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
