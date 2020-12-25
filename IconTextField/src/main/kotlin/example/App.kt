package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val image = ImageIcon(cl.getResource("example/16x16.png"))
  val label1 = JLabel(image)
  val field1 = object : JTextField("1111111111111111") {
    override fun updateUI() {
      super.updateUI()
      add(label1)
    }
  }
  val w = image.iconWidth
  var m = field1.margin
  field1.margin = Insets(m.top, m.left + w, m.bottom, m.right)
  label1.cursor = Cursor.getDefaultCursor()
  label1.border = BorderFactory.createEmptyBorder()
  label1.setBounds(m.left, m.top, w, image.iconHeight)

  val label2 = JLabel(image)
  label2.cursor = Cursor.getDefaultCursor()
  label2.border = BorderFactory.createEmptyBorder()
  val field2 = object : JTextField("2222222222222222222222222222222222222") {
    override fun updateUI() {
      super.updateUI()
      removeAll()
      val l = SpringLayout()
      layout = l
      val fw = l.getConstraint(SpringLayout.WIDTH, this)
      val fh = l.getConstraint(SpringLayout.HEIGHT, this)
      val c = l.getConstraints(label2)
      c.setConstraint(SpringLayout.WEST, fw)
      c.setConstraint(SpringLayout.SOUTH, fh)
      add(label2)
    }
  }
  m = field2.margin
  field2.margin = Insets(m.top + 2, m.left, m.bottom, m.right + w)

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("Default", JTextField("000000000000")))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("add Image(JLabel)", field1))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("SpringLayout", field2))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
