package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.*

private val check = JCheckBox("scroll tabs")

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane(TOP, SCROLL_TAB_LAYOUT) {
    private var handler: MouseWheelListener? = null

    override fun updateUI() {
      removeMouseWheelListener(handler)
      super.updateUI()
      handler = TabWheelHandler()
      addMouseWheelListener(handler)
    }
  }
  tabbedPane.addTab("JLabel1", JLabel("JLabel1"))
  tabbedPane.addTab("JLabel2", JLabel("JLabel2"))
  tabbedPane.addTab("JLabel(disabled)", JLabel("JLabel"))
  tabbedPane.setEnabledAt(2, false)
  tabbedPane.addTab("JSplitPane", JSplitPane())
  tabbedPane.addTab("JPanel", JLabel("JPanel"))
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JTextArea", JScrollPane(JTextArea("JTextArea")))
  for (i in 0..<20) {
    val title = "title$i"
    tabbedPane.addTab(title, JScrollPane(JLabel(title)))
  }

  val comboBox = JComboBox(TabPlacements.entries.toTypedArray())
  comboBox.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is TabPlacements) {
      tabbedPane.tabPlacement = item.tabPlacement
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(JLabel("TabPlacement: "))
  box.add(Box.createHorizontalStrut(2))
  box.add(comboBox)
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TabWheelHandler : MouseWheelListener {
  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val src = e.component as? JTabbedPane ?: return
    val policy = src.tabLayoutPolicy == JTabbedPane.SCROLL_TAB_LAYOUT
    if (policy && getTabAreaBounds(src).contains(e.point)) {
      val dir = (if (e.isControlDown) -1 else 1) * e.preciseWheelRotation > 0
      val id = ActionEvent.ACTION_PERFORMED
      val cmd = getCommand(dir)
      val event = ActionEvent(src, id, cmd, e.getWhen(), e.modifiersEx)
      src.actionMap[cmd].actionPerformed(event)
    }
  }

  private fun getCommand(dir: Boolean) = if (check.isSelected) {
    if (dir) "scrollTabsForwardAction" else "scrollTabsBackwardAction"
  } else {
    if (dir) "navigateNext" else "navigatePrevious"
  }
}

fun descendants(parent: Container): List<Component> =
  parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }

fun getTabAreaBounds(tabbedPane: JTabbedPane) =
  descendants(tabbedPane)
    .filterIsInstance<JViewport>()
    .first { "TabbedPane.scrollableViewport" == it.name }
    .let {
      val r = SwingUtilities.calculateInnerArea(it, null)
      SwingUtilities.convertRectangle(it, r, tabbedPane)
    }
    ?: Rectangle()

private enum class TabPlacements(val tabPlacement: Int) {
  TOP(SwingConstants.TOP),
  BOTTOM(SwingConstants.BOTTOM),
  LEFT(SwingConstants.LEFT),
  RIGHT(SwingConstants.RIGHT),
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
