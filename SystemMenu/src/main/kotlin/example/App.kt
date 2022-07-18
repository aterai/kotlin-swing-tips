package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()
  EventQueue.invokeLater {
    val menu = descendants(log.rootPane)
      .filterIsInstance<JMenu>()
      .firstOrNull()
      ?: JMenu(" ")
    menu.add("added to the SystemMenu")
    log.append("${menu.preferredSize}\n")
    menu.icon = UIManager.getIcon("InternalFrame.icon")
    log.append("${menu.preferredSize}\n----\n")
    var c: Component? = menu
    while (c != null) {
      log.append("${c.javaClass.name}\n")
      c = c.parent
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

fun main() {
  EventQueue.invokeLater {
    JFrame.setDefaultLookAndFeelDecorated(true)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
