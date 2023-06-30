package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val path = "example/16x16.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = ImageIcon(img)
  val label1 = JLabel(icon)
  val field1 = object : JTextField("1111111111111111") {
    override fun updateUI() {
      super.updateUI()
      add(label1)
    }
  }
  val w = icon.iconWidth
  var m = field1.margin
  field1.margin = Insets(m.top, m.left + w, m.bottom, m.right)
  label1.cursor = Cursor.getDefaultCursor()
  label1.border = BorderFactory.createEmptyBorder()
  label1.setBounds(m.left, m.top, w, icon.iconHeight)

  val label2 = JLabel(icon)
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

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 8 - w / 2, 8 - h / 2)
  g2.dispose()
  return bi
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
