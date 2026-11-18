package example

import java.awt.*
import java.util.EventListener
import java.util.EventObject
import javax.swing.*

private fun createList() = listOf(
  object : AbstractExpansionPanel("Panel1") {
    override fun createPanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      p.add(JCheckBox("11111"))
      p.add(JCheckBox("2222222222"))
      return p
    }
  },
  object : AbstractExpansionPanel("Panel2") {
    override fun createPanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      for (i in 0..15) {
        p.add(createLabel(i))
      }
      return p
    }

    private fun createLabel(i: Int) = JLabel("%02d".format(i))
  },
  object : AbstractExpansionPanel("Panel3") {
    override fun createPanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      val bg = ButtonGroup()
      listOf(
        JRadioButton("aa"),
        JRadioButton("bb"),
        JRadioButton("cc", true),
      ).forEach {
        p.add(it)
        bg.add(it)
      }
      return p
    }
  },
)

fun createUI(): Component {
  val northBox = Box.createVerticalBox()
  val centerBox = Box.createVerticalBox()
  val southBox = Box.createVerticalBox()
  val panelList = createList()
  val panel = object : JPanel(BorderLayout()) {
    override fun getMinimumSize() = super.getMinimumSize()?.also {
      it.width = 120
    }
  }
  panel.add(northBox, BorderLayout.NORTH)
  panel.add(centerBox)
  panel.add(southBox, BorderLayout.SOUTH)

  fun rearrangePanels(source: Component) {
    listOf(northBox, centerBox, southBox).forEach { it.removeAll() }
    var target = northBox
    for (p in panelList) {
      if (p == source && p.isExpanded) {
        centerBox.add(p)
        target = southBox
      } else {
        p.isExpanded = false
        target.add(p)
      }
    }
  }

  val listener = ExpansionListener { e ->
    (e.source as? Component)?.also {
      rearrangePanels(it)
      // Removing components does not always repaint the vacated area
      panel.revalidate()
      panel.repaint()
    }
  }
  panelList.forEach {
    northBox.add(it)
    it.addExpansionListener(listener)
  }

  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, JScrollPane(JTree())).also {
    it.preferredSize = Dimension(320, 240)
  }
}

private abstract class AbstractExpansionPanel(
  title: String?,
) : JPanel(BorderLayout()) {
  private var expansionEvent: ExpansionEvent? = null
  private val scroll = JScrollPane()
  private val button = JButton(title)
  var isExpanded = false
    set(expanded) {
      if (expanded) {
        add(scroll)
      } else {
        remove(scroll)
      }
      field = expanded
    }

  init {
    scroll.also {
      it.setViewportView(createPanel())
      it.verticalScrollBar.unitIncrement = 25
    }
    button.also {
      it.addActionListener {
        isExpanded = !isExpanded
        fireExpansionEvent()
      }
      add(it, BorderLayout.NORTH)
    }
  }

  abstract fun createPanel(): Container?

  fun addExpansionListener(l: ExpansionListener) {
    listenerList.add(ExpansionListener::class.java, l)
  }

  // Notify all listeners that have registered interest for
  // notification on this event type. The event instance
  // is lazily created using the parameters passed into
  // the fire method.
  protected fun fireExpansionEvent() {
    // Guaranteed to return a non-null array
    val listeners = listenerList.listenerList
    // Process the listeners last to first, notifying
    // those that are interested in this event
    var i = listeners.size - 2
    while (i >= 0) {
      if (listeners[i] === ExpansionListener::class.java) {
        // Lazily create the event:
        val ee = expansionEvent ?: ExpansionEvent(this)
        (listeners[i + 1] as? ExpansionListener)?.expansionStateChanged(ee)
      }
      i -= 2
    }
  }
}

class ExpansionEvent(
  source: Any?,
) : EventObject(source) {
  companion object {
    private const val serialVersionUID = 1L
  }
}

fun interface ExpansionListener : EventListener {
  fun expansionStateChanged(e: ExpansionEvent)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
