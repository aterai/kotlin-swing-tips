package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.ImageObserver
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url1 = cl.getResource("example/favicon.png")
  val url2 = cl.getResource("example/animated.gif")

  val combo = JComboBox<Icon>()
  combo.model = DefaultComboBoxModel(
    arrayOf(ImageIcon(url1), makeImageIcon(url2, combo, 1))
  )

  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.add(JLabel("Default ImageIcon"))
  p.add(JComboBox(arrayOf(ImageIcon(url1), ImageIcon(url2))))
  p.add(JLabel("ImageIcon#setImageObserver(ImageObserver)"))
  p.add(combo)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeImageIcon(url: URL?, combo: JComboBox<*>, row: Int): Icon {
  val icon = ImageIcon(url)
  icon.imageObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
    // @see http://www2.gol.com/users/tame/swing/examples/SwingExamples.html
    if (combo.isShowing && infoFlags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS) != 0) {
      repaintComboBox(combo, row)
    }
    infoFlags and (ImageObserver.ALLBITS or ImageObserver.ABORT) == 0
  }
  return icon
}

private fun repaintComboBox(combo: JComboBox<*>, row: Int) {
  if (combo.selectedIndex == row) {
    combo.repaint()
  }
  (combo.accessibleContext.getAccessibleChild(0) as? ComboPopup)?.list
    ?.takeIf { it.isShowing }
    ?.also {
      it.repaint(it.getCellBounds(row, row))
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
