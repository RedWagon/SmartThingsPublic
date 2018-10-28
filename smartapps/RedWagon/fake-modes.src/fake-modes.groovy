definition(
    name: "Fake Modes Controller",
    namespace: "fake-modes/parent",
    author: "RedWagon",
    description: "Parent script",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
    device_inputs()
    sensor_inputs()
    detail_inputs()
}

def detail_inputs() {
    section(title: "Devices") {
        input "timeout", "number", title: "Normal timeout?", required: false
        input "hard_timeout", "number", title: "Hard timeout?", required: false
        app(name: "fakeMode", appName: "Fake Mode", namespace: "fake-modes/child", title: "Create New Mode", multiple: true)
    }
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
    state.bow_step = 1;
    state.bow_delay = 30;
    state.hue = 0;
    state.saturation = 0;
    state.level = 0;
    state.active = false
    
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
    unschedule(optionalOff)
    if (state.active) {
        log.debug "This is already active doing nothing"
    } else {
        log.debug "This was off, turning on"
        state.active = true
        auto_on()
    }
}

def auto_on() {
    if (state.active) {
        log.debug "Writing new modes"
        unschedule(allOff)
        unschedule(optionalOff)
        writeMode(topMode())
    } else {
        log.debug "Not active, doing nothing"
    }
}

def timeoutHandler(evt) {
	log.debug "Timeout $evt.displayName $evt.name: $evt.value"
    log.debug "Scheduling allOff in $timeout minutes"
    log.debug "Scheduling optionalOff in $hard_timeout"
    runIn(timeout * 60, allOff)
    runIn(hard_timeout * 60, optionalOff)
}

def offHandler(evt) {
	log.debug "Off $evt.displayName $evt.name: $evt.value"
    allOff()
}

def allOff() {
    state.active = false
    log.debug("Turning off rainbow")
    unschedule(rainbow)
    temps?.off()
    colors?.off()
    dimmers?.off()
    switches?.off()
}

def optionalOff() {
    optionals?.off()
}

def rainbow() {
    log.debug("Updating rainbow")
    log.debug("State hue: $state.hue step: $state.bow_step")
    def new_hue = state.hue + state.bow_step
    if (new_hue > 100) {
        new_hue = new_hue - 100
    }
    log.debug("Rainbow hue is $new_hue")
    state.hue = new_hue
    def payload = [hue: new_hue]
    colors?.setColor(payload)
    runIn(state.bow_delay, rainbow)
}
def writeColor(mode) {
    if (!mode?.color) {
        return
    }
    def payload = mode.getHueColor()
    log.debug("Setting color to $payload")
    colors?.setColor(payload)
    if(mode?.bow_delay) {
        log.debug("Starting Rainbow")
        state.bow_step = mode.bow_step
        state.bow_delay = mode.bow_delay
        state.hue = payload.hue
        runIn(mode.bow_delay, rainbow)
    } else {
        unschedule(rainbow)
        return
    }
}

def writeSwitches(mode) {
    if (!mode?.switch_state) {
        return
    }
    log.debug("Setting switch to $mode.switch_state")
    if (mode.switch_state as String == "true") {
        log.debug("the switches are on")
        switches?.on()
        return
    }
    log.debug("the switches are off")
    switches?.off()
}

def writeMode(mode) {
    if (mode?.temp) {
        log.debug("Setting temp")
        temps?.setColorTemperature(mode.temp.toInteger())
    }
    writeColor(mode)
    if (mode?.dimmer_level != null) {
        log.debug("Setting dimmer")
        dimmers?.setLevel(mode.dimmer_level.toInteger())
    }
    writeSwitches(mode)
}

def topMode() {
    log.debug("Choosing top mode")
    def modes = getChildApps()
    log.debug("I found $modes.size modes")
    for (mode in modes.sort{ it.order }) {
        log.debug("Testing mode $mode.label")
        if (mode.active()) {
            log.debug("Mode is active $mode.label")
            return mode
        }
    }
    log.warn("topMode didn't return")
}
