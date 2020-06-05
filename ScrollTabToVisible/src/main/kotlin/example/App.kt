package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ChangeEvent

private val check = JCheckBox("setSelectedIndex").also {
  it.horizontalAlignment = SwingConstants.RIGHT
}

fun makeUI(): Component {
  val jtp = JTabbedPane()
  jtp.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  for (i in 0 until 100) {
    jtp.addTab("title$i", JLabel("label$i"))
  }

  val slider = JSlider(0, jtp.tabCount - 1, 50)
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.paintTicks = true
  slider.paintLabels = true
  slider.addChangeListener { e: ChangeEvent ->
    val i = (e.source as JSlider).value
    if (check.isSelected) {
      jtp.selectedIndex = i
    }
    scrollTabAt(jtp, i)
  }

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder("Scroll Slider")
  p.add(check, BorderLayout.SOUTH)
  p.add(slider, BorderLayout.NORTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(jtp)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun scrollTabAt(tp: JTabbedPane, index: Int) {
  var cmp: Component? = null
  for (c in tp.components) {
    if ("TabbedPane.scrollableViewport" == c.name) {
      cmp = c
      break
    }
  }
  if (cmp is JViewport) {
    for (i in 0 until tp.tabCount) {
      tp.setForegroundAt(i, if (i == index) Color.RED else Color.BLACK)
    }
    val d = tp.size
    val r = tp.getBoundsAt(index)
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
