package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Hashtable
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val list = listOf(
    "wi0009-16.png", "wi0054-16.png", "wi0062-16.png",
    "wi0063-16.png", "wi0064-16.png", "wi0096-16.png",
    "wi0111-16.png", "wi0122-16.png", "wi0124-16.png",
    "wi0126-16.png"
  )
  val labelTable = Hashtable<Int, Component>(list.size)
  list.forEachIndexed { i, s -> labelTable[i] = makeLabel(cl, s) }
  labelTable[list.size] = JButton("aaa")
  val slider1 = JSlider(SwingConstants.VERTICAL, 0, 10, 0)
  slider1.snapToTicks = true
  slider1.paintTicks = true
  slider1.labelTable = labelTable
  slider1.paintLabels = true

  val labelTable2 = Hashtable<Int, Component>(11)
  listOf("—ë", "ˆë", "“ó", "ŽQ", "ãæ", "ŒÞ", "—¤", "Ž½", "ŽJ", "‹è", "E")
    .map { JLabel(it) }
    .forEachIndexed { i, label ->
      label.foreground = Color(250, 100 - i * 10, 10)
      labelTable2[i] = label
    }

  val slider2 = JSlider(0, 10, 0)
  slider2.snapToTicks = true
  slider2.labelTable = labelTable2
  slider2.paintTicks = true
  slider2.paintLabels = true

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(20, 20, 20, 0)
  box.add(JSlider(0, 100, 100))
  box.add(Box.createVerticalStrut(20))
  box.add(JSlider())
  box.add(Box.createVerticalStrut(20))
  box.add(slider2)
  box.add(Box.createHorizontalGlue())

  return JPanel(BorderLayout()).also {
    it.add(slider1, BorderLayout.WEST)
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabel(cl: ClassLoader, path: String) =
  JLabel(path, ImageIcon(cl.getResource("example/$path")), SwingConstants.RIGHT)

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
