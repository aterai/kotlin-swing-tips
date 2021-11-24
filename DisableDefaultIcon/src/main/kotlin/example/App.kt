package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/16x16transparent.png")

  val r1 = JRadioButton("img=null")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      (r1.topLevelAncestor as? Frame)?.iconImage = null
    }
  }

  val r2 = JRadioButton("img=new ImageIcon(\"\").getImage()")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      (r2.topLevelAncestor as? Frame)?.iconImage = ImageIcon("").image // JDK 1.5
    }
  }

  val r3 = JRadioButton("img=new BufferedImage(1, 1, TYPE_INT_ARGB)")
  r3.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      (r3.topLevelAncestor as? Frame)?.iconImage =
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB) // size=(1x1)
    }
  }

  val r4 = JRadioButton("img=toolkit.createImage(url_16x16transparent)", true)
  r4.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      (r4.topLevelAncestor as? Frame)?.iconImage =
        Toolkit.getDefaultToolkit().createImage(url) // 16x16transparent.png
    }
  }
  EventQueue.invokeLater {
    (r4.topLevelAncestor as? Frame)?.iconImage = Toolkit.getDefaultToolkit().createImage(url)
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("frame.setIconImage(img)")
  val bg = ButtonGroup()

  listOf(r1, r2, r3, r4).forEach {
    bg.add(it)
    box.add(it)
    box.add(Box.createVerticalStrut(5))
  }

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
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
