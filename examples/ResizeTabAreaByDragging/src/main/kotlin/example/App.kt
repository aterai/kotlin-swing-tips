package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val tabbedPane = ClippedTitleTabbedPane(SwingConstants.LEFT).also {
    it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    it.addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
    it.addTab("2", ColorIcon(Color.GREEN), JScrollPane(JTable(5, 3)))
    it.addTab("33333333333333", ColorIcon(Color.BLUE), JLabel("666666666"))
    it.addTab("444444444444444444444444", ColorIcon(Color.ORANGE), JSplitPane())
  }
  return JPanel(BorderLayout()).also {
    it.add(JLayer(tabbedPane, TabAreaResizeLayer()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ClippedTitleTabbedPane(
  tabPlacement: Int,
) : JTabbedPane(tabPlacement) {
  var tabAreaWidth = 32
    set(newWidth) {
      val w = newWidth.coerceIn(MIN_WIDTH, width - MIN_WIDTH)
      if (field != w) {
        field = w
        revalidate()
      }
    }
  private val tabInsets = UIManager.getInsets("TabbedPane.tabInsets")
    ?: getSynthTabInsets()
  private val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets")
    ?: getSynthTabAreaInsets()

  private fun getSynthTabInsets(): Insets {
    val region = Region.TABBED_PANE_TAB
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  private fun getSynthTabAreaInsets(): Insets {
    val region = Region.TABBED_PANE_TAB_AREA
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabAreaIns = tabAreaInsets
    val ins = insets
    val areaWidth = width - tabAreaIns.left - tabAreaIns.right - ins.left - ins.right
    val isSide = tabPlacement == LEFT || tabPlacement == RIGHT
    var tabWidth = if (isSide) tabAreaWidth else areaWidth / tabCount
    val gap = if (isSide) 0 else areaWidth - tabWidth * tabCount

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)

    super.doLayout()
  }

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip ?: title, index)
    setTabComponentAt(index, JLabel(title, icon, SwingConstants.LEADING))
  }

  private fun updateAllTabWidth(
    tabWidth: Int,
    gap: Int,
  ) {
    val dim = Dimension()
    var rest = gap
    for (i in 0..<tabCount) {
      val tab = getTabComponentAt(i) as? JComponent ?: continue
      val a = if (i == tabCount - 1) rest else 1
      val w = if (rest > 0) tabWidth + a else tabWidth
      dim.setSize(w, tab.preferredSize.height)
      tab.preferredSize = dim
      rest -= a
    }
  }

  companion object {
    private const val MIN_WIDTH = 24
  }
}

private class TabAreaResizeLayer : LayerUI<ClippedTitleTabbedPane>() {
  private var offset = 0
  private var resizing = false

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out ClippedTitleTabbedPane>,
  ) {
    val tabbedPane = l.view
    if (e.id == MouseEvent.MOUSE_PRESSED) {
      val rect = getDividerBounds(tabbedPane)
      val pt = e.point
      SwingUtilities.convertPoint(e.component, pt, tabbedPane)
      if (rect.contains(pt)) {
        offset = pt.x - tabbedPane.tabAreaWidth
        tabbedPane.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
        resizing = true
        e.consume()
      }
    } else if (e.id == MouseEvent.MOUSE_RELEASED) {
      tabbedPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      resizing = false
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out ClippedTitleTabbedPane>,
  ) {
    val tabbedPane = l.view
    val pt = e.point
    SwingUtilities.convertPoint(e.component, pt, tabbedPane)
    if (e.id == MouseEvent.MOUSE_MOVED) {
      val r = getDividerBounds(tabbedPane)
      val c = if (r.contains(pt)) Cursor.W_RESIZE_CURSOR else Cursor.DEFAULT_CURSOR
      tabbedPane.cursor = Cursor.getPredefinedCursor(c)
    } else if (e.id == MouseEvent.MOUSE_DRAGGED && resizing) {
      tabbedPane.tabAreaWidth = pt.x - offset
      e.consume()
    }
  }

  companion object {
    private fun getDividerBounds(tabbedPane: ClippedTitleTabbedPane): Rectangle {
      val dividerSize = Dimension(4, 4)
      val bounds = tabbedPane.bounds
      val compRect = tabbedPane.selectedComponent?.bounds ?: Rectangle()
      when (tabbedPane.tabPlacement) {
        SwingConstants.LEFT -> {
          bounds.x = compRect.x - dividerSize.width
          bounds.width = dividerSize.width * 2
        }

        SwingConstants.RIGHT -> {
          bounds.x += compRect.x + compRect.width - dividerSize.width
          bounds.width = dividerSize.width * 2
        }

        SwingConstants.BOTTOM -> {
          bounds.y += compRect.y + compRect.height - dividerSize.height
          bounds.height = dividerSize.height * 2
        }

        else -> {
          bounds.y = compRect.y - dividerSize.height
          bounds.height = dividerSize.height * 2
        }
      }
      return bounds
    }
  }
}

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
    g2.fillRect(1, 1, iconWidth - 3, iconHeight - 3)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
