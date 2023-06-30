package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val button1 = JButton("JOptionPane.showMessageDialog")
  button1.addActionListener {
    JOptionPane.showMessageDialog(button1.rootPane, "showMessageDialog")
  }

  val button2 = JButton("Default")
  button2.addActionListener {
    val dialog = JDialog(JOptionPane.getFrameForComponent(button2.rootPane), "title", true)
    val act = object : AbstractAction("OK") {
      override fun actionPerformed(e: ActionEvent) {
        dialog.dispose()
      }
    }
    dialog.contentPane.add(makePanel(act))
    dialog.pack()
    dialog.isResizable = false
    dialog.setLocationRelativeTo(button2.rootPane)
    dialog.isVisible = true
  }

  val button3 = JButton("close JDialog with ESC key")
  button3.addActionListener {
    val dialog = JDialog(JOptionPane.getFrameForComponent(button3.rootPane), "title", true)
    val act = object : AbstractAction("OK") {
      override fun actionPerformed(e: ActionEvent) {
        dialog.dispose()
      }
    }
    val imap = dialog.rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-it")
    dialog.rootPane.actionMap.put("close-it", act)
    dialog.contentPane.add(makePanel(act))
    dialog.pack()
    dialog.isResizable = false
    dialog.setLocationRelativeTo(button3.rootPane)
    dialog.isVisible = true
  }

  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("JOptionPane")
  p1.add(button1)

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("JDialog")
  p2.add(button2)
  p2.add(button3)

  return JPanel(GridLayout(2, 1)).also {
    it.add(p1)
    it.add(p2)
    it.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makePanel(act: Action?): Component {
  val p = object : JPanel(GridBagLayout()) {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.width = 240.coerceAtLeast(it.width)
    }
  }
  val c = GridBagConstraints()
  c.insets = Insets(5, 10, 5, 10)
  c.anchor = GridBagConstraints.LINE_START
  p.add(JLabel(ColorIcon(Color.RED)), c)
  // p.add(JLabel(UIManager.getIcon("OptionPane.informationIcon")), c)
  c.insets = Insets(5, 0, 5, 0)
  p.add(JLabel("Message"), c)
  c.gridwidth = 2
  c.gridy = 1
  c.weightx = 1.0
  c.anchor = GridBagConstraints.CENTER
  c.fill = GridBagConstraints.NONE
  p.add(JButton(act), c)
  return p
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillOval(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
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
