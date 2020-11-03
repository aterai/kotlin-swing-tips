package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val progressBar = JProgressBar()
  progressBar.isIndeterminate = true

  val p = JPanel(GridBagLayout())
  p.add(progressBar)

  val ctv = UIManager.getInt("ProgressBar.cycleTime")
  val cycleTime = SpinnerNumberModel(ctv, 1000, 10_000, 100)
  val riv = UIManager.getInt("ProgressBar.repaintInterval")
  val repaintInterval = SpinnerNumberModel(riv, 10, 100, 10)

  val button = JButton("UIManager.put")
  button.addActionListener {
    progressBar.isIndeterminate = false
    UIManager.put("ProgressBar.repaintInterval", repaintInterval.number.toInt())
    UIManager.put("ProgressBar.cycleTime", cycleTime.number.toInt())
    progressBar.isIndeterminate = true
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)

  val sp = JPanel(GridLayout(3, 2, 5, 5))
  sp.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  sp.add(JLabel("ProgressBar.cycleTime:", SwingConstants.RIGHT))
  sp.add(JSpinner(cycleTime))
  sp.add(JLabel("ProgressBar.repaintInterval:", SwingConstants.RIGHT))
  sp.add(JSpinner(repaintInterval))
  sp.add(Box.createHorizontalStrut(5))
  sp.add(box)

  return JPanel(BorderLayout()).also {
    it.add(sp, BorderLayout.NORTH)
    it.add(p)
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
