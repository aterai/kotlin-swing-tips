package example

import java.awt.*
import java.util.Locale
import javax.swing.*

private val LOCALE_ARRAY = arrayOf(
  Locale.ENGLISH,
  Locale.FRENCH,
  Locale.GERMAN,
  Locale.ITALIAN,
  Locale.JAPANESE,
  Locale.KOREAN,
  Locale.CHINESE,
  Locale.SIMPLIFIED_CHINESE,
  Locale.TRADITIONAL_CHINESE,
  Locale.FRANCE,
  Locale.GERMANY,
  Locale.ITALY,
  Locale.JAPAN,
  Locale.KOREA,
  Locale.CHINA,
  Locale.PRC,
  Locale.TAIWAN,
  Locale.UK,
  Locale.US,
  Locale.CANADA,
  Locale.CANADA_FRENCH
)

fun makeUI(): Component {
  val log = JTextArea()

  UIManager.put("FileChooser.readOnly", true)
  val combo = JComboBox(LOCALE_ARRAY)
  val fileChooser = JFileChooser()
  val button = JButton("<-")
  button.addActionListener {
    fileChooser.locale = combo.getItemAt(combo.selectedIndex)
    SwingUtilities.updateComponentTreeUI(fileChooser)
    val retValue = fileChooser.showOpenDialog(combo.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("${fileChooser.selectedFile}\n")
    }
  }

  val p = JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createTitledBorder("Open JFileChooser")
    it.add(combo)
    it.add(button, BorderLayout.EAST)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
