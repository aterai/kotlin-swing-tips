package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.filechooser.FileNameExtensionFilter

fun makeUI(): Component {
  val textArea = JTextArea().also {
    it.setEditable(false)
    it.setLineWrap(true)
  }

  val label = JLabel().also {
    it.setHorizontalAlignment(SwingConstants.CENTER)
  }

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.add(JScrollPane(textArea).also {
    it.setBorder(BorderFactory.createTitledBorder("File -> String"))
  })
  p.add(JScrollPane(label).also {
    it.setBorder(BorderFactory.createTitledBorder("JTextArea -> ImageIcon"))
  })

  val encode = JButton("encode")
  encode.addActionListener {
    val chooser = JFileChooser()
    chooser.addChoosableFileFilter(FileNameExtensionFilter("PNG (*.png)", "png"))
    val retValue = chooser.showOpenDialog(encode)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      val path = chooser.getSelectedFile().toPath()
      runCatching {
        textArea.setText(Base64.getEncoder().encodeToString(Files.readAllBytes(path)))
      }.onFailure {
        textArea.setText(it.message)
      }
    }
  }
  val decode = JButton("decode")
  decode.addActionListener {
    val b64 = textArea.getText()
    if (b64.isNotEmpty()) {
      runCatching {
        val dec = Base64.getDecoder().decode(b64.toByteArray(StandardCharsets.ISO_8859_1))
        ByteArrayInputStream(dec).use {
          label.setIcon(ImageIcon(ImageIO.read(it)))
        }
      }.onFailure {
        label.setIcon(null)
        label.setText(it.message)
      }
    }
  }

  val box = JPanel(GridLayout(1, 2, 5, 5)).also {
    it.setBorder(BorderFactory.createTitledBorder("java.util.Base64"))
    it.add(encode)
    it.add(decode)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
