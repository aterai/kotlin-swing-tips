package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()

  val b1 = JButton("Default")
  b1.addActionListener { e ->
    val chooser = JFileChooser()
    val ret = chooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      log.append(chooser.selectedFile.toString() + "\n")
    }
  }

  val b2 = JButton("Resizable(false)")
  b2.addActionListener { e ->
    val chooser = object : JFileChooser() {
      override fun createDialog(parent: Component) = super.createDialog(parent).also {
        it.isResizable = false
      }
    }
    val ret = chooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      log.append(chooser.selectedFile.toString() + "\n")
    }
  }

  val b3 = JButton("MinimumSize(640, 480)")
  b3.addActionListener { e ->
    val chooser = object : JFileChooser() {
      override fun createDialog(parent: Component) = super.createDialog(parent).also {
        it.minimumSize = Dimension(640, 480)
      }
    }
    val ret = chooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      log.append(chooser.selectedFile.toString() + "\n")
    }
  }

  val p1 = JPanel()
  p1.add(b1)
  p1.add(b2)
  val p2 = JPanel()
  p2.add(b3)

  val p = JPanel(GridLayout(2, 1))
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(p1)
  p.add(p2)

  return JPanel(BorderLayout(2, 2)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
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
