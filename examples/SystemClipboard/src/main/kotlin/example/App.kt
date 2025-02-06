package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import javax.swing.*

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
        str = "the current transferable object on the clipboard is null"
      } else {
        if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
          image = ImageIcon(t.getTransferData(DataFlavor.imageFlavor) as? Image)
        } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          str = t.getTransferData(DataFlavor.stringFlavor).toString()
        }
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
