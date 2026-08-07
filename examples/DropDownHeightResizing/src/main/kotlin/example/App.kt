package example

import java.awt.*
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
  val fontListModel = DefaultListModel<String>()
  allFonts.map { it.fontName }.forEach { fontListModel.addElement(it) }
  val fontList = JList(fontListModel)
  fontList.selectionMode = ListSelectionModel.SINGLE_SELECTION

  val popupMenu = JPopupMenu()
  popupMenu.setBorder(BorderFactory.createEmptyBorder())
  popupMenu.setPopupSize(POPUP_WIDTH, POPUP_HEIGHT)

  val fontComboBox = createFontComboBox(allFonts, fontList, popupMenu)
  fontList.addListSelectionListener {
    fontComboBox.selectedIndex = fontList.selectedIndex
  }
  fontList.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.getClickCount() - 1 > 0) {
        fontComboBox.selectedIndex = fontList.selectedIndex
        popupMenu.setVisible(false)
      }
    }
  })
  fontComboBox.addItemListener {
    val idx = fontComboBox.getSelectedIndex()
    fontList.setSelectedIndex(idx)
    fontList.scrollRectToVisible(fontList.getCellBounds(idx, idx))
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
  fonts: Array<Font>,
  fontList: JList<String>,
  popupMenu: JPopupMenu,
): JComboBox<String> {
  val fontComboBoxModel = DefaultComboBoxModel<String>()
  fonts.map { it.fontName }.forEach { fontComboBoxModel.addElement(it) }
  val fontComboBox = object : JComboBox<String>(fontComboBoxModel) {
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
    // rect.setSize(window.getSize())
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // rect.setSize(window.getSize())
  }
}

private class PopupMenuResizeHandler : MouseInputAdapter() {
  private val newSize = Rectangle()
  private val dragStartPoint = Point()
  private val dragStartSize = Dimension()

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, c)
    newSize.size = popup.size
    dragStartSize.size = popup.size
    dragStartPoint.location = c.locationOnScreen
  }

  override fun mouseDragged(e: MouseEvent) {
    newSize.height = dragStartSize.height + e.locationOnScreen.y - dragStartPoint.y
    val c = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.component)
    if (c is JPopupMenu) {
      c.preferredSize = newSize.size
      val window = SwingUtilities.getWindowAncestor(c)
      if (window != null && window.type == Window.Type.POPUP) {
        // Popup$HeavyWeightWindow
        window.setSize(newSize.width, newSize.height)
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
    val start = iconWidth / 2 - (DOT_COUNT - 1) * 2
    val centerY = iconHeight / 2
    for (i in 0..<DOT_COUNT) {
      g2.fillRect(start + DOT_GAP * i, centerY, DOT_SIZE, DOT_SIZE)
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
