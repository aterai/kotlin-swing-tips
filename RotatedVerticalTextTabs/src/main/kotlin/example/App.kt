package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*

private val p = JPanel()

private fun makeVerticalTabIcon(title: String?, icon: Icon, clockwise: Boolean): Icon? {
  val label = JLabel(title, icon, SwingConstants.LEADING)
  label.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
  val d = label.preferredSize
  val w = d.height
  val h = d.width
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.graphics as? Graphics2D ?: return null
  val at = if (clockwise) {
    AffineTransform.getTranslateInstance(w.toDouble(), 0.0)
  } else {
    AffineTransform.getTranslateInstance(0.0, h.toDouble())
  }
  at.quadrantRotate(if (clockwise) 1 else -1)
  g2.transform = at
  SwingUtilities.paintComponent(g2, label, p, 0, 0, d.width, d.height)
  g2.dispose()
  return ImageIcon(bi)
}

fun makeUI(): Component {
  val tabs1 = JTabbedPane(SwingConstants.LEFT)
  val tabs2 = JTabbedPane(SwingConstants.RIGHT)
  listOf("computer", "directory", "file").forEach {
    val icon = UIManager.getIcon("FileView.${it}Icon")
    val c1 = JLabel(it, icon, SwingConstants.LEADING)
    tabs1.addTab(null, makeVerticalTabIcon(it, icon, false), c1)
    val c2 = JLabel(it, icon, SwingConstants.CENTER)
    tabs2.addTab(null, makeVerticalTabIcon(it, icon, true), c2)
  }
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs1, tabs2)
  split.resizeWeight = .5
  split.preferredSize = Dimension(320, 240)
  return split
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
