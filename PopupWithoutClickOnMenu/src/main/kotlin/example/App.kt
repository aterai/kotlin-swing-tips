package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("JMenu: hover(show popup automatically) on cursor", true)
  val handler = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (check.isSelected()) {
        (e.getComponent() as? AbstractButton)?.doClick()
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      if (check.isSelected()) {
        (e.getComponent() as? AbstractButton)?.doClick()
      }
    }
  }
  val menuBar = makeMenuBar()
  menuBar.getSubElements()
    .filterIsInstance<JMenu>()
    .forEach { it.addMouseListener(handler) }

  val p = JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
  }
  EventQueue.invokeLater { p.getRootPane().setJMenuBar(menuBar) }
  return p
}

fun makeMenuBar() = JMenuBar().also {
  it.add(JMenu("File")).also { menu ->
    menu.add("Open")
    menu.add("Save")
    menu.add("Exit")
  }

  it.add(JMenu("Edit")).also { menu ->
    menu.add("Undo")
    menu.add("Redo")
    menu.addSeparator()
    menu.add("Cut")
    menu.add("Copy")
    menu.add("Paste")
    menu.add("Delete")
  }

  it.add(JMenu("Test")).also { menu ->
    menu.add("JMenuItem1")
    menu.add("JMenuItem2")
    menu.add(JMenu("JMenu").also { sub ->
      sub.add("JMenuItem4")
      sub.add("JMenuItem5")
    })
    menu.add("JMenuItem3")
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
