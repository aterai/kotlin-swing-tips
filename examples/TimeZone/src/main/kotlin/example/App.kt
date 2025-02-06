package example

import java.awt.*
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.swing.*

fun makeUI(): Component {
  val format = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
  val df = DateFormat.getDateTimeInstance()
  // df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"))
  df.timeZone = TimeZone.getDefault()

  val field = JTextField(30)
  field.text = format.format(Date())

  val formatButton = JButton("format")
  formatButton.addActionListener { field.text = format.format(Date()) }

  val textArea = JTextArea()
  textArea.isEditable = false

  val parseButton = JButton("parse")
  parseButton.addActionListener {
    val date = format.parse(field.text.trim(), ParsePosition(0))
    val str = date?.let { df.format(date) } ?: "error"
    textArea.append(str + "\n")
  }

  val bp = JPanel(GridLayout(1, 0, 2, 2))
  bp.add(formatButton)
  bp.add(parseButton)
  val c = GridBagConstraints()
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("DateFormat")
  c.insets = Insets(2, 2, 2, 2)
  c.fill = GridBagConstraints.HORIZONTAL
  c.anchor = GridBagConstraints.LINE_END
  c.weightx = 1.0
  p.add(field, c)
  c.insets = Insets(2, 0, 2, 2)
  c.fill = GridBagConstraints.NONE
  c.weightx = 0.0
  c.gridy = 1
  p.add(bp, c)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
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
