package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
  it.add(JScrollPane(makeTestBox()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeTestBox() = Box.createVerticalBox().also {
  it.add(makeSystemColor(SystemColor.desktop, "desktop"))
  it.add(makeSystemColor(SystemColor.activeCaption, "activeCaption"))
  it.add(makeSystemColor(SystemColor.inactiveCaption, "inactiveCaption"))
  it.add(makeSystemColor(SystemColor.activeCaptionText, "activeCaptionText"))
  it.add(makeSystemColor(SystemColor.inactiveCaptionText, "inactiveCaptionText"))
  it.add(makeSystemColor(SystemColor.activeCaptionBorder, "activeCaptionBorder"))
  it.add(makeSystemColor(SystemColor.inactiveCaptionBorder, "inactiveCaptionBorder"))
  it.add(makeSystemColor(SystemColor.window, "window"))
  it.add(makeSystemColor(SystemColor.windowText, "windowText"))
  it.add(makeSystemColor(SystemColor.menu, "menu"))
  it.add(makeSystemColor(SystemColor.menuText, "menuText"))
  it.add(makeSystemColor(SystemColor.text, "text"))
  it.add(makeSystemColor(SystemColor.textHighlight, "textHighlight"))
  it.add(makeSystemColor(SystemColor.textText, "textText"))
  it.add(makeSystemColor(SystemColor.textHighlightText, "textHighlightText"))
  it.add(makeSystemColor(SystemColor.control, "control"))
  it.add(makeSystemColor(SystemColor.controlLtHighlight, "controlLtHighlight"))
  it.add(makeSystemColor(SystemColor.controlHighlight, "controlHighlight"))
  it.add(makeSystemColor(SystemColor.controlShadow, "controlShadow"))
  it.add(makeSystemColor(SystemColor.controlDkShadow, "controlDkShadow"))
  it.add(makeSystemColor(SystemColor.controlText, "controlText"))
  it.add(makeSystemColor(SystemColor.control, "control"))
  it.add(makeSystemColor(SystemColor.scrollbar, "scrollbar"))
  it.add(makeSystemColor(SystemColor.info, "info"))
  it.add(makeSystemColor(SystemColor.infoText, "infoText"))
  it.add(Box.createVerticalGlue())
}

private fun makeSystemColor(
  color: Color,
  text: String,
): Component {
  val field = JTextField("%s RGB(#%06X)".format(text, color.rgb and 0xFF_FF_FF))
  field.isEditable = false
  val c = object : JLabel() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 20
      return d
    }
  }
  c.isOpaque = true
  c.background = color
  val p = JPanel(BorderLayout())
  p.add(field)
  p.add(c, BorderLayout.EAST)
  return p
}

private object LookAndFeelUtils {
  private const val MAC = "com.sun.java.swing.plaf.mac.MacLookAndFeel"
  private const val METAL = "javax.swing.plaf.metal.MetalLookAndFeel"
  private const val MOTIF = "com.sun.java.swing.plaf.motif.MotifLookAndFeel"
  private const val WINDOWS = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
  private const val GTK = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
  private const val NIMBUS = "javax.swing.plaf.nimbus.NimbusLookAndFeel" // JDK 1.7.0
  private var currentLaf = METAL

  fun createLookAndFeelMenu(): JMenu {
    val lafMenuGroup = ButtonGroup()
    val lafMenu = JMenu("Look&Feel")
    val mi = createLafMenuItem(lafMenu, lafMenuGroup, "Metal", METAL)
    mi.isSelected = true // this is the default l&f
    createLafMenuItem(lafMenu, lafMenuGroup, "Mac", MAC)
    createLafMenuItem(lafMenu, lafMenuGroup, "Motif", MOTIF)
    createLafMenuItem(lafMenu, lafMenuGroup, "Windows", WINDOWS)
    createLafMenuItem(lafMenu, lafMenuGroup, "GTK", GTK)
    createLafMenuItem(lafMenu, lafMenuGroup, "Nimbus", NIMBUS)
    return lafMenu
  }

  private fun createLafMenuItem(
    menu: JMenu,
    lafMenuGroup: ButtonGroup,
    label: String,
    laf: String,
  ): JMenuItem {
    val mi = menu.add(JRadioButtonMenuItem(label))
    lafMenuGroup.add(mi)
    mi.addActionListener(ChangeLookAndFeelAction(laf))
    mi.isEnabled = isAvailableLookAndFeel(laf)
    return mi
  }

  private fun isAvailableLookAndFeel(laf: String) = runCatching {
    val o = Class.forName(laf).getConstructor().newInstance()
    (o as? LookAndFeel)?.isSupportedLookAndFeel == true
  }.getOrElse { false }

  fun setLookAndFeel(laf: String) {
    if (currentLaf == laf) {
      return
    }
    currentLaf = laf
    runCatching {
      UIManager.setLookAndFeel(currentLaf)
      updateLookAndFeel()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
      // println("Failed loading L&F: $currentLaf")
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }

  private class ChangeLookAndFeelAction(
    private val laf: String,
  ) : AbstractAction("ChangeTheme") {
    override fun actionPerformed(e: ActionEvent) {
      setLookAndFeel(laf)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
