package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  val m1 = DefaultListModel<String>()
  fonts.map { it.fontName }.forEach { m1.addElement(it) }
  val list = JList(m1)
  list.selectionMode = ListSelectionModel.SINGLE_SELECTION

  val popup = JPopupMenu()
  popup.border = BorderFactory.createEmptyBorder()
  popup.setPopupSize(240, 120)

  val m2 = DefaultComboBoxModel<String>()
  fonts.map { it.fontName }.forEach { m2.addElement(it) }
  val combo = object : JComboBox<String>(m2) {
    private var handler: PopupMenuListener? = null
    override fun updateUI() {
      removePopupMenuListener(handler)
      super.updateUI()
      handler = object : PopupMenuListener {
        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
          val c = e.source as? JComboBox<*> ?: return
          list.selectedIndex = c.selectedIndex
          EventQueue.invokeLater { popup.show(c, 0, c.height) }
        }

        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
          // not need
        }

        override fun popupMenuCanceled(e: PopupMenuEvent) {
          // not need
        }
      }
      addPopupMenuListener(handler)
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = minOf(d.width, 240)
      return d
    }
  }
  combo.maximumRowCount = 1

  list.addListSelectionListener { combo.selectedIndex = list.selectedIndex }
  list.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount > 1) {
        combo.selectedIndex = list.selectedIndex
        popup.isVisible = false
      }
    }
  })
  combo.addItemListener {
    val idx = combo.selectedIndex
    list.selectedIndex = idx
    list.scrollRectToVisible(list.getCellBounds(idx, idx))
  }

  val scroll = JScrollPane(list)
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()

  val bottom = JLabel("", DotIcon(), SwingConstants.CENTER)
  val rwl = ResizePopupMeneListener()
  bottom.addMouseListener(rwl)
  bottom.addMouseMotionListener(rwl)
  bottom.cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)
  bottom.isOpaque = true
  bottom.background = Color(0xE0_E0_E0)
  bottom.isFocusable = false

  val resizePanel = JPanel(BorderLayout())
  resizePanel.add(scroll)
  resizePanel.add(bottom, BorderLayout.SOUTH)
  resizePanel.add(Box.createHorizontalStrut(240), BorderLayout.NORTH)
  resizePanel.border = BorderFactory.createLineBorder(Color(0x64_64_64))
  popup.add(resizePanel)

  return JPanel(FlowLayout(FlowLayout.LEADING)).also {
    it.add(combo)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ResizePopupMeneListener : MouseInputAdapter() {
  private val rect = Rectangle()
  private val startPt = Point()
  private val startDim = Dimension()

  override fun mousePressed(e: MouseEvent) {
    val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.component)
    rect.size = popup.size
    startDim.size = popup.size
    startPt.location = e.component.locationOnScreen
  }

  override fun mouseDragged(e: MouseEvent) {
    rect.height = startDim.height + e.locationOnScreen.y - startPt.y
    val c = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.component)
    if (c is JPopupMenu) {
      val popup = c
      popup.preferredSize = rect.size
      val w = SwingUtilities.getWindowAncestor(popup)
      if (w != null && w.type == Window.Type.POPUP) {
        w.setSize(rect.width, rect.height)
      } else {
        popup.pack()
      }
    }
  }
}

private class DotIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.GRAY
    val dots = 4
    val gap = 4
    val start = iconWidth / 2 - (dots - 1) * 2
    val h = iconHeight / 2
    for (i in 0 until dots) {
      g2.fillRect(start + gap * i, h, 2, 2)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 5
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
