package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.filechooser.FileNameExtensionFilter

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textArea = JTextArea().apply {
      setEditable(false)
      setLineWrap(true)
    }

    val label = JLabel().apply {
      setHorizontalAlignment(SwingConstants.CENTER)
    }

    val p = JPanel(GridLayout(2, 1, 5, 5)).apply {
      add(JScrollPane(textArea).apply {
        setBorder(BorderFactory.createTitledBorder("File -> String"))
      })
      add(JScrollPane(label).apply {
        setBorder(BorderFactory.createTitledBorder("JTextArea -> ImageIcon"))
      })
    }

    val encode = JButton("encode")
    encode.addActionListener {
      val chooser = JFileChooser()
      chooser.addChoosableFileFilter(FileNameExtensionFilter("PNG (*.png)", "png"))
      val retvalue = chooser.showOpenDialog(encode)
      if (retvalue == JFileChooser.APPROVE_OPTION) {
        val path = chooser.getSelectedFile().toPath()
        try {
          textArea.setText(Base64.getEncoder().encodeToString(Files.readAllBytes(path)))
        } catch (ex: IOException) {
          ex.printStackTrace()
        }
      }
    }
    val decode = JButton("decode")
    decode.addActionListener {
      val b64 = textArea.getText()
      if (!b64.isEmpty()) {
        try {
          val dec = Base64.getDecoder().decode(b64.toByteArray(StandardCharsets.ISO_8859_1))
          ByteArrayInputStream(dec).use { bais ->
            label.setIcon(ImageIcon(ImageIO.read(bais)))
          }
        } catch (ex: IOException) {
          ex.printStackTrace()
        }
      }
    }

    val box = JPanel(GridLayout(1, 2, 5, 5)).apply {
      setBorder(BorderFactory.createTitledBorder("java.util.Base64"))
      add(encode)
      add(decode)
    }

    add(box, BorderLayout.NORTH)
    add(p)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
