package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private const val POPUP_WIDTH = 240
private const val POPUP_HEIGHT = 120
private val GRIP_BACKGROUND = Color(0xE0_E0_E0)
private val BORDER_COLOR = Color(0x64_64_64)

fun createUI(): Component {
  val allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  val fontNames = allFonts.map { it.fontName }.toTypedArray()
  val fontList = JList(fontNames)
  fontList.selectionMode = ListSelectionModel.SINGLE_SELECTION

  val popupMenu = JPopupMenu()
  popupMenu.setBorder(BorderFactory.createEmptyBorder())
  popupMenu.setPopupSize(POPUP_WIDTH, POPUP_HEIGHT)

  val fontComboBox = createFontComboBox(fontNames, fontList, popupMenu)
  // Selecting an item in the list updates the combo box selection to match.
  fontList.addListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      fontComboBox.selectedIndex = fontList.selectedIndex
    }
  }
  // Double-clicking an item closes the popup;
  // the selection is already synchronized by the ListSelectionListener.
  fontList.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val isDoubleClick = e.clickCount >= 2
      if (isDoubleClick) {
        popupMenu.setVisible(false)
      }
    }
  })
  fontComboBox.addItemListener { e ->
    val idx = fontComboBox.getSelectedIndex()
    if (e.stateChange == ItemEvent.SELECTED && idx >= 0) {
      fontList.setSelectedIndex(idx)
      fontList.scrollRectToVisible(fontList.getCellBounds(idx, idx))
    }
  }

  val scrollPane = JScrollPane(fontList)
  scrollPane.setBorder(BorderFactory.createEmptyBorder())
  scrollPane.setViewportBorder(BorderFactory.createEmptyBorder())
  popupMenu.add(createResizablePopupContentPanel(scrollPane))

  return JPanel(FlowLayout(FlowLayout.LEADING)).also {
    it.add(fontComboBox)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFontComboBox(
  fontNames: Array<String>,
  fontList: JList<String>,
  popupMenu: JPopupMenu,
): JComboBox<String> {
  val fontComboBox = object : JComboBox<String>(fontNames) {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(listener)
      super.updateUI()
      listener = ComboBoxPopupMenuHandler(fontList, popupMenu)
      addPopupMenuListener(listener)
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = minOf(d.width, POPUP_WIDTH)
      return d
    }
  }
  fontComboBox.setMaximumRowCount(1)
  return fontComboBox
}

private fun createResizablePopupContentPanel(scrollPane: JScrollPane): JPanel {
  val resizeGripLabel = JLabel("", ResizeGripIcon(), SwingConstants.CENTER)
  val resizeHandler = PopupMenuResizeHandler()
  resizeGripLabel.addMouseListener(resizeHandler)
  resizeGripLabel.addMouseMotionListener(resizeHandler)
  resizeGripLabel.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR))
  resizeGripLabel.setOpaque(true)
  resizeGripLabel.setBackground(GRIP_BACKGROUND)
  resizeGripLabel.setFocusable(false)

  val contentPanel = JPanel(BorderLayout())
  contentPanel.add(scrollPane)
  contentPanel.add(resizeGripLabel, BorderLayout.SOUTH)
  contentPanel.add(Box.createHorizontalStrut(POPUP_WIDTH), BorderLayout.NORTH)
  contentPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR))
  return contentPanel
}

private class ComboBoxPopupMenuHandler(
  private val fontList: JList<String>,
  private val popupMenu: JPopupMenu,
) : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val src = e.getSource()
    if (src is JComboBox<*>) {
      fontList.setSelectedIndex(src.getSelectedIndex())
      EventQueue.invokeLater { popupMenu.show(src, 0, src.getHeight()) }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

// Resizes the enclosing JPopupMenu (and its underlying heavyweight/lightweight
// popup window) vertically while the grip label is dragged.
private class PopupMenuResizeHandler : MouseInputAdapter() {
  private val dragStartPoint = Point()
  private val dragStartSize = Dimension()

  override fun mousePressed(e: MouseEvent) {
    val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.component)
    if (popup != null) {
      dragStartSize.size = popup.size
      dragStartPoint.location = e.locationOnScreen
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.component)
    if (c is JPopupMenu) {
      val dy = e.locationOnScreen.y - dragStartPoint.y
      val minHeight = c.minimumSize.height
      val size =
        Dimension(dragStartSize.width, maxOf(minHeight, dragStartSize.height + dy))
      c.preferredSize = size
      val window = SwingUtilities.getWindowAncestor(c)
      if (window != null && window.type == Window.Type.POPUP) {
        // Popup$HeavyWeightWindow
        window.size = size
      } else {
        // Popup$LightWeightWindow
        c.pack()
      }
    }
  }
}

private class ResizeGripIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.GRAY
    // Center the row of dots in the icon
    val dotsWidth = (DOT_COUNT - 1) * DOT_GAP + DOT_SIZE
    val startX = (iconWidth - dotsWidth) / 2
    val startY = (iconHeight - DOT_SIZE) / 2
    for (i in 0..<DOT_COUNT) {
      g2.fillRect(startX + DOT_GAP * i, startY, DOT_SIZE, DOT_SIZE)
    }
    g2.dispose()
  }

  override fun getIconWidth() = ICON_WIDTH

  override fun getIconHeight() = ICON_HEIGHT

  companion object {
    private const val ICON_WIDTH = 32
    private const val ICON_HEIGHT = 5
    private const val DOT_COUNT = 4
    private const val DOT_GAP = 4
    private const val DOT_SIZE = 2
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
