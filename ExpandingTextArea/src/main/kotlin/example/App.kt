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
  box.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30))
  box.add(makeExpandingTextArea1())
  box.add(Box.createVerticalStrut(10))
  box.add(makeExpandingTextArea2())

  it.add(box, BorderLayout.NORTH)
  it.add(JButton("focus dummy"), BorderLayout.SOUTH)
  it.setPreferredSize(Dimension(320, 240))
}

private fun makeExpandingTextArea1(): Component {
  val p = JPanel(BorderLayout())
  val textArea = JTextArea(TEXT, 1, 10)
  textArea.setLineWrap(true)
  textArea.addFocusListener(object : FocusListener {
    override fun focusGained(e: FocusEvent) {
      (e.getComponent() as? JTextArea)?.setRows(3)
      p.revalidate()
    }

    override fun focusLost(e: FocusEvent) {
      (e.getComponent() as? JTextArea)?.setRows(1)
      p.revalidate()
    }
  })
  val scroll = JScrollPane(textArea)
  // scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
  scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  p.add(scroll, BorderLayout.NORTH)
  return p
}

private fun makeExpandingTextArea2(): Component {
  val cardLayout = CardLayout()
  val cp = JPanel(cardLayout)

  val textArea = object : JTextArea(TEXT, 3, 10) {
    override fun updateUI() {
      super.updateUI()
      setLineWrap(true)
      setWrapStyleWord(true)
      setMargin(Insets(1, 1, 1, 1))
    }
  }

  val textField = object : JLabel(TEXT) {
    override fun updateUI() {
      super.updateUI()
      setOpaque(true)
      setFocusable(true)
      setBackground(UIManager.getColor("TextField.background"))
      setForeground(UIManager.getColor("TextField.foreground"))
      setBorder(
        BorderFactory.createCompoundBorder(
          UIManager.getBorder("TextField.border"), BorderFactory.createEmptyBorder(1, 1, 1, 1)
        )
      )
      setFont(UIManager.getFont("TextArea.font"))
    }
  }

  textArea.addFocusListener(object : FocusAdapter() {
    override fun focusLost(e: FocusEvent) {
      val text = textArea.getText()
      textField.setText(if (text.isEmpty()) " " else text)
      cardLayout.show(cp, "TextField")
    }
  })
  textField.addFocusListener(object : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
      cardLayout.show(cp, "TextArea")
      textArea.requestFocusInWindow()
    }
  })
  textField.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      cardLayout.show(cp, "TextArea")
      textArea.requestFocusInWindow()
    }
  })
  val panel = JPanel(BorderLayout())
  panel.add(textField, BorderLayout.NORTH)
  val scroll = JScrollPane(textArea)
  scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
  scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  cp.add(panel, "TextField")
  cp.add(scroll, "TextArea")
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
