package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JInternalFrame.JDesktopIcon

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.desktopManager = object : DefaultDesktopManager() {
    override fun iconifyFrame(f: JInternalFrame) {
      val r = getBoundsForIconOf(f)
      r.width = f.desktopIcon.preferredSize.width
      f.desktopIcon.bounds = r
      super.iconifyFrame(f)
    }
  }
  desktop.add(createFrame("looooooooooooong title #", 1))
  desktop.add(createFrame("#", 0))

  val act = object : AbstractAction("add") {
    private var num = 2
    override fun actionPerformed(e: ActionEvent) {
      desktop.add(createFrame("#", num++))
    }
  }
  val button = JButton(act)

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun createFrame(t: String, i: Int): JInternalFrame {
  val f = JInternalFrame(t + i, true, true, true, true)
  f.desktopIcon = object : JDesktopIcon(f) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      val title = f.title
      val font = font
      if (font != null) {
        testWidth()
        var buttonsW = 22
        if (f.isClosable) {
          buttonsW += 19
        }
        if (f.isMaximizable) {
          buttonsW += 19
        }
        if (f.isIconifiable) {
          buttonsW += 19
        }
        val fm = getFontMetrics(font)
        val titleW = SwingUtilities.computeStringWidth(fm, title)
        val ins = insets
        // 2: Magic number of gap between icons
        d.width = buttonsW + ins.left + ins.right + titleW + 2 + 2 + 2
        // 27: Magic number for NimbusLookAndFeel
        d.height = 27.coerceAtMost(d.height)
        println("BasicInternalFrameTitlePane: " + d.width)
      }
      return d
    }

    private fun testWidth() {
      val dim = layout.minimumLayoutSize(this)
      println("minimumLayoutSize: " + dim.width)
      val bw = descendants(this).filterIsInstance<AbstractButton>().sumOf {
        it.preferredSize.width
      }
      println("Total width of all buttons: $bw")
    }
  }
  f.setSize(200, 100)
  f.setLocation(5 + 40 * i, 5 + 50 * i)
  EventQueue.invokeLater { f.isVisible = true }
  return f
}

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

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
