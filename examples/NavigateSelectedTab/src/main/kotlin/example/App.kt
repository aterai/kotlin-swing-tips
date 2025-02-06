package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun makeUI(): Component {
  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  for (i in 0..<20) {
    val title = "title$i"
    tabs.addTab(title, JScrollPane(JTextArea(title)))
  }

  val layout = JCheckBox("SCROLL_TAB_LAYOUT", true)
  layout.isFocusable = false
  layout.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    tabs.tabLayoutPolicy = if (b) {
      JTabbedPane.SCROLL_TAB_LAYOUT
    } else {
      JTabbedPane.WRAP_TAB_LAYOUT
    }
  }

  val placement = JCheckBox("TOP", true)
  placement.isFocusable = false
  placement.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    tabs.tabPlacement = if (b) SwingConstants.TOP else SwingConstants.LEFT
  }

  val im0 = tabs.getInputMap(JComponent.WHEN_FOCUSED)
  val im1 = tabs.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  val am = tabs.actionMap

  val prev = "navigatePrevious"
  am.put(prev, TabNavigateAction(tabs, am[prev]))
  im0.put(KeyStroke.getKeyStroke("LEFT"), prev)
  im1.put(KeyStroke.getKeyStroke("alt LEFT"), prev)
  im0.put(KeyStroke.getKeyStroke("UP"), prev)
  im1.put(KeyStroke.getKeyStroke("alt UP"), prev)

  val next = "navigateNext"
  am.put(next, TabNavigateAction(tabs, am[next]))
  im0.put(KeyStroke.getKeyStroke("RIGHT"), next)
  im1.put(KeyStroke.getKeyStroke("alt RIGHT"), next)
  im0.put(KeyStroke.getKeyStroke("DOWN,"), next)
  im1.put(KeyStroke.getKeyStroke("alt DOWN,"), next)

  val box = Box.createHorizontalBox()
  box.add(layout)
  box.add(placement)

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TabNavigateAction(
  val tabs: JTabbedPane,
  val action: Action?,
) : AbstractAction() {
  override fun actionPerformed(e: ActionEvent) {
    if (action != null && action.isEnabled) {
      val isWrap = tabs.tabLayoutPolicy == JTabbedPane.WRAP_TAB_LAYOUT
      val isAltDown = e.modifiers and ActionEvent.ALT_MASK != 0
      val name = action.getValue(NAME)
      val base = tabs.selectedIndex
      val prev = name == "navigatePrevious" && base != 0
      val next = name == "navigateNext" && base != tabs.tabCount - 1
      val skip = prev || next
      if (isWrap || isAltDown || skip) {
        action.actionPerformed(
          ActionEvent(
            tabs,
            ActionEvent.ACTION_PERFORMED,
            null,
            e.getWhen(),
            e.modifiers,
          ),
        )
      }
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
