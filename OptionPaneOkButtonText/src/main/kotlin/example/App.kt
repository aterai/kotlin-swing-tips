package example

import java.awt.*
import java.awt.event.HierarchyEvent
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("OptionPane.okButtonText", "back")

  val button1 = JButton("Default")
  button1.addActionListener { e ->
    val p = (e.source as? JComponent)?.rootPane
    JOptionPane.showMessageDialog(p, "Default", "title0", JOptionPane.PLAIN_MESSAGE)
  }
  val label2 = JLabel("JButton#setFocusPainted(false)")
  label2.addHierarchyListener { e ->
    val c = e.component
    val r = (c as? JComponent)?.rootPane
    val b = e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
    if (b && c.isShowing && r != null) {
      descendants(r).filterIsInstance<JButton>().firstOrNull()?.also {
        it.isFocusPainted = false
        it.text = "back2"
      }
    }
  }

  val button2 = JButton("showMessageDialog + HierarchyListener")
  button2.addActionListener { e ->
    val p = (e.source as? JComponent)?.rootPane
    JOptionPane.showMessageDialog(p, label2, "title2", JOptionPane.PLAIN_MESSAGE)
  }

  val button3 = JButton("showOptionDialog")
  button3.addActionListener { e ->
    val options = arrayOf("Yes, please")
    JOptionPane.showOptionDialog(
      (e.source as? JComponent)?.rootPane,
      "Would you like green eggs and ham?",
      "A Silly Question",
      JOptionPane.OK_OPTION,
      JOptionPane.PLAIN_MESSAGE,
      null,
      options,
      options[0]
    )
  }

  return JPanel().also {
    listOf(button1, button2, button3).forEach { b ->
      it.add(b)
    }
    it.border = BorderFactory.createEmptyBorder(5, 32, 5, 32)
    it.preferredSize = Dimension(320, 240)
  }
}

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

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
