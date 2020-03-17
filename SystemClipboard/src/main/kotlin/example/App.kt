package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label = JLabel()

  val button = JButton("get Clipboard DataFlavor")
  button.addActionListener {
    var str: String? = ""
    var image: ImageIcon? = null
    runCatching {
      val t = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
      if (t == null) {
        Toolkit.getDefaultToolkit().beep()
        return@addActionListener
      }
      if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        image = ImageIcon(t.getTransferData(DataFlavor.imageFlavor) as? Image)
      } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        str = t.getTransferData(DataFlavor.stringFlavor).toString()
      }
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
      str = it.message
      image = null
    }
    label.text = str
    label.icon = image
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(label))
    it.add(button, BorderLayout.SOUTH)
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
