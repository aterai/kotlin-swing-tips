package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicInternalFrameUI

fun makeUI() = JDesktopPane().also {
  addFrame(it, 0)
  addFrame(it, 1)
  it.preferredSize = Dimension(320, 240)
}

private fun addFrame(desktop: JDesktopPane, idx: Int) {
  val titleAlignment = if (idx == 0) "CENTER" else "LEADING"
  val frame = JInternalFrame("title: $titleAlignment", true, true, true, true)
  val ui = frame.ui as BasicInternalFrameUI
  val titleBar = ui.northPane
  val d = UIDefaults()
  d["InternalFrame:InternalFrameTitlePane.titleAlignment"] = titleAlignment
  titleBar.putClientProperty("Nimbus.Overrides", d)
  frame.add(makePanel())
  frame.setSize(240, 100)
  frame.isVisible = true
  frame.setLocation(10 + 60 * idx, 10 + 120 * idx)
  desktop.add(frame)
  desktop.desktopManager.activateFrame(frame)
}

private fun makePanel() = JPanel().also {
  it.add(JLabel("label"))
  it.add(JButton("button"))
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
