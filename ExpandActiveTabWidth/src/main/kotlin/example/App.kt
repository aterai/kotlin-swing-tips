package example

import java.awt.*
import java.util.Arrays
import javax.swing.*

class MainPanel : JPanel(BorderLayout()) {
  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  private val icons = Arrays.asList("wi0009-16.png", "wi0054-16.png", "wi0062-16.png", "wi0063-16.png", "wi0124-16.png", "wi0126-16.png")

  init {
    val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    icons.forEach({ s ->
      val icon = ImageIcon(javaClass.getResource(s))
      val label = ShrinkLabel(s, icon)
      tabbedPane.addTab(s, icon, JLabel(s), s)
      tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, label)
    })
    updateTabWidth(tabbedPane)
    tabbedPane.addChangeListener({ e -> updateTabWidth(e.getSource() as JTabbedPane) })
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }
  fun updateTabWidth(tabs: JTabbedPane) {
    when (tabs.getTabPlacement()) {
      JTabbedPane.TOP, JTabbedPane.BOTTOM -> {
        val sidx = tabs.getSelectedIndex()
        for (i in 0 until tabs.getTabCount()) {
          val c = tabs.getTabComponentAt(i)
          if (c is ShrinkLabel) {
            c.isSelected = i == sidx
          }
        }
      }
    }
  }
}

internal class ShrinkLabel(title: String, icon: Icon) : JLabel(title, icon, SwingConstants.LEFT) {
  var isSelected: Boolean = false
  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    if (!isSelected) {
      d.width = 20
    }
    return d
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
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
  })
}
