package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseWheelListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabbedPane = makeWheelTabbedPane()
  tabbedPane.addTab("JLabel1", JLabel("JLabel1"))
  tabbedPane.addTab("JLabel2", JLabel("JLabel2"))
  tabbedPane.addTab("JLabel(disabled)", JLabel("JLabel"))
  tabbedPane.setEnabledAt(2, false)
  tabbedPane.addTab("JSplitPane", JSplitPane())
  tabbedPane.addTab("JPanel", JLabel("JPanel"))
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JTextArea", JScrollPane(JTextArea("JTextArea")))
  val comboBox = JComboBox(TabPlacements.values())
  comboBox.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is TabPlacements) {
      tabbedPane.tabPlacement = item.tabPlacement
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
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

private fun makeWheelTabbedPane() = object : JTabbedPane(SwingConstants.TOP, SCROLL_TAB_LAYOUT) {
  @Transient private var handler: MouseWheelListener? = null
  override fun updateUI() {
    removeMouseWheelListener(handler)
    super.updateUI()
    handler = MouseWheelListener { e ->
      val source = e.component
      if (source is JTabbedPane && getTabAreaBounds(source).contains(e.point)) {
        val dir = (if (e.isControlDown) -1 else 1) * e.preciseWheelRotation
        val key = if (dir > 0) "navigateNext" else "navigatePrevious"
        val ae = ActionEvent(
          source,
          ActionEvent.ACTION_PERFORMED,
          "",
          e.getWhen(),
          e.modifiersEx
        )
        source.actionMap[key].actionPerformed(ae)
      }
    }
    addMouseWheelListener(handler)
  }
}

fun getTabAreaBounds(tabbedPane: JTabbedPane): Rectangle {
  val tabbedRect = tabbedPane.bounds
  val xx = tabbedRect.x
  val yy = tabbedRect.y
  val compRect = tabbedPane.selectedComponent?.bounds ?: Rectangle()
  val tabPlacement = tabbedPane.tabPlacement
  if (isTopBottomTabPlacement(tabPlacement)) {
    tabbedRect.height = tabbedRect.height - compRect.height
    if (tabPlacement == SwingConstants.BOTTOM) {
      tabbedRect.y += compRect.y + compRect.height
    }
  } else {
    tabbedRect.width = tabbedRect.width - compRect.width
    if (tabPlacement == SwingConstants.RIGHT) {
      tabbedRect.x += compRect.x + compRect.width
    }
  }
  tabbedRect.translate(-xx, -yy)
  return tabbedRect
}

private fun isTopBottomTabPlacement(tabPlacement: Int) =
  tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM

private enum class TabPlacements(val tabPlacement: Int) {
  TOP(SwingConstants.TOP), BOTTOM(SwingConstants.BOTTOM), LEFT(SwingConstants.LEFT), RIGHT(SwingConstants.RIGHT);
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
