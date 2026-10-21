package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*

// The divider location used to expand the collapsed tab component
private const val EXPANDED_SIZE = 120
private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
private val tabAreaSize = Dimension(40, 40)
private val renderer = JPanel()

fun createUI(): Component {
  val tabbedPane = JTabbedPane(SwingConstants.LEFT)
  tabbedPane.minimumSize = tabAreaSize
  tabbedPane.isFocusable = false
  tabbedPane.addChangeListener { updateDividerLocation(tabbedPane) }
  tabbedPane.addMouseListener(object : MouseAdapter() {
    private var prevIndex = -1

    override fun mouseClicked(e: MouseEvent) {
      val tabs = e.component as? JTabbedPane ?: return
      val selected = tabs.selectedIndex
      // Clicks on the tab component area are also delivered to the JTabbedPane,
      // so only toggle when the already selected tab itself is clicked
      val isSelectedTab = tabs.indexAtLocation(e.x, e.y) == selected
      if (isSelectedTab && prevIndex == selected && SwingUtilities.isLeftMouseButton(e)) {
        toggleCollapsed(tabs)
      }
      prevIndex = selected
    }
  })
  listOf("computer", "directory", "file").forEach {
    val icon = UIManager.getIcon("FileView.${it}Icon")
    val label = JLabel(it, icon, SwingConstants.CENTER)
    label.preferredSize = Dimension(100, 100)
    label.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        tabComponentResized(e, tabbedPane)
      }
    })
    val tabIcon = makeVerticalTabIcon(it, icon)
    tabbedPane.addTab(null, tabIcon, label, it)
  }
  splitPane.leftComponent = tabbedPane
  splitPane.rightComponent = JScrollPane(JTree())

  return JPanel(BorderLayout()).also {
    it.add(splitPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun tabComponentResized(
  e: ComponentEvent,
  tabs: JTabbedPane,
) {
  val c = e.component
  if (c == tabs.selectedComponent) {
    val d = c.preferredSize
    if (isTopBottomTabPlacement(tabs.tabPlacement)) {
      d.height = splitPane.dividerLocation - tabAreaSize.height
    } else {
      d.width = splitPane.dividerLocation - tabAreaSize.width
    }
    c.preferredSize = d
  }
}

private fun updateDividerLocation(tabs: JTabbedPane) {
  val c = tabs.selectedComponent
  if (isTopBottomTabPlacement(tabs.tabPlacement)) {
    splitPane.dividerLocation = c.preferredSize.height + tabAreaSize.height
  } else {
    splitPane.dividerLocation = c.preferredSize.width + tabAreaSize.width
  }
}

// Toggle the collapsed state of the selected tab component: collapse it to the tab area size,
// or expand it to the default size if it is already collapsed
private fun toggleCollapsed(tabs: JTabbedPane) {
  val isTopBottom = isTopBottomTabPlacement(tabs.tabPlacement)
  val d = tabs.selectedComponent.preferredSize
  val isCollapsed = (if (isTopBottom) d.height else d.width) == 0
  val tabAreaLength = if (isTopBottom) tabAreaSize.height else tabAreaSize.width
  splitPane.dividerLocation = if (isCollapsed) EXPANDED_SIZE else tabAreaLength
}

private fun makeVerticalTabIcon(
  title: String?,
  icon: Icon?,
): Icon {
  val label = JLabel(title, icon, SwingConstants.LEADING)
  label.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
  val d = label.preferredSize
  val w = d.height
  val h = d.width
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  val at = AffineTransform.getTranslateInstance(0.0, h.toDouble())
  at.quadrantRotate(-1)
  g2.transform = at
  SwingUtilities.paintComponent(g2, label, renderer, 0, 0, d.width, d.height)
  g2.dispose()
  return ImageIcon(bi)
}

private fun isTopBottomTabPlacement(tabPlacement: Int) =
  tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM

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
