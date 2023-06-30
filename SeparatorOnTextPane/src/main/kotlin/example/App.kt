package example

import java.awt.*
import javax.swing.*
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

fun makeUI(): Component {
  val kit = HTMLEditorKit()
  val doc = HTMLDocument()
  val textPane = JTextPane()
  textPane.editorKit = kit
  textPane.document = doc
  textPane.isEditable = false
  textPane.text = "<html>&lt;hr&gt;:<hr />"
  textPane.insertComponent(JLabel("JSeparator: "))
  textPane.insertComponent(JSeparator(SwingConstants.HORIZONTAL))
  insertBr(kit, doc)

  textPane.insertComponent(JLabel("MatteBorder1: "))
  val label1 = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.RED)
    }

    override fun getMaximumSize() = Dimension(textPane.size.width, 1)
  }
  textPane.insertComponent(label1)
  insertBr(kit, doc)

  textPane.insertComponent(JLabel("MatteBorder2: "))
  val label2 = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GREEN)
    }

    override fun getPreferredSize() = Dimension(textPane.size.width, 1)

    override fun getMaximumSize() = this.preferredSize
  }
  textPane.insertComponent(label2)
  insertBr(kit, doc)

  textPane.insertComponent(JLabel("SwingConstants.VERTICAL "))
  val separator = object : JSeparator(VERTICAL) {
    override fun getPreferredSize() = Dimension(1, 16)

    override fun getMaximumSize() = this.preferredSize
  }
  textPane.insertComponent(separator)
  textPane.insertComponent(JLabel(" TEST"))

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun insertBr(kit: HTMLEditorKit, doc: HTMLDocument) {
  runCatching {
    kit.insertHTML(doc, doc.length, "<br />", 0, 0, null)
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
