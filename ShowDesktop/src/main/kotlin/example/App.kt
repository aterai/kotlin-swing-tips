package example

import java.awt.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*

fun makeUI(): Component {
  val desktop = JDesktopPane()

  val button1 = JToggleButton("Iconify Frames")
  button1.addActionListener { e ->
    val m = desktop.desktopManager
    if ((e.source as? AbstractButton)?.isSelected == true) {
      desktop.allFrames.reversed().forEach(m::iconifyFrame)
    } else {
      desktop.allFrames.forEach(m::deiconifyFrame)
    }
  }

  val button2 = JToggleButton("Show Desktop")
  button2.addActionListener { e ->
    val show = (e.source as? AbstractButton)?.isSelected == true
    desktop.allFrames.reversed().forEach { it.isVisible = !show }
  }

  val idx = AtomicInteger()
  desktop.add(createFrame(idx.getAndIncrement()))

  val button3 = JButton("add")
  button3.addActionListener {
    desktop.add(createFrame(idx.getAndIncrement()))
  }

  val mb = JMenuBar().also {
    it.add(button1)
    it.add(Box.createHorizontalStrut(2))
    it.add(button2)
    it.add(Box.createHorizontalGlue())
    it.add(button3)
  }

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(i: Int) = JInternalFrame("#$i", true, true, true, true).also {
  it.setSize(200, 100)
  it.setLocation(i * 16, i * 24)
  EventQueue.invokeLater { it.isVisible = true }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
