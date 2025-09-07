package example

import java.awt.*
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val mb = makeMenuBar()
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  val file = JMenu("File")
  file.add("New")
  file.add("Open")
  file.addSeparator()
  file.add(makeRecentMenu())
  menuBar.add(file)
  menuBar.add(JMenu("Edit"))
  return menuBar
}

private fun makeRecentMenu(): JMenu {
  val menu = JMenu("Recent Files")
  val field = JTextField(20)
  menu.add(field)
  field.document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
      filter(menu, field)
    }

    override fun removeUpdate(e: DocumentEvent?) {
      filter(menu, field)
    }

    override fun changedUpdate(e: DocumentEvent?) {
      // not needed
    }
  })
  menu.add("aa001.txt")
  menu.add("aa002.log")
  menu.add("aabb33.txt")
  menu.add("abc4.md")
  menu.add("b5.markdown")
  menu.add("ccc6.txt")
  return menu
}

private fun getPattern(field: JTextField): Pattern? {
  val regex = field.getText()
  var pattern: Pattern? = null
  if (!regex.isNullOrBlank()) {
    runCatching {
      pattern = Pattern.compile(regex)
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(field)
    }
  }
  return pattern
}

private fun filter(menu: JMenu, field: JTextField) {
  val ptn = getPattern(field)
  menu
    .getPopupMenu()
    .getSubElements()
    .filterIsInstance<JMenuItem>()
    .forEach {
      it.isVisible = ptn == null || ptn.matcher(it.text).find()
    }
  menu.getPopupMenu().pack()
  EventQueue.invokeLater { field.requestFocusInWindow() }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
