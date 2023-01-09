package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val button0 = JButton("Default")
  button0.addActionListener { e ->
    val p = (e.source as? JComponent)?.rootPane
    JOptionPane.showMessageDialog(p, "message0", "title0", JOptionPane.PLAIN_MESSAGE)
  }

  val label1 = JLabel("message1")
  label1.addHierarchyListener { e ->
    val c = e.component
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && c.isShowing) {
      val o = SwingUtilities.getAncestorOfClass(JOptionPane::class.java, c)
      (o as? JOptionPane)?.preferredSize = Dimension(120, 120)
      val w = SwingUtilities.getWindowAncestor(c)
      w.pack()
      w.setLocationRelativeTo(button0.rootPane)
    }
  }
  val button1 = JButton("HierarchyListener + setPreferredSize")
  button1.addActionListener { e ->
    val p = (e.source as? JComponent)?.rootPane
    JOptionPane.showMessageDialog(p, label1, "title1(120*120)", JOptionPane.PLAIN_MESSAGE)
  }

  val key = "OptionPane.minimumSize"
  val button2 = JButton(key)
  button2.addActionListener { e ->
    UIManager.put(key, Dimension(120, 120))
    val p = (e.source as? JComponent)?.rootPane
    JOptionPane.showMessageDialog(p, "message3", "title3(120*120)", JOptionPane.PLAIN_MESSAGE)
    UIManager.put(key, UIManager.getLookAndFeelDefaults().getDimension(key))
  }

  val button3 = JButton("$key + JTextArea")
  button3.addActionListener { e ->
    UIManager.put(key, Dimension(120, 120))
    val p = (e.source as? JComponent)?.rootPane
    val s = JScrollPane(JTextArea(10, 30))
    JOptionPane.showMessageDialog(p, s, "title4(120*120)", JOptionPane.PLAIN_MESSAGE)
    UIManager.put(key, UIManager.getLookAndFeelDefaults().getDimension(key))
  }

  return JPanel().also {
    listOf(button0, button1, button2, button3).forEach { c -> it.add(c) }
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
