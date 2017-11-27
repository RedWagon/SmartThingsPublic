definition(
    name: "Fake Modes Controller",
    namespace: "fake-modes/parent",
    author: "RedWagon",
    description: " ",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
    device_inputs()
    sensor_inputs()
    detail_inputs()
    app(name: "fakeMode", appName: "Fake Mode", namespace: "fake-modes/child", title: "Create New Mode", multiple: true)
}

def detail_inputs() {
    input "timeout", "number", title: "Normal timeout?", required: false
    input "hard_timeout", "number", title: "Hard timeout?", required: false
}

def device_inputs() {
    section(title: "Devices") {
        input "temps", "capability.colorTemperature", title: "Hue bulbs (temp control)", required: false, multiple: true, hideWhenEmpty: true
        input "colors", "capability.colorControl", title: "Hue bulbs (color control)", required: false, multiple: true, hideWhenEmpty: true
        input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required:false, hideWhenEmpty: true
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false, hideWhenEmpty: true
    }
    section(title: "Optional Devices") {
        input "optionals", "capability.switch", title: "Optional Devices", multiple: true, required: false, hideWhenEmpty: true
    }
}

def sensor_inputs() {
    section(title: "Positive Events") {
        input "sensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false, hideWhenEmpty: true
        input "open_doors", "capability.contactSensor", title: "Open Doors", multiple: true, required: false, hideWhenEmpty: true
        input "closed_doors", "capability.contactSensor", title: "Closed Doors", multiple: true, required: false, hideWhenEmpty: true
        input "push_buttons", "capability.button", title: "Push Buttons", multiple: true, required: false, hideWhenEmpty: true
        input "hold_buttons", "capability.button", title: "Hold Buttons", multiple: true, required: false, hideWhenEmpty: true
    }
    section(title: "Manual Off Events") {
        input "open_doors_off", "capability.contactSensor", title: "Open Doors", multiple: true, required: false, hideWhenEmpty: true
        input "closed_doors_off", "capability.contactSensor", title: "Closed Doors", multiple: true, required: false, hideWhenEmpty: true
        input "push_buttons_off", "capability.button", title: "Push Buttons", multiple: true, required: false, hideWhenEmpty: true
        input "hold_buttons_off", "capability.button", title: "Hold Buttons", multiple: true, required: false, hideWhenEmpty: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}."
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}."
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	subscribe(temps, "colorTemperature", rogueHandler)
	subscribe(colors, "colorControl", rogueHandler)

	subscribe(temps, "switchLevel", rogueHandler)
	subscribe(colors, "switchLevel", rogueHandler)
	subscribe(dimmers, "switchLevel", rogueHandler)

	subscribe(temps, "switch", rogueHandler)
	subscribe(colors, "switch", rogueHandler)
	subscribe(dimmers, "switch", rogueHandler)
	subscribe(switches, "switch", rogueHandler)

    subscribe(sensors, "motion.active", onHandler)
    subscribe(open_doors, "contact.open", onHandler)
    subscribe(closed_doors, "contact.closed", onHandler)
    subscribe(push_buttons, "button.pushed", onHandler)
    subscribe(held_buttons, "button.held", onHandler)

    subscribe(sensors, "motion.inactive", timeoutHandler)

    subscribe(open_doors_off, "contact.open", offHandler)
    subscribe(closed_doors_off, "contact.closed", offHandler)
    subscribe(push_buttons_off, "button.pushed", offHandler)
    subscribe(held_buttons_off, "button.held", offHandler)
}

def rogueHandler(evt) {
	log.debug "Rogue $evt.displayName $evt.name: $evt.value"
}
def onHandler(evt) {
	log.debug "On $evt.displayName $evt.name: $evt.value"
    unschedule(allOff)
    writeMode(topMode())
}
def timeoutHandler(evt) {
	log.debug "Timeout $evt.displayName $evt.name: $evt.value"
    runIn(timeout, allOff)
}
def offHandler(evt) {
	log.debug "Off $evt.displayName $evt.name: $evt.value"
    allOff()
}

def allOff() {
    temps?.off()
    colors?.off()
    dimmers?.off()
    switches?.off()
}

def writeMode(mode) {
    temps?.setColorTemperature(modes.temp.toInteger())
    colors?.setColor(mode.getHueColor())
    dimmers?.setLevel(mode.dimmer_level.toInteger())
    if (mode.switch_state) {
        switches?.on()
    } else {
        switches?.off()
    }

}

def topMode() {
    modes = getChildApps()
    modes.each { mode ->
        if (mode.active()) {
            return mode
        }
    }
}
