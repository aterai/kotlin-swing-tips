package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  for (i in 0 until 100) {
    tabbedPane.addTab("title$i", JLabel("label$i"))
  }

  val check = JCheckBox("setSelectedIndex")
  check.horizontalAlignment = SwingConstants.RIGHT

  val slider = JSlider(0, tabbedPane.tabCount - 1, 50)
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.paintTicks = true
  slider.paintLabels = true
  slider.addChangeListener { e ->
    val s = e.source
    if (s is JSlider) {
      val i = s.value
      if (check.isSelected) {
        tabbedPane.selectedIndex = i
      }
      scrollTabAt(tabbedPane, i)
    }
  }

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder("Scroll Slider")
  p.add(check, BorderLayout.SOUTH)
  p.add(slider, BorderLayout.NORTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun scrollTabAt(tabbedPane: JTabbedPane, index: Int) {
  var cmp: Component? = null
  for (c in tabbedPane.components) {
    if ("TabbedPane.scrollableViewport" == c.name) {
      cmp = c
      break
    }
  }
  if (cmp is JViewport) {
    for (i in 0 until tabbedPane.tabCount) {
      tabbedPane.setForegroundAt(i, if (i == index) Color.RED else Color.BLACK)
    }
    val d = tabbedPane.size
    val r = tabbedPane.getBoundsAt(index)
    val gw = (d.width - r.width) / 2
    r.grow(gw, 0)
    cmp.scrollRectToVisible(r)
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
