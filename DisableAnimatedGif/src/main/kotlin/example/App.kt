package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/duke.running.gif"))
  val label1 = JLabel(icon)
  label1.isEnabled = false
  label1.border = BorderFactory.createTitledBorder("Default")

  val label2 = object : JLabel(icon) {
    override fun imageUpdate(img: Image, infoflags: Int, x: Int, y: Int, w: Int, h: Int): Boolean {
      var info = infoflags
      if (!isEnabled) {
        info = info and FRAMEBITS.inv()
      }
      return super.imageUpdate(img, info, x, y, w, h)
    }
  }
  label2.isEnabled = false
  label2.border = BorderFactory.createTitledBorder("Override imageUpdate(...)")

  val label3 = JLabel(icon)
  label3.isEnabled = false
  label3.border = BorderFactory.createTitledBorder("setDisabledIcon")
  val i = ImageIcon(cl.getResource("example/duke.running_frame_0001.gif"))
  label3.disabledIcon = makeDisabledIcon(i)

  val check = JCheckBox("setEnabled")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    label1.isEnabled = b
    label2.isEnabled = b
    label3.isEnabled = b
  }

  val p = JPanel(GridLayout(2, 2))
  p.add(label1)
  p.add(label2)
  p.add(label3)

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeDisabledIcon(icon: ImageIcon): Icon {
  val img = icon.image
  return ImageIcon(GrayFilter.createDisabledImage(img))
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
