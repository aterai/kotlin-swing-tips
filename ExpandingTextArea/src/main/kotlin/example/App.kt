package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val TEXT = "The quick brown fox jumps over the lazy dog."

fun makeUI() = JPanel(BorderLayout()).also {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 30, 10, 30)
  box.add(makeExpandingTextArea1())
  box.add(Box.createVerticalStrut(10))
  box.add(makeExpandingTextArea2())

  it.add(box, BorderLayout.NORTH)
  it.add(JButton("focus dummy"), BorderLayout.SOUTH)
  it.preferredSize = Dimension(320, 240)
}

private fun makeExpandingTextArea1(): Component {
  val p = JPanel(BorderLayout())
  val textArea = JTextArea(TEXT, 1, 10)
  textArea.lineWrap = true
  textArea.addFocusListener(object : FocusListener {
    override fun focusGained(e: FocusEvent) {
      (e.component as? JTextArea)?.rows = 3
      p.revalidate()
    }

    override fun focusLost(e: FocusEvent) {
      (e.component as? JTextArea)?.rows = 1
      p.revalidate()
    }
  })
  val scroll = JScrollPane(textArea)
  // scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  p.add(scroll, BorderLayout.NORTH)
  return p
}

private fun makeExpandingTextArea2(): Component {
  val cardLayout = CardLayout()
  val cp = JPanel(cardLayout)

  val textArea = object : JTextArea(TEXT, 3, 10) {
    override fun updateUI() {
      super.updateUI()
      lineWrap = true
      wrapStyleWord = true
      margin = Insets(1, 1, 1, 1)
    }
  }

  val textField = object : JLabel(TEXT) {
    override fun updateUI() {
      super.updateUI()
      isOpaque = true
      isFocusable = true
      background = UIManager.getColor("TextField.background")
      foreground = UIManager.getColor("TextField.foreground")
      border = BorderFactory.createCompoundBorder(
        UIManager.getBorder("TextField.border"), BorderFactory.createEmptyBorder(1, 1, 1, 1)
      )
      font = UIManager.getFont("TextArea.font")
    }
  }

  val keyCollapse = "TextField"
  val keyExpand = "TextArea"
  textArea.addFocusListener(object : FocusAdapter() {
    override fun focusLost(e: FocusEvent) {
      val text = textArea.text
      // textField.text = if (text.isEmpty()) " " else text
      textField.text = text.takeIf { it.isNotEmpty() } ?: " "
      cardLayout.show(cp, keyCollapse)
    }
  })
  textField.addFocusListener(object : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
      cardLayout.show(cp, keyExpand)
      textArea.requestFocusInWindow()
    }
  })
  textField.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      cardLayout.show(cp, keyExpand)
      textArea.requestFocusInWindow()
    }
  })
  val panel = JPanel(BorderLayout())
  panel.add(textField, BorderLayout.NORTH)
  val scroll = JScrollPane(textArea)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  cp.add(panel, keyCollapse)
  cp.add(scroll, keyExpand)
  return cp
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
