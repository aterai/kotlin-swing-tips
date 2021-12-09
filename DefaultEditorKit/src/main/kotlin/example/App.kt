package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val pf1 = JTextField(30)
  pf1.componentPopupMenu = TextFieldPopupMenu()
  val pf2 = JTextField(30)
  pf2.componentPopupMenu = TextFieldPopupMenu()
  val panel = Box.createVerticalBox()
  panel.border = BorderFactory.createTitledBorder("E-mail Address")
  panel.add(pf1)
  panel.add(Box.createVerticalStrut(5))
  panel.add(JLabel("Please enter your email address twice for confirmation:"))
  panel.add(pf2)
  panel.add(Box.createVerticalStrut(5))
  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(panel, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea("Dummy")))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextFieldPopupMenu : JPopupMenu() {
  init {
    add(DefaultEditorKit.CutAction())
    add(DefaultEditorKit.CopyAction())
    add(DefaultEditorKit.PasteAction())
    add("delete").addActionListener {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
    addSeparator()
    add("cut2").addActionListener {
      (invoker as? JTextComponent)?.cut()
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText == null
      for (menuElement in subElements) {
        val m = menuElement.component
        if (m is JMenuItem && m.action is DefaultEditorKit.PasteAction) {
          continue
        }
        m.isEnabled = hasSelectedText
      }
      super.show(c, x, y)
    }
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
