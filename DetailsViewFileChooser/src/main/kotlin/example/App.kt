package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")

  val listView = JButton("List View(Default)")
  listView.addActionListener {
    val chooser = JFileChooser()
    val retValue = chooser.showOpenDialog(p)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val detailsView = JButton("Details View")
  detailsView.addActionListener { e ->
    val chooser = JFileChooser()
    chooser.actionMap["viewTypeDetails"]?.actionPerformed(
      ActionEvent(e.source, e.id, "viewTypeDetails")
    )
    val retValue = chooser.showOpenDialog(p)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }
  p.add(listView)
  p.add(detailsView)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
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
