package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val desktop = JDesktopPane()

  val num = AtomicInteger()
  val button = JButton("add")
  button.addActionListener { addInternalFrame(desktop, num.getAndIncrement()) }

  val lv = button.multiClickThreshhold
  val m = SpinnerNumberModel(lv, 0L, 10_000L, 100L)
  m.addChangeListener {
    button.multiClickThreshhold = m.number.toLong()
  }

  val mb = JMenuBar()
  mb.add(JLabel("MultiClickThreshhold: "))
  mb.add(JSpinner(m))
  mb.add(Box.createHorizontalGlue())
  mb.add(button)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addInternalFrame(desktop: JDesktopPane, idx: Int) {
  val f = JInternalFrame("#$idx", true, true, true, true)
  f.setBounds(idx * 10, idx * 10, 200, 100)
  f.isVisible = true
  desktop.add(f)
  desktop.desktopManager.activateFrame(f)
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
