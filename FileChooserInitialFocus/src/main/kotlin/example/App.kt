package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val fileChooser = JFileChooser()
  val field = JTextField("C:/temp/test.txt")
  val log = JTextArea()

  val radio = JRadioButton("set initial focus on JTextField", true)
  val bg = ButtonGroup()
  val p2 = JPanel()

  listOf(JRadioButton("default"), radio).forEach {
    bg.add(it)
    p2.add(it)
  }

  val button = JButton("JFileChooser")
  button.addActionListener {
    fileChooser.selectedFile = File(field.text.trim())
    if (radio.isSelected) {
      EventQueue.invokeLater {
        descendants(fileChooser)
          .filterIsInstance<JTextField>()
          .first()
          .also {
            it.selectAll()
            it.requestFocusInWindow()
          }
      }
    }
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      val path = fileChooser.selectedFile.absolutePath
      field.text = path
      log.append("$path\n")
    }
  }

  val p1 = JPanel(BorderLayout(5, 5))
  p1.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p1.add(field)
  p1.add(button, BorderLayout.EAST)

  val p = JPanel(BorderLayout())
  p.add(p1, BorderLayout.NORTH)
  p.add(p2, BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
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
