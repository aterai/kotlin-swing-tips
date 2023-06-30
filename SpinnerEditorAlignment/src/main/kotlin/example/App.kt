package example

import java.awt.*
import java.awt.event.ItemListener
import java.util.Calendar
import java.util.Date
import javax.swing.*
import javax.swing.JSpinner.DateEditor

val r1 = JRadioButton("LEADING")
val r2 = JRadioButton("CENTER")
val r3 = JRadioButton("TRAILING")

fun makeUI(): Component {
  val il = ItemListener { e ->
    val alignment = when {
      e.itemSelectable === r1 -> SwingConstants.LEADING
      e.itemSelectable === r2 -> SwingConstants.CENTER
      else -> SwingConstants.TRAILING
    }
    UIManager.put("Spinner.editorAlignment", alignment)
    SwingUtilities.updateComponentTreeUI(r1.rootPane)
  }
  val bg = ButtonGroup()
  val box = Box.createHorizontalBox()
  for (r in listOf(r1, r2, r3)) {
    r.addItemListener(il)
    bg.add(r)
    box.add(r)
  }
  val weeks = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Sat")
  val spinner0 = JSpinner(SpinnerListModel(weeks))
  val date = Date()
  val spinner1 = JSpinner(SpinnerDateModel(date, date, null, Calendar.DAY_OF_MONTH))
  spinner1.editor = DateEditor(spinner1, "yyyy/MM/dd")

  val spinner2 = JSpinner(SpinnerNumberModel(5, 0, 10, 1))

  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.gridheight = 1
  c.gridwidth = 1
  c.gridx = 0
  c.weightx = 0.0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.EAST
  c.gridy = 0
  p.add(JLabel("SpinnerListModel: "), c)
  c.gridy = 1
  p.add(JLabel("SpinnerDateModel: "), c)
  c.gridy = 2
  p.add(JLabel("SpinnerNumberModel: "), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.gridy = 0
  p.add(spinner0, c)
  c.gridy = 1
  p.add(spinner1, c)
  c.gridy = 2
  p.add(spinner2, c)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    EventQueue.invokeLater {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtils.createLookAndFeelMenu())
      it.rootPane.jMenuBar = mb
    }
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
